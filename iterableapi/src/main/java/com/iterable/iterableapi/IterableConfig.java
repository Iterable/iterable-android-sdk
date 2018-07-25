package com.iterable.iterableapi;

/**
 *
 */
public class IterableConfig {

    /**
     * Push integration name – used for token registration.
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
     * GCM sender ID for the previous integration
     * Only set this if you're migrating from GCM to FCM and they're in different projects / have different sender IDs
     */
    final String legacyGCMSenderId;

    private IterableConfig(Builder builder) {
        pushIntegrationName = builder.pushIntegrationName;
        urlHandler = builder.urlHandler;
        customActionHandler = builder.customActionHandler;
        legacyGCMSenderId = builder.legacyGCMSenderId;
    }

    public static class Builder {
        private String pushIntegrationName;
        private IterableUrlHandler urlHandler;
        private IterableCustomActionHandler customActionHandler;
        private String legacyGCMSenderId;

        public Builder() {}

        /**
         * Push integration name – used for token registration
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
         * Set the GCM sender ID for the previous integration
         * Only set this if you're migrating from GCM to FCM and they're in different projects / have different sender IDs
         * @param legacyGCMSenderId legacy GCM sender ID
         */
        public Builder setLegacyGCMSenderId(String legacyGCMSenderId) {
            this.legacyGCMSenderId = legacyGCMSenderId;
            return this;
        }

        public IterableConfig build() {
            return new IterableConfig(this);
        }
    }

}
