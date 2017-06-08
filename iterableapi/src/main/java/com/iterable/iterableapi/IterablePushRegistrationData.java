package com.iterable.iterableapi;

/**
 * Created by David Truong dt@iterable.com.
 */
class IterablePushRegistrationData {

    public enum PushRegistrationAction {
        ENABLE,
        DISABLE,
        GET
    }

    String iterableAppId;
    String projectNumber = "";
    String messagingPlatform = IterableConstants.MESSAGING_PLATFORM_GOOGLE;
    PushRegistrationAction pushRegistrationAction;


    public IterablePushRegistrationData(String iterableAppId, String projectNumber, String messagingPlatform, PushRegistrationAction pushRegistrationAction){
        this.iterableAppId = iterableAppId;
        this.projectNumber = projectNumber;
        this.messagingPlatform = messagingPlatform;
        this.pushRegistrationAction = pushRegistrationAction;
    }
}
