package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;

/**
 * Created by David Truong dt@iterable.com
 */
class IterablePushRegistration extends AsyncTask<IterablePushRegistrationData, Void, Void> {
    static final String TAG = "IterablePushRegistration";

    /**
     * Registers or disables the device
     *
     * @param params Push registration request data
     */
    protected Void doInBackground(IterablePushRegistrationData... params) {
        IterablePushRegistrationData iterablePushRegistrationData = params[0];
        if (iterablePushRegistrationData.pushIntegrationName != null) {
            PushRegistrationObject pushRegistrationObject = getDeviceToken();
            if (pushRegistrationObject != null) {
                if (iterablePushRegistrationData.pushRegistrationAction == IterablePushRegistrationData.PushRegistrationAction.ENABLE) {
                    disableOldDeviceIfNeeded();
                    IterableApi.sharedInstance.registerDeviceToken(iterablePushRegistrationData.pushIntegrationName, pushRegistrationObject.token);
                } else if (iterablePushRegistrationData.pushRegistrationAction == IterablePushRegistrationData.PushRegistrationAction.DISABLE) {
                    IterableApi.sharedInstance.disableToken(pushRegistrationObject.token);
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

            int firebaseResourceId = Util.getFirebaseResouceId(applicationContext);
            if (firebaseResourceId == 0) {
                IterableLogger.e(TAG, "Could not find firebase_database_url, please check that Firebase SDK is set up properly");
                return null;
            }

            FirebaseInstanceId instanceID = FirebaseInstanceId.getInstance();
            return new PushRegistrationObject(instanceID.getToken());

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
                    String oldToken = FirebaseInstanceId.getInstance().getToken(gcmSenderId, IterableConstants.MESSAGING_PLATFORM_GOOGLE);

                    // We disable the device on Iterable but keep the token
                    if (oldToken != null) {
                        IterableApi.sharedInstance.disableToken(oldToken, new IterableHelper.SuccessHandler() {
                            @Override
                            public void onSuccess(JSONObject data) {
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
        static int getFirebaseResouceId(Context applicationContext) {
            return applicationContext.getResources().getIdentifier(IterableConstants.FIREBASE_RESOURCE_ID, IterableConstants.ANDROID_STRING, applicationContext.getPackageName());
        }

        static String getSenderId(Context applicationContext) {
            int resId = applicationContext.getResources().getIdentifier(IterableConstants.FIREBASE_SENDER_ID, IterableConstants.ANDROID_STRING, applicationContext.getPackageName());
            if (resId != 0) {
                return applicationContext.getResources().getString(resId);
            } else {
                return null;
            }
        }
    }


    static class PushRegistrationObject {
        String token;
        String messagingPlatform;

        public PushRegistrationObject(String token) {
            this.token = token;
            this.messagingPlatform = IterableConstants.MESSAGING_PLATFORM_FIREBASE;
        }
    }
}


