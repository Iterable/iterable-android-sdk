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
