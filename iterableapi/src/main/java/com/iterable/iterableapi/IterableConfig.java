package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import android.util.Log;

/**
 *
 */
public class IterableConfig {

    /**
     * Push integration name - used for token registration.
     * Make sure the name of this integration matches the one set up in Iterable console.
     */
    final String pushIntegrationName;

    /**
     * Custom URL handler to override openUrl actions
     */
    final IterableUrlHandler urlHandler;

    /**
     * Action handler for custom actions
     */
    final IterableCustomActionHandler customActionHandler;

    /**
     * If set to `true`, the SDK will automatically register the push token when you
     * call {@link IterableApi#setUserId(String)} or {@link IterableApi#setEmail(String)}
     * and disable the old device entry when the user logs out
     */
    final boolean autoPushRegistration;

    /**
     * When set to true, it will check for deferred deep links on first time app launch
     * after installation.
     */
    final boolean checkForDeferredDeeplink;

    /**
     * Log level for Iterable SDK log messages
     */
    final int logLevel;

    /**
     * Custom in-app handler that can be used to control whether an incoming in-app message should
     * be shown immediately or not
     */
    final IterableInAppHandler inAppHandler;

    /**
     * The number of seconds to wait before showing the next in-app message, if there are multiple
     * messages in the queue
     */
    final double inAppDisplayInterval;

    /**
     * Custom auth handler that can be used to control retrieving and storing an auth token
     */
    final IterableAuthHandler authHandler;

    /**
     * Duration prior to an auth expiration that a new auth token should be requested.
     */
    final long expiringAuthTokenRefreshPeriod;

    /**
     * By default, the SDK allows navigation/calls to URLs with the `https` protocol (e.g. deep links or external links)
     * If you'd like to allow other protocols like `http`, `tel`, etc., add them to the `allowedProtocols` array
     */
    final String[] allowedProtocols;

    private IterableConfig(Builder builder) {
        pushIntegrationName = builder.pushIntegrationName;
        urlHandler = builder.urlHandler;
        customActionHandler = builder.customActionHandler;
        autoPushRegistration = builder.autoPushRegistration;
        checkForDeferredDeeplink = builder.checkForDeferredDeeplink;
        logLevel = builder.logLevel;
        inAppHandler = builder.inAppHandler;
        inAppDisplayInterval = builder.inAppDisplayInterval;
        authHandler = builder.authHandler;
        expiringAuthTokenRefreshPeriod = builder.expiringAuthTokenRefreshPeriod;
        allowedProtocols = builder.allowedProtocols;
    }

    public static class Builder {
        private String pushIntegrationName;
        private IterableUrlHandler urlHandler;
        private IterableCustomActionHandler customActionHandler;
        private boolean autoPushRegistration = true;
        private boolean checkForDeferredDeeplink;
        private int logLevel = Log.ERROR;
        private IterableInAppHandler inAppHandler = new IterableDefaultInAppHandler();
        private double inAppDisplayInterval = 30.0;
        private IterableAuthHandler authHandler;
        private long expiringAuthTokenRefreshPeriod = 60000L;
        private String[] allowedProtocols = {};
        public Builder() {}

        /**
         * Push integration name - used for token registration
         * Make sure the name of this integration matches the one set up in Iterable console
         * If this field is not set, Iterable SDK defaults it to the app's package name
         * @param pushIntegrationName Push integration name
         */
        @NonNull
        public Builder setPushIntegrationName(@NonNull String pushIntegrationName) {
            this.pushIntegrationName = pushIntegrationName;
            return this;
        }

        /**
         * Set a custom URL handler to override openUrl actions
         * @param urlHandler Custom URL handler provided by the app
         */
        @NonNull
        public Builder setUrlHandler(@NonNull IterableUrlHandler urlHandler) {
            this.urlHandler = urlHandler;
            return this;
        }

        /**
         * Set an action handler for custom actions
         * @param customActionHandler Custom action handler provided by the app
         */
        @NonNull
        public Builder setCustomActionHandler(@NonNull IterableCustomActionHandler customActionHandler) {
            this.customActionHandler = customActionHandler;
            return this;
        }

        /**
         * Enable or disable automatic push token registration
         * If set to `true`, the SDK will automatically register the push token when you
         * call {@link IterableApi#setUserId(String)} or {@link IterableApi#setEmail(String)}
         * and disable the old device entry when the user logs out
         * @param enabled Enable automatic push token registration
         */
        @NonNull
        public Builder setAutoPushRegistration(boolean enabled) {
            this.autoPushRegistration = enabled;
            return this;
        }

        /**
         * When set to true, it will check for deferred deep links on first time app launch
         * after installation.
         * @param checkForDeferredDeeplink Enable deferred deep link checks on first launch
         */
        @NonNull
        public Builder setCheckForDeferredDeeplink(boolean checkForDeferredDeeplink) {
            this.checkForDeferredDeeplink = checkForDeferredDeeplink;
            return this;
        }

        /**
         * Set the log level for Iterable SDK log messages
         * @param logLevel Log level, defaults to {@link Log#ERROR}
         */
        @NonNull
        public Builder setLogLevel(int logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        /**
         * Set a custom in-app handler that can be used to control whether an incoming in-app message
         * should be shown immediately or not
         * @param inAppHandler In-app handler provided by the app
         */
        @NonNull
        public Builder setInAppHandler(@NonNull IterableInAppHandler inAppHandler) {
            this.inAppHandler = inAppHandler;
            return this;
        }

        /**
         * Set the in-app message display interval: the number of seconds to wait before showing
         * the next in-app message, if there are multiple messages in the queue
         * @param inAppDisplayInterval display interval in seconds
         */
        @NonNull
        public Builder setInAppDisplayInterval(double inAppDisplayInterval) {
            this.inAppDisplayInterval = inAppDisplayInterval;
            return this;
        }

        /**
         * Set a custom auth handler that can be used to retrieve a new auth token
         * @param authHandler Auth handler provided by the app
         */
        @NonNull
        public Builder setAuthHandler(@NonNull IterableAuthHandler authHandler) {
            this.authHandler = authHandler;
            return this;
        }

        /**
         * Set a custom period before an auth token expires to automatically retrieve a new token
         * @param period in seconds
         */
        @NonNull
        public Builder setExpiringAuthTokenRefreshPeriod(@NonNull Long period) {
            this.expiringAuthTokenRefreshPeriod = period * 1000L;
            return this;
        }

        @NonNull
        public Builder setAllowedProtocols(@NonNull String[] allowedProtocols) {
            this.allowedProtocols = allowedProtocols;
            return this;
        }

        @NonNull
        public IterableConfig build() {
            return new IterableConfig(this);
        }
    }

}
