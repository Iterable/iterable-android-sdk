package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by David Truong dt@iterable.com
 */
class IterablePushRegistrationTask extends AsyncTask<IterablePushRegistrationData, Void, Void> {
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
                disableOldDeviceIfNeeded();
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

    /**
     * If {@link IterableConfig#legacyGCMSenderId} is specified, this will attempt to retrieve the old token
     * and disable it to avoid duplicate notifications
     */
    private void disableOldDeviceIfNeeded() {
        try {
            Context applicationContext = IterableApi.sharedInstance.getMainActivityContext();
            String gcmSenderId = IterableApi.sharedInstance.config.legacyGCMSenderId;
            if (gcmSenderId != null && gcmSenderId.length() > 0 && !gcmSenderId.equals(Util.getSenderId(applicationContext))) {
                final SharedPreferences sharedPref = applicationContext.getSharedPreferences(IterableConstants.PUSH_APP_ID, Context.MODE_PRIVATE);
                boolean migrationDone = sharedPref.getBoolean(IterableConstants.SHARED_PREFS_FCM_MIGRATION_DONE_KEY, false);
                if (!migrationDone) {
                    String oldToken = Util.getFirebaseToken(gcmSenderId, IterableConstants.MESSAGING_PLATFORM_GOOGLE);

                    // We disable the device on Iterable but keep the token
                    if (oldToken != null) {
                        IterableApi.sharedInstance.disableToken(iterablePushRegistrationData.email, iterablePushRegistrationData.userId, iterablePushRegistrationData.authToken, oldToken, new IterableHelper.SuccessHandler() {
                            @Override
                            public void onSuccess(@NonNull JSONObject data) {
                                sharedPref.edit().putBoolean(IterableConstants.SHARED_PREFS_FCM_MIGRATION_DONE_KEY, true).apply();
                            }
                        }, null);
                    }
                }
            }
        } catch (Exception e) {
            IterableLogger.e(TAG, "Exception while trying to disable the old device token", e);
        }
    }

    static class Util {
        static UtilImpl instance = new UtilImpl();

        static String getFirebaseToken() {
            return instance.getFirebaseToken();
        }

        static String getFirebaseToken(String senderId, String platform) throws IOException {
            return instance.getFirebaseToken(senderId, platform);
        }

        static String getSenderId(Context applicationContext) {
            return instance.getSenderId(applicationContext);
        }

        static class UtilImpl {
            String getFirebaseToken() {
                FirebaseInstanceId instanceID = FirebaseInstanceId.getInstance();
                return instanceID.getToken();
            }

            String getFirebaseToken(String senderId, String platform) throws IOException {
                FirebaseInstanceId instanceId = FirebaseInstanceId.getInstance();
                return instanceId.getToken(senderId, platform);
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


