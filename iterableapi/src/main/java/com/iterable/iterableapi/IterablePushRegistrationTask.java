package com.iterable.iterableapi;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Created by David Truong dt@iterable.com
 */
class
IterablePushRegistrationTask extends AsyncTask<IterablePushRegistrationData, Void, Void> {
    static final String TAG = "IterablePushRegistration";
    IterablePushRegistrationData iterablePushRegistrationData;

    /**
     * Registers or disables the device
     * @param params Push registration request data
     */
    protected Void doInBackground(IterablePushRegistrationData... params) {
        iterablePushRegistrationData = params[0];
        if (iterablePushRegistrationData.pushIntegrationName != null) {
            PushRegistrationObject pushRegistrationObject = getDeviceToken();
            if (pushRegistrationObject != null) {
                if (iterablePushRegistrationData.pushRegistrationAction == IterablePushRegistrationData.PushRegistrationAction.ENABLE) {
                    IterableApi.sharedInstance.registerDeviceToken(
                            iterablePushRegistrationData.email,
                            iterablePushRegistrationData.userId,
                            iterablePushRegistrationData.authToken,
                            iterablePushRegistrationData.pushIntegrationName,
                            pushRegistrationObject.token,
                            IterableApi.getInstance().getDeviceAttributes());

                } else if (iterablePushRegistrationData.pushRegistrationAction == IterablePushRegistrationData.PushRegistrationAction.DISABLE) {
                    IterableApi.sharedInstance.disableToken(
                            iterablePushRegistrationData.email,
                            iterablePushRegistrationData.userId,
                            iterablePushRegistrationData.authToken,
                            pushRegistrationObject.token,
                            null,
                            null
                    );
                }
            }
        } else {
            IterableLogger.e("IterablePush", "iterablePushRegistrationData has not been specified");
        }
        return null;
    }

    /**
     * @return PushRegistrationObject
     */
    PushRegistrationObject getDeviceToken() {
        try {
            Context applicationContext = IterableApi.sharedInstance.getMainActivityContext();
            if (applicationContext == null) {
                IterableLogger.e(TAG, "MainActivity Context is null");
                return null;
            }

            String senderId = Util.getSenderId(applicationContext);
            if (senderId == null) {
                IterableLogger.e(TAG, "Could not find gcm_defaultSenderId, please check that Firebase SDK is set up properly");
                return null;
            }

            return new PushRegistrationObject(Util.getFirebaseToken());

        } catch (Exception e) {
            IterableLogger.e(TAG, "Exception while retrieving the device token: check that firebase is added to the build dependencies", e);
            return null;
        }
    }

    static class Util {
        static UtilImpl instance = new UtilImpl();

        static String getFirebaseToken() {
            return instance.getFirebaseToken();
        }

        static String getSenderId(Context applicationContext) {
            return instance.getSenderId(applicationContext);
        }

        static class UtilImpl {
            String getFirebaseToken() {
                return IterableFirebaseMessagingService.getFirebaseToken();
            }

            String getSenderId(Context applicationContext) {
                int resId = applicationContext.getResources().getIdentifier(IterableConstants.FIREBASE_SENDER_ID, IterableConstants.ANDROID_STRING, applicationContext.getPackageName());
                if (resId != 0) {
                    return applicationContext.getResources().getString(resId);
                } else {
                    return null;
                }
            }
        }
    }


    static class PushRegistrationObject {
        String token;
        String messagingPlatform;

        PushRegistrationObject(String token) {
            this.token = token;
            this.messagingPlatform = IterableConstants.MESSAGING_PLATFORM_FIREBASE;
        }
    }
}


