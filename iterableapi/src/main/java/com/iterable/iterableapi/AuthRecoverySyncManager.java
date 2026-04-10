package com.iterable.iterableapi;

/**
 * Syncs in-app and embedded messages when the auth token recovers from a 401 JWT rejection.
 * Registered as an {@link IterableAuthManager.AuthTokenReadyListener}, which only fires on
 * INVALID → UNKNOWN/VALID transitions (i.e., after a 401, not on routine token refreshes).
 *
 * Also triggers push re-registration when autoPushRegistration is enabled.
 */
class AuthRecoverySyncManager implements IterableAuthManager.AuthTokenReadyListener {
    private static final String TAG = "AuthRecoverySyncMgr";

    private final IterableApi api;

    AuthRecoverySyncManager(IterableApi api) {
        this.api = api;
    }

    @Override
    public void onAuthTokenReady() {
        IterableLogger.d(TAG, "Auth token ready after recovery - syncing messages");

        if (api.config.autoPushRegistration) {
            api.registerForPush();
        }

        api.getInAppManager().syncInApp();
        api.getEmbeddedManager().syncMessages();
    }
}
