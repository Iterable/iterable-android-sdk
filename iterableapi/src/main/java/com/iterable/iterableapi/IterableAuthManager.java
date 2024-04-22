package com.iterable.iterableapi;

import android.util.Base64;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IterableAuthManager {
    private static final String TAG = "IterableAuth";
    private static final String expirationString = "exp";

    private final IterableApi api;
    private final IterableAuthHandler authHandler;
    private final long expiringAuthTokenRefreshPeriod;
    @VisibleForTesting
    Timer timer;
    private boolean hasFailedPriorAuth;
    private boolean pendingAuth;
    private boolean requiresAuthRefresh;
    private RetryPolicy authRetryPolicy;
    private boolean pauseAuthRetry;
    private int retryCount;
    private boolean isLastAuthTokenValid;
    private boolean isTimerScheduled;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    IterableAuthManager(IterableApi api, IterableAuthHandler authHandler, RetryPolicy authRetryPolicy, long expiringAuthTokenRefreshPeriod) {
        this.api = api;
        this.authHandler = authHandler;
        this.authRetryPolicy = authRetryPolicy;
        this.expiringAuthTokenRefreshPeriod = expiringAuthTokenRefreshPeriod;
    }

    public synchronized void requestNewAuthToken(boolean hasFailedPriorAuth) {
        requestNewAuthToken(hasFailedPriorAuth, null, true);
    }

    public void pauseAuthRetries(boolean pauseRetry) {
        pauseAuthRetry = pauseRetry;
        resetRetryCount();
    }

    void reset() {
        clearRefreshTimer();
        setIsLastAuthTokenValid(false);
    }

    void setIsLastAuthTokenValid(boolean isValid) {
        isLastAuthTokenValid = isValid;
    }

    void resetRetryCount() {
        retryCount = 0;
    }

    private void handleSuccessForAuthToken(String authToken, IterableHelper.SuccessHandler successCallback) {
        try {
            JSONObject object = new JSONObject();
            object.put("newAuthToken", authToken);
            successCallback.onSuccess(object);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public synchronized void requestNewAuthToken(
            boolean hasFailedPriorAuth,
            final IterableHelper.SuccessHandler successCallback,
            boolean shouldIgnoreRetryPolicy) {
        if ((!shouldIgnoreRetryPolicy && pauseAuthRetry) || (retryCount >= authRetryPolicy.maxRetry && !shouldIgnoreRetryPolicy)) {
            return;
        }

        if (authHandler != null) {
            if (!pendingAuth) {
                if (!(this.hasFailedPriorAuth && hasFailedPriorAuth)) {
                    this.hasFailedPriorAuth = hasFailedPriorAuth;
                    pendingAuth = true;

                    executor.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (isLastAuthTokenValid && !shouldIgnoreRetryPolicy) {
                                    // if some JWT retry had valid token it will not fetch the auth token again from developer function
                                    handleAuthTokenSuccess(IterableApi.getInstance().getAuthToken(), successCallback);
                                    return;
                                }
                                final String authToken = authHandler.onAuthTokenRequested();
                                retryCount++;
                                handleAuthTokenSuccess(authToken, successCallback);
                            } catch (final Exception e) {
                                handleAuthTokenFailure(e);
                            }
                        }
                    });
                }
            } else if (!hasFailedPriorAuth) {
                //setFlag to resync auth after current auth returns
                requiresAuthRefresh = true;
            }

        } else {
            IterableApi.getInstance().setAuthToken(null, true);
        }
    }

    private void handleAuthTokenSuccess(String authToken, IterableHelper.SuccessHandler successCallback) {
        if (authToken != null) {
            if (successCallback != null) {
                handleSuccessForAuthToken(authToken, successCallback);
            }
            queueExpirationRefresh(authToken);
        } else {
            IterableLogger.w(TAG, "Auth token received as null. Calling the handler in 10 seconds");
            //TODO: Make this time configurable and in sync with SDK initialization flow for auth null scenario
            scheduleAuthTokenRefresh(getNextRetryInterval(), false, null);
            authHandler.onTokenRegistrationFailed(new Throwable("Auth token null"));
            return;
        }
        IterableApi.getInstance().setAuthToken(authToken);
        pendingAuth = false;
        reSyncAuth();
        authHandler.onTokenRegistrationSuccessful(authToken);
    }

    private void handleAuthTokenFailure(Throwable throwable) {
        IterableLogger.e(TAG, "Error while requesting Auth Token", throwable);
        authHandler.onTokenRegistrationFailed(throwable);
        pendingAuth = false;
        reSyncAuth();
    }

    public void queueExpirationRefresh(String encodedJWT) {
        clearRefreshTimer();
        try {
            long expirationTimeSeconds = decodedExpiration(encodedJWT);
            long triggerExpirationRefreshTime = expirationTimeSeconds * 1000L - expiringAuthTokenRefreshPeriod - IterableUtil.currentTimeMillis();
            if (triggerExpirationRefreshTime > 0) {
                scheduleAuthTokenRefresh(triggerExpirationRefreshTime, true, null);
            } else {
                IterableLogger.w(TAG, "The expiringAuthTokenRefreshPeriod has already passed for the current JWT");
            }
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while parsing JWT for the expiration", e);
            authHandler.onTokenRegistrationFailed(new Throwable("Auth token decode failure. Scheduling auth token refresh in 10 seconds..."));
            //TODO: Sync with configured time duration once feature is available.
            scheduleAuthTokenRefresh(getNextRetryInterval(), false, null);
        }
    }

    void resetFailedAuth() {
        hasFailedPriorAuth = false;
    }

    void reSyncAuth() {
        if (requiresAuthRefresh) {
            requiresAuthRefresh = false;
            requestNewAuthToken(false);
        }
    }

    long getNextRetryInterval() {
        long nextRetryInterval = authRetryPolicy.retryInterval;
        if (authRetryPolicy.retryBackoff == RetryPolicy.Type.EXPONENTIAL) {
            nextRetryInterval *= Math.pow(IterableConstants.EXPONENTIAL_FACTOR, retryCount - 1); // Exponential backoff
        }

        return nextRetryInterval;
    }

    void scheduleAuthTokenRefresh(long timeDuration, boolean isScheduledRefresh, final IterableHelper.SuccessHandler successCallback) {
        if (pauseAuthRetry && !isScheduledRefresh && isTimerScheduled) {
            // we only stop schedule token refresh if it is called from retry (in case of failure). The normal auth token refresh schedule would work
            return;
        }
        if (timer == null) {
            timer = new Timer(true);
        }
        try {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (api.getEmail() != null || api.getUserId() != null) {
                        api.getAuthManager().requestNewAuthToken(false, successCallback, isScheduledRefresh);
                    } else {
                        IterableLogger.w(TAG, "Email or userId is not available. Skipping token refresh");
                    }
                    isTimerScheduled = false;
                }
            }, timeDuration);
            isTimerScheduled = true;
        } catch (Exception e) {
            IterableLogger.e(TAG, "timer exception: " + timer, e);
        }
    }

    private String getEmailOrUserId() {
        String email = api.getEmail();
        String userId = api.getUserId();

        if (email != null) {
            return email;
        } else if (userId != null) {
            return userId;
        }
        return null;
    }

    private static long decodedExpiration(String encodedJWT) throws Exception {
        long exp = 0;
            String[] split = encodedJWT.split("\\.");
            //Check if jwt is valid
            if (split.length != 3) {
                throw new IllegalArgumentException("Invalid JWT");
            }
            String body = getJson(split[1]);
            JSONObject jObj = new JSONObject(body);
            exp = jObj.getLong(expirationString);
        return exp;
    }

    private static String getJson(String strEncoded) throws UnsupportedEncodingException {
        byte[] decodedBytes = Base64.decode(strEncoded, Base64.URL_SAFE);
        return new String(decodedBytes, "UTF-8");
    }

    void clearRefreshTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}

