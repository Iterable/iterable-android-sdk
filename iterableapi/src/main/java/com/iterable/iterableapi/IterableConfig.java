package com.iterable.iterableapi;

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
     * GCM sender ID for the previous integration
     * Only set this if you're migrating from GCM to FCM and they're in different projects / have different sender IDs
     */
    final String legacyGCMSenderId;

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

    private IterableConfig(Builder builder) {
        pushIntegrationName = builder.pushIntegrationName;
        urlHandler = builder.urlHandler;
        customActionHandler = builder.customActionHandler;
        autoPushRegistration = builder.autoPushRegistration;
        legacyGCMSenderId = builder.legacyGCMSenderId;
        checkForDeferredDeeplink = builder.checkForDeferredDeeplink;
        logLevel = builder.logLevel;
        inAppHandler = builder.inAppHandler;
    }

    public static class Builder {
        private String pushIntegrationName;
        private IterableUrlHandler urlHandler;
        private IterableCustomActionHandler customActionHandler;
        private boolean autoPushRegistration = true;
        private String legacyGCMSenderId;
        private boolean checkForDeferredDeeplink;
        private int logLevel = Log.ERROR;
        private IterableInAppHandler inAppHandler = new IterableDefaultInAppHandler();

        public Builder() {}

        /**
         * Push integration name - used for token registration
         * Make sure the name of this integration matches the one set up in Iterable console
         * @param pushIntegrationName Push integration name
         */
        public Builder setPushIntegrationName(String pushIntegrationName) {
            this.pushIntegrationName = pushIntegrationName;
            return this;
        }

        /**
         * Set a custom URL handler to override openUrl actions
         * @param urlHandler Custom URL handler provided by the app
         */
        public Builder setUrlHandler(IterableUrlHandler urlHandler) {
            this.urlHandler = urlHandler;
            return this;
        }

        /**
         * Set an action handler for custom actions
         * @param customActionHandler Custom action handler provided by the app
         */
        public Builder setCustomActionHandler(IterableCustomActionHandler customActionHandler) {
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
        public Builder setAutoPushRegistration(boolean enabled) {
            this.autoPushRegistration = enabled;
            return this;
        }

        /**
         * Set the GCM sender ID for the previous integration
         * Only set this if you're migrating from GCM to FCM and they're in different projects / have different sender IDs
         * @param legacyGCMSenderId legacy GCM sender ID
         */
        public Builder setLegacyGCMSenderId(String legacyGCMSenderId) {
            this.legacyGCMSenderId = legacyGCMSenderId;
            return this;
        }

        /**
         * When set to true, it will check for deferred deep links on first time app launch
         * after installation.
         * @param checkForDeferredDeeplink Enable deferred deep link checks on first launch
         */
        public Builder setCheckForDeferredDeeplink(boolean checkForDeferredDeeplink) {
            this.checkForDeferredDeeplink = checkForDeferredDeeplink;
            return this;
        }

        /**
         * Set the log level for Iterable SDK log messages
         * @param logLevel Log level, defaults to {@link Log#ERROR}
         */
        public Builder setLogLevel(int logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        /**
         * Set a custom in-app handler that can be used to control whether an incoming in-app message
         * should be shown immediately or not
         * @param inAppHandler In-app handler provided by the app
         */
        public Builder setInAppHandler(IterableInAppHandler inAppHandler) {
            this.inAppHandler = inAppHandler;
            return this;
        }

        public IterableConfig build() {
            return new IterableConfig(this);
        }
    }

}
