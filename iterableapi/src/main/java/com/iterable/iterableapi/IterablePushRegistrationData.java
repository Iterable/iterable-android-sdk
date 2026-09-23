package com.iterable.iterableapi;

/**
 * Created by David Truong dt@iterable.com.
 */
class IterablePushRegistrationData {

    public enum PushRegistrationAction {
        ENABLE,
        DISABLE
    }

    String email;
    String userId;
    String pushIntegrationName;
    String projectNumber = "";
    String messagingPlatform = IterableConstants.MESSAGING_PLATFORM_FIREBASE;
    String authToken;
    PushRegistrationAction pushRegistrationAction;

    /**
     * API key captured when the registration was initiated. Getting the FCM token is network-bound,
     * so the live key can change while this task is in flight; a disable that goes out with the wrong
     * key hits the wrong project. Null means "use whatever key is live when the request is sent".
     */
    String apiKey;

    /**
     * Region endpoint captured alongside {@link #apiKey}, for the same reason: the endpoint is
     * otherwise resolved from the live config when the request is sent, so a switch to a project in
     * a different data region would pair the captured key with the new project's endpoint. Null means
     * "use whatever region is live when the request is sent".
     */
    String baseUrl;

    /**
     * Notified once the registration task has finished handing its request to the request layer, or
     * has established that there is nothing to send.
     */
    interface DispatchListener {
        /**
         * @param dispatched true when a request was actually built and handed off. False when there
         *                   was nothing to send: no push integration name, no device token, or the
         *                   token lookup failed.
         */
        void onDispatched(boolean dispatched);
    }

    DispatchListener dispatchListener;

    /**
     * Notified if the request this task dispatches comes back as a failure. Only set by callers that
     * need to know, so the usual fire-and-forget registration is unaffected.
     */
    IterableHelper.FailureHandler onFailure;


    IterablePushRegistrationData(String email, String userId, String pushIntegrationName, String projectNumber, String messagingPlatform, PushRegistrationAction pushRegistrationAction) {
        this.email = email;
        this.userId = userId;
        this.pushIntegrationName = pushIntegrationName;
        this.projectNumber = projectNumber;
        this.messagingPlatform = messagingPlatform;
        this.pushRegistrationAction = pushRegistrationAction;
    }

    IterablePushRegistrationData(String email, String userId, String authToken, String pushIntegrationName, PushRegistrationAction pushRegistrationAction) {
        this.email = email;
        this.userId = userId;
        this.pushIntegrationName = pushIntegrationName;
        this.pushRegistrationAction = pushRegistrationAction;
        this.authToken = authToken;
    }
}
