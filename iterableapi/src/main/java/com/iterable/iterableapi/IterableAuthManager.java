package com.iterable.iterableapi;

import android.util.Base64;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class IterableAuthManager implements IterableActivityMonitor.AppStateCallback {
    private static final String TAG = "IterableAuth";
    private static final String expirationString = "exp";

    /**
     * Represents the state of the JWT auth token.
     * VALID: Last request succeeded with this token.
     * INVALID: A 401 JWT error was received; processing should pause.
     * UNKNOWN: A new token was obtained but not yet verified by a request.
     * RESTORING: Startup auth is unresolved, so JWT-required work must wait.
     */
    enum AuthState {
        VALID,
        INVALID,
        UNKNOWN,
        RESTORING
    }

    private enum RefreshCancellationReason {
        AUTH_RESET,
        TOKEN_REPLACED,
        EXPLICIT_CLEAR,
        APP_BACKGROUNDED
    }

    /**
     * Listener interface for components that need to react when a new auth token is ready.
     */
    interface AuthTokenReadyListener {
        void onAuthTokenReady();
    }

    private final IterableApi api;
    private final IterableAuthHandler authHandler;
    private final long expiringAuthTokenRefreshPeriod;
    private final IterableActivityMonitor activityMonitor;
    @VisibleForTesting
    Timer timer;
    @VisibleForTesting
    volatile TimerTask scheduledRefreshTask;
    @VisibleForTesting
    volatile IterableAuthRefreshReason scheduledRefreshReason;
    @Nullable
    private volatile IterableAuthDataRestorer authDataRestorer;
    private boolean hasFailedPriorAuth;
    private boolean pendingAuth;
    private boolean requiresAuthRefresh;
    RetryPolicy authRetryPolicy;
    boolean pauseAuthRetry;
    int retryCount;
    private boolean isLastAuthTokenValid;
    private volatile boolean isInForeground = true; // Assume foreground initially

    private volatile AuthState authState = AuthState.UNKNOWN;
    private final ArrayList<AuthTokenReadyListener> authTokenReadyListeners = new ArrayList<>();

    @VisibleForTesting
    ExecutorService executor = Executors.newSingleThreadExecutor();

    IterableAuthManager(IterableApi api, IterableAuthHandler authHandler, RetryPolicy authRetryPolicy, long expiringAuthTokenRefreshPeriod) {
        this.api = api;
        this.authHandler = authHandler;
        this.authRetryPolicy = authRetryPolicy;
        this.expiringAuthTokenRefreshPeriod = expiringAuthTokenRefreshPeriod;
        this.activityMonitor = IterableActivityMonitor.getInstance();
        this.activityMonitor.addCallback(this);
    }

    void addAuthTokenReadyListener(AuthTokenReadyListener listener) {
        authTokenReadyListeners.add(listener);
    }

    void removeAuthTokenReadyListener(AuthTokenReadyListener listener) {
        authTokenReadyListeners.remove(listener);
    }

    /**
     * Returns true if the auth token is in a state that allows requests to proceed.
     * Requests can proceed when auth state is VALID or UNKNOWN (newly obtained token).
     * If no authHandler is configured (JWT not used), this always returns true.
     */
    boolean isAuthTokenReady() {
        if (authHandler == null) {
            return true;
        }
        return isReadyState(authState);
    }

    /**
     * Marks the auth token as invalid. Called when a 401 JWT error is received.
     */
    void setAuthTokenInvalid() {
        setAuthState(AuthState.INVALID);
    }

    AuthState getAuthState() {
        return authState;
    }

    /**
     * Centralized auth state setter. Listeners are notified whenever auth moves from a blocked
     * state to a ready state.
     */
    private void setAuthState(AuthState newState) {
        AuthState previousState = this.authState;
        this.authState = newState;

        if (!isReadyState(previousState) && isReadyState(newState)) {
            notifyAuthTokenReadyListeners();
        }
    }

    private boolean isReadyState(AuthState state) {
        return state == AuthState.VALID || state == AuthState.UNKNOWN;
    }

    @Nullable
    String restoreAuthToken(
            IterableKeychain keychain,
            @Nullable ScheduledExecutorService retryExecutor
    ) {
        KeychainReadResult initialRead = keychain.readAuthToken();
        setAuthState(AuthState.RESTORING);
        authDataRestorer = retryExecutor == null
                ? new IterableAuthDataRestorer(keychain)
                : new IterableAuthDataRestorer(keychain, retryExecutor);
        authDataRestorer.restore(
                initialRead,
                new IterableAuthDataRestorer.Callback() {
                    @Override
                    public void onAuthTokenRestored(@Nullable String authToken) {
                        api.setRestoredAuthToken(authToken);
                        authDataRestorer = null;
                        handleRestoredAuthToken(authToken);
                    }

                    @Override
                    public void onAuthTokenUnavailable() {
                        IterableLogger.w(
                                TAG,
                                "auth_restore action=block reason=storage_unavailable"
                        );
                    }
                }
        );
        return initialRead.valueOrNull();
    }

    private void handleRestoredAuthToken(@Nullable String authToken) {
        if (authToken != null) {
            setAuthState(AuthState.UNKNOWN);
            queueExpirationRefresh(authToken);
        } else {
            scheduleAuthTokenRefresh(
                    getNextRetryInterval(),
                    IterableAuthRefreshReason.STORED_TOKEN_MISSING,
                    null
            );
        }
    }

    void cancelAuthTokenRestore(String reason) {
        if (authDataRestorer != null) {
            authDataRestorer.cancel(reason);
            authDataRestorer = null;
        }
    }

    void useExplicitAuthToken(String authToken) {
        cancelAuthTokenRestore("explicit_token");
        api.setAuthToken(authToken);
        if (authHandler != null) {
            setAuthState(AuthState.UNKNOWN);
            queueExpirationRefresh(authToken);
        }
    }

    private void notifyAuthTokenReadyListeners() {
        ArrayList<AuthTokenReadyListener> listenersCopy = new ArrayList<>(authTokenReadyListeners);
        for (AuthTokenReadyListener listener : listenersCopy) {
            listener.onAuthTokenReady();
        }
    }

    public synchronized void requestNewAuthToken(boolean hasFailedPriorAuth, IterableHelper.SuccessHandler successCallback) {
        requestNewAuthToken(hasFailedPriorAuth, successCallback, true);
    }

    public void pauseAuthRetries(boolean pauseRetry) {
        pauseAuthRetry = pauseRetry;
        resetRetryCount();
    }

    void reset() {
        clearRefreshTimer(RefreshCancellationReason.AUTH_RESET);
        setIsLastAuthTokenValid(false);
    }

    void resetForIdentityChange() {
        cancelAuthTokenRestore("identity_changed");
        if (authHandler != null) {
            setAuthState(AuthState.RESTORING);
        }
        reset();
    }

    void setIsLastAuthTokenValid(boolean isValid) {
        isLastAuthTokenValid = isValid;
        if (isValid) {
            setAuthState(AuthState.VALID);
        }
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
                                    handleAuthTokenSuccess(api.getAuthToken(), successCallback);
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
            api.setAuthToken(null, true);
        }
    }

    private void handleAuthTokenSuccess(String authToken, IterableHelper.SuccessHandler successCallback) {
        if (authToken != null) {
            // Token obtained but not yet verified by a request. Storing it before changing state
            // ensures listeners cannot resume JWT work with the previous token.
            api.setAuthToken(authToken);
            setAuthState(AuthState.UNKNOWN);
            queueExpirationRefresh(authToken);

            if (successCallback != null) {
                handleSuccessForAuthToken(authToken, successCallback);
            }
        } else {
            handleAuthFailure(authToken, AuthFailureReason.AUTH_TOKEN_NULL);
            api.setAuthToken(authToken);
            scheduleAuthTokenRefresh(
                    getNextRetryInterval(),
                    IterableAuthRefreshReason.AUTH_HANDLER_RETRY,
                    null
            );
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
        scheduleAuthTokenRefresh(
                getNextRetryInterval(),
                IterableAuthRefreshReason.AUTH_HANDLER_RETRY,
                null
        );
    }

    public void queueExpirationRefresh(@Nullable String encodedJWT) {
        clearRefreshTimer(RefreshCancellationReason.TOKEN_REPLACED);
        try {
            if (encodedJWT == null) {
                IterableLogger.d(TAG, "JWT is null. Scheduling token refresh");
                scheduleAuthTokenRefresh(
                        getNextRetryInterval(),
                        IterableAuthRefreshReason.TOKEN_MISSING,
                        null
                );
                return;
            }

            long expirationTimeSeconds = decodedExpiration(encodedJWT);
            long triggerExpirationRefreshTime = expirationTimeSeconds * 1000L - expiringAuthTokenRefreshPeriod - IterableUtil.currentTimeMillis();
            if (triggerExpirationRefreshTime > 0) {
                scheduleAuthTokenRefresh(
                        triggerExpirationRefreshTime,
                        IterableAuthRefreshReason.TOKEN_EXPIRING,
                        null
                );
            } else {
                scheduleAuthTokenRefresh(
                        getNextRetryInterval(),
                        IterableAuthRefreshReason.TOKEN_EXPIRED,
                        null
                );
            }
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while parsing JWT for the expiration", e);
            isLastAuthTokenValid = false;
            handleAuthFailure(encodedJWT, AuthFailureReason.AUTH_TOKEN_PAYLOAD_INVALID);
            scheduleAuthTokenRefresh(
                    getNextRetryInterval(),
                    IterableAuthRefreshReason.TOKEN_INVALID,
                    null
            );
        }
    }

    void resetFailedAuth() {
        hasFailedPriorAuth = false;
    }

    void reSyncAuth() {
        if (requiresAuthRefresh) {
            requiresAuthRefresh = false;
            scheduleAuthTokenRefresh(
                    getNextRetryInterval(),
                    IterableAuthRefreshReason.DEFERRED_REFRESH,
                    null
            );
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

    synchronized void scheduleAuthTokenRefresh(
            long timeDuration,
            IterableAuthRefreshReason reason,
            final IterableHelper.SuccessHandler successCallback
    ) {
        if (pauseAuthRetry && !reason.ignoresRetryPolicy()) {
            IterableLogger.d(
                    TAG,
                    "auth_refresh action=skip reason="
                            + reason
                            + " cause=retry_paused"
            );
            return;
        }
        if (scheduledRefreshTask != null) {
            IterableLogger.d(
                    TAG,
                    "auth_refresh action=skip reason="
                            + reason
                            + " cause=already_scheduled pending_reason="
                            + scheduledRefreshReason
            );
            return;
        }
        if (timer == null) {
            timer = new Timer(true);
        }

        final TimerTask refreshTask = new TimerTask() {
            @Override
            public void run() {
                if (!claimRefreshTask(this, reason)) {
                    return;
                }

                IterableLogger.d(TAG, "auth_refresh action=fire reason=" + reason);
                if (api.getEmail() != null || api.getUserId() != null) {
                    requestNewAuthToken(
                            false,
                            successCallback,
                            reason.ignoresRetryPolicy()
                    );
                } else {
                    IterableLogger.w(
                            TAG,
                            "auth_refresh action=skip reason="
                                    + reason
                                    + " cause=identity_missing"
                    );
                }
            }
        };

        try {
            scheduledRefreshTask = refreshTask;
            scheduledRefreshReason = reason;
            timer.schedule(refreshTask, timeDuration);
            IterableLogger.d(
                    TAG,
                    "auth_refresh action=schedule reason="
                            + reason
                            + " delay_ms="
                            + timeDuration
            );
        } catch (Exception e) {
            releaseRefreshTask(refreshTask);
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            IterableLogger.e(
                    TAG,
                    "auth_refresh action=error reason="
                            + reason
                            + " cause=schedule_failed",
                    e
            );
        }
    }

    private synchronized boolean claimRefreshTask(
            TimerTask task,
            IterableAuthRefreshReason reason
    ) {
        if (scheduledRefreshTask != task) {
            IterableLogger.d(
                    TAG,
                    "auth_refresh action=ignore reason="
                            + reason
                            + " cause=stale_task"
            );
            return false;
        }

        scheduledRefreshTask = null;
        scheduledRefreshReason = null;
        return true;
    }

    private synchronized void releaseRefreshTask(TimerTask task) {
        if (scheduledRefreshTask == task) {
            scheduledRefreshTask = null;
            scheduledRefreshReason = null;
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
        clearRefreshTimer(RefreshCancellationReason.EXPLICIT_CLEAR);
    }

    private synchronized void clearRefreshTimer(RefreshCancellationReason reason) {
        IterableAuthRefreshReason cancelledReason = scheduledRefreshReason;
        if (scheduledRefreshTask != null) {
            scheduledRefreshTask.cancel();
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        scheduledRefreshTask = null;
        scheduledRefreshReason = null;
        if (cancelledReason != null) {
            IterableLogger.d(
                    TAG,
                    "auth_refresh action=cancel reason="
                            + cancelledReason
                            + " cause="
                            + reason
            );
        }
    }

    @Override
    public void onSwitchToForeground() {
        try {
            IterableLogger.d(TAG, "App switched to foreground - enabling auth token requests");
            isInForeground = true;
            if (authDataRestorer != null && authDataRestorer.resumeIfUnresolved()) {
                return;
            }
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
            clearRefreshTimer(RefreshCancellationReason.APP_BACKGROUNDED);
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while switching to background", e);
        }
    }
}
