package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    @Deprecated
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
     * Retry policy for JWT Refresh.
     */
    final RetryPolicy retryPolicy;

    /**
     * By default, the SDK allows navigation/calls to URLs with the `https` protocol (e.g. deep links or external links)
     * If you'd like to allow other protocols like `http`, `tel`, etc., add them to the `allowedProtocols` array
     */
    final String[] allowedProtocols;

    /**
     * Data region determining which data center and endpoints are used by the SDK.
     */
    final IterableDataRegion dataRegion;

    /**
     * This controls whether the in-app content should be saved to disk, or only kept in memory.
     * By default, the SDK will save in-apps to disk.
     */
    final boolean useInMemoryStorageForInApps;

    /**
     * Allows for fetching embedded messages.
     */
    final boolean enableEmbeddedMessaging;

    /**
     * When set to true, disables encryption for keychain storage.
     * By default, encryption is enabled for storing sensitive user data.
     */
    final boolean keychainEncryption;

    /**
     * Handler for decryption failures of PII information.
     * Before calling this handler, the SDK will clear the PII information and create new encryption keys
     */
    final IterableDecryptionFailureHandler decryptionFailureHandler;

    /**
     * Mobile framework information for the app
     */
    @Nullable
    final IterableAPIMobileFrameworkInfo mobileFrameworkInfo;

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
        retryPolicy = builder.retryPolicy;
        allowedProtocols = builder.allowedProtocols;
        dataRegion = builder.dataRegion;
        useInMemoryStorageForInApps = builder.useInMemoryStorageForInApps;
        enableEmbeddedMessaging = builder.enableEmbeddedMessaging;
        keychainEncryption = builder.keychainEncryption;
        decryptionFailureHandler = builder.decryptionFailureHandler;
        mobileFrameworkInfo = builder.mobileFrameworkInfo;
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
        private RetryPolicy retryPolicy = new RetryPolicy(10, 6L, RetryPolicy.Type.LINEAR);
        private String[] allowedProtocols = new String[0];
        private IterableDataRegion dataRegion = IterableDataRegion.US;
        private boolean useInMemoryStorageForInApps = false;
        private boolean enableEmbeddedMessaging = false;
        private boolean keychainEncryption = true;
        private IterableDecryptionFailureHandler decryptionFailureHandler;
        private IterableAPIMobileFrameworkInfo mobileFrameworkInfo;

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
         * Set retry policy for JWT Refresh
         * @param retryPolicy
         */
        @NonNull
        public Builder setAuthRetryPolicy(@NonNull RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
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

        /**
         * Set what URLs the SDK should allow to open (in addition to `https`)
         * @param allowedProtocols an array/list of protocols (e.g. `http`, `tel`)
         */
        @NonNull
        public Builder setAllowedProtocols(@NonNull String[] allowedProtocols) {
            this.allowedProtocols = allowedProtocols;
            return this;
        }

        /**
         * Set the data region used by the SDK
         * @param dataRegion enum value that determines which endpoint to use, defaults to IterableDataRegion.US
         */
        @NonNull
        public Builder setDataRegion(@NonNull IterableDataRegion dataRegion) {
            this.dataRegion = dataRegion;
            return this;
        }

        /**
         * Set whether the SDK should store in-apps only in memory, or in file storage
         * @param useInMemoryStorageForInApps `true` will have in-apps be only in memory
         */
        @NonNull
        public Builder setUseInMemoryStorageForInApps(boolean useInMemoryStorageForInApps) {
            this.useInMemoryStorageForInApps = useInMemoryStorageForInApps;
            return this;
        }

        /**
         * Allows for fetching embedded messages.
         * @param enableEmbeddedMessaging `true` will allow automatically fetching embedded messaging.
         */
        public Builder setEnableEmbeddedMessaging(boolean enableEmbeddedMessaging) {
            this.enableEmbeddedMessaging = enableEmbeddedMessaging;
            return this;
        }

        /**
         * When set to true, disables encryption for Iterable's keychain storage.
         * By default, encryption is enabled for storing sensitive user data.
         * @param keychainEncryption Whether to disable encryption for keychain
         */
        @NonNull
        public Builder setKeychainEncryption(boolean keychainEncryption) {
            this.keychainEncryption = keychainEncryption;
            return this;
        }

        /**
         * Set a handler for decryption failures that can be used to handle data recovery
         * @param handler Decryption failure handler provided by the app
         */
        @NonNull
        public Builder setDecryptionFailureHandler(@NonNull IterableDecryptionFailureHandler handler) {
            this.decryptionFailureHandler = handler;
            return this;
        }

        /**
         * Set mobile framework information for the app
         * @param mobileFrameworkInfo Mobile framework information
         */
        @NonNull
        public Builder setMobileFrameworkInfo(@NonNull IterableAPIMobileFrameworkInfo mobileFrameworkInfo) {
            this.mobileFrameworkInfo = mobileFrameworkInfo;
            return this;
        }

        @NonNull
        public IterableConfig build() {
            return new IterableConfig(this);
        }
    }
}