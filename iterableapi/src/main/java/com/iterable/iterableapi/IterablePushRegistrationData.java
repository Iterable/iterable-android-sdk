package com.iterable.iterableapi;

/**
 * Created by David Truong dt@iterable.com.
 */
class IterablePushRegistrationData {

    public enum PushRegistrationAction {
        ENABLE,
        DISABLE
    }

    String pushIntegrationName;
    String projectNumber = "";
    String messagingPlatform = IterableConstants.MESSAGING_PLATFORM_FIREBASE;
    PushRegistrationAction pushRegistrationAction;


    public IterablePushRegistrationData(String pushIntegrationName, String projectNumber, String messagingPlatform, PushRegistrationAction pushRegistrationAction){
        this.pushIntegrationName = pushIntegrationName;
        this.projectNumber = projectNumber;
        this.messagingPlatform = messagingPlatform;
        this.pushRegistrationAction = pushRegistrationAction;
    }

    public IterablePushRegistrationData(String pushIntegrationName, PushRegistrationAction pushRegistrationAction) {
        this.pushIntegrationName = pushIntegrationName;
        this.pushRegistrationAction = pushRegistrationAction;
    }
}
