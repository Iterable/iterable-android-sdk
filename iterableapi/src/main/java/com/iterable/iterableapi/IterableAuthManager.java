package com.iterable.iterableapi;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.iterable.iterableapi.response.IterableAuthResponseObject;
import com.iterable.iterableapi.response.IterableResponseObject;
import com.iterable.iterableapi.response.handlers.IterableCallbackHandlers;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IterableAuthManager implements IterableActivityMonitor.AppStateCallback {
    private static final String TAG = "IterableAuth";
    private static final String expirationString = "exp";

    private final IterableApi api;
    private final IterableAuthHandler authHandler;
    private final long expiringAuthTokenRefreshPeriod;
    private final IterableActivityMonitor activityMonitor;
    @VisibleForTesting
    Timer timer;
    private boolean hasFailedPriorAuth;
    private boolean pendingAuth;
    private boolean requiresAuthRefresh;
    RetryPolicy authRetryPolicy;
    boolean pauseAuthRetry;
    int retryCount;
    private boolean isLastAuthTokenValid;
    private volatile boolean isTimerScheduled;
    private volatile boolean isInForeground = true; // Assume foreground initially

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    IterableAuthManager(IterableApi api, IterableAuthHandler authHandler, RetryPolicy authRetryPolicy, long expiringAuthTokenRefreshPeriod) {
        this.api = api;
        this.authHandler = authHandler;
        this.authRetryPolicy = authRetryPolicy;
        this.expiringAuthTokenRefreshPeriod = expiringAuthTokenRefreshPeriod;
        this.activityMonitor = IterableActivityMonitor.getInstance();
        this.activityMonitor.addCallback(this);
    }

    public synchronized void requestNewAuthToken(boolean hasFailedPriorAuth, IterableCallbackHandlers.SuccessCallback successCallback) {
        requestNewAuthToken(hasFailedPriorAuth, successCallback, true);
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

    private void handleSuccessForAuthToken(@NonNull String authToken, IterableCallbackHandlers.SuccessCallback successCallback) {
        IterableAuthResponseObject.Success remoteSuccess = new IterableAuthResponseObject.Success(authToken);
        successCallback.onSuccess(remoteSuccess);

    }

    public synchronized void requestNewAuthToken(
            boolean hasFailedPriorAuth,
            final IterableCallbackHandlers.SuccessCallback successCallback,
            boolean shouldIgnoreRetryPolicy) {
        if (!shouldIgnoreRetryPolicy && (pauseAuthRetry || (retryCount >= authRetryPolicy.maxRetry))) {
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
                                    pendingAuth = false;
                                    return;
                                }

                                // Only request new auth token if app is in foreground
                                if (!isInForeground) {
                                    IterableLogger.w(TAG, "Auth token request skipped - app is in background");
                                    pendingAuth = false;
                                    return;
                                }

                                final String authToken = authHandler.onAuthTokenRequested();
                                pendingAuth = false;
                                retryCount++;
                                handleAuthTokenSuccess(authToken, successCallback);
                            } catch (final Exception e) {
                                retryCount++;
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

    private void handleAuthTokenSuccess(String authToken, IterableCallbackHandlers.SuccessCallback successCallback) {
        if (authToken != null) {
            IterableApi.getInstance().setAuthToken(authToken);
            queueExpirationRefresh(authToken);

            if (successCallback != null) {
                handleSuccessForAuthToken(authToken, successCallback);
            }
        } else {
            handleAuthFailure(authToken, AuthFailureReason.AUTH_TOKEN_NULL);
            IterableApi.getInstance().setAuthToken(authToken);
            scheduleAuthTokenRefresh(getNextRetryInterval(), false, null);
            return;
        }
        reSyncAuth();
        authHandler.onTokenRegistrationSuccessful(authToken);
    }

    // This method is called when there is an error receiving an the auth token.
    private void handleAuthTokenFailure(Throwable throwable) {
        IterableLogger.e(TAG, "Error while requesting Auth Token", throwable);
        handleAuthFailure(null, AuthFailureReason.AUTH_TOKEN_GENERATION_ERROR);
        pendingAuth = false;
        scheduleAuthTokenRefresh(getNextRetryInterval(), false, null);
    }

    public void queueExpirationRefresh(@Nullable String encodedJWT) {
        clearRefreshTimer();
        try {
            if (encodedJWT == null) {
                IterableLogger.d(TAG, "JWT is null. Scheduling token refresh");
                if (!isTimerScheduled) {
                    scheduleAuthTokenRefresh(getNextRetryInterval(), false, null);
                }
                return;
            }

            long expirationTimeSeconds = decodedExpiration(encodedJWT);
            long triggerExpirationRefreshTime = expirationTimeSeconds * 1000L - expiringAuthTokenRefreshPeriod - IterableUtil.currentTimeMillis();
            if (triggerExpirationRefreshTime > 0) {
                scheduleAuthTokenRefresh(triggerExpirationRefreshTime, true, null);
            } else {
                scheduleAuthTokenRefresh(getNextRetryInterval(), true, null);
            }
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while parsing JWT for the expiration", e);
            isLastAuthTokenValid = false;
            handleAuthFailure(encodedJWT, AuthFailureReason.AUTH_TOKEN_PAYLOAD_INVALID);
            scheduleAuthTokenRefresh(getNextRetryInterval(), false, null);
        }
    }

    void resetFailedAuth() {
        hasFailedPriorAuth = false;
    }

    void reSyncAuth() {
        if (requiresAuthRefresh) {
            requiresAuthRefresh = false;
            scheduleAuthTokenRefresh(getNextRetryInterval(), false, null);
        }
    }

    // This method is called is used to call the authHandler.onAuthFailure method with appropriate AuthFailureReason
    void handleAuthFailure(String authToken, AuthFailureReason failureReason) {
        if (authHandler != null) {
            authHandler.onAuthFailure(new AuthFailure(getEmailOrUserId(), authToken, IterableUtil.currentTimeMillis(), failureReason));
        }
    }


    long getNextRetryInterval() {
        long nextRetryInterval = authRetryPolicy.retryInterval;
        if (authRetryPolicy.retryBackoff == RetryPolicy.Type.EXPONENTIAL) {
            nextRetryInterval *= Math.pow(IterableConstants.EXPONENTIAL_FACTOR, retryCount - 1); // Exponential backoff
        }

        return nextRetryInterval;
    }

    void scheduleAuthTokenRefresh(long timeDuration, boolean isScheduledRefresh, final IterableCallbackHandlers.SuccessCallback successCallback) {
        if ((pauseAuthRetry && !isScheduledRefresh) || isTimerScheduled) {
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

    /**
     * Checks if the current auth token needs to be refreshed and handles any deferred auth requests.
     * This method is called when the app comes to foreground to ensure timely token refresh.
     */
    private void checkAndHandleAuthRefresh() {
        // First, check if current auth token needs refresh based on expiration
        if (api.getEmail() != null || api.getUserId() != null) {
            String currentAuthToken = api.getAuthToken();
            queueExpirationRefresh(currentAuthToken);
        } else {
            IterableLogger.d(TAG, "Email or userId is not available. Skipping token refresh");
        }
    }

    void clearRefreshTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            isTimerScheduled = false;
        }
    }

    @Override
    public void onSwitchToForeground() {
        try {
            IterableLogger.d(TAG, "App switched to foreground - enabling auth token requests");
            isInForeground = true;
            checkAndHandleAuthRefresh();
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error occurred in handling auth token refresh", e);
        }
    }

    @Override
    public void onSwitchToBackground() {
        try {
            IterableLogger.d(TAG, "App switched to background - disabling auth token requests");
            isInForeground = false;
            clearRefreshTimer();
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while switching to background", e);
        }
    }
}

