package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by David Truong dt@iterable.com
 */
class IterablePushRegistration extends AsyncTask<IterablePushRegistrationData, Void, String> {
    static final String TAG = "IterablePushRegistration";

    private IterablePushRegistrationData iterablePushRegistrationData;

    IterableHelper.IterableActionHandler onResponseReceived;

    /**
     * Generates a deviceRegistrationToken from Google
     * @param params
     * @return registration token
     */
    protected String doInBackground(IterablePushRegistrationData... params) {
        PushRegistrationObject pushRegistrationObject = null;
        iterablePushRegistrationData = params[0];
        if (iterablePushRegistrationData.iterableAppId != null) {
            if (iterablePushRegistrationData.pushRegistrationAction == IterablePushRegistrationData.PushRegistrationAction.ENABLE) {
                pushRegistrationObject = getDeviceToken(iterablePushRegistrationData.projectNumber, iterablePushRegistrationData.messagingPlatform, iterablePushRegistrationData.iterableAppId, true);
                JSONObject data = new JSONObject();
                try {
                    data.put("tokenRegistrationType", iterablePushRegistrationData.messagingPlatform);
                } catch (JSONException e) {
                    IterableLogger.e(TAG, e.toString());
                }
              if (pushRegistrationObject != null) {
                  IterableApi.sharedInstance.registerDeviceToken(iterablePushRegistrationData.iterableAppId, pushRegistrationObject.token, pushRegistrationObject.messagingPlatform, data);
              }
            } else if (iterablePushRegistrationData.pushRegistrationAction == IterablePushRegistrationData.PushRegistrationAction.DISABLE) {
                pushRegistrationObject = getDeviceToken(iterablePushRegistrationData.projectNumber, iterablePushRegistrationData.messagingPlatform, iterablePushRegistrationData.iterableAppId, false);
                if (pushRegistrationObject != null) {
                    IterableApi.sharedInstance.disablePush(pushRegistrationObject.token);
                }
            }
        } else {
            IterableLogger.e("IterablePush", "The IterableAppId has not been added");
        }

        String deviceToken = null;
        if (pushRegistrationObject != null) {
            deviceToken = pushRegistrationObject.token;
        }

        return deviceToken;
    }

    /**
     * Executes the disable
     * @param registrationToken
     */
    @Override
    protected void onPostExecute(String registrationToken) {
        super.onPostExecute(registrationToken);
        if (onResponseReceived != null) {
            onResponseReceived.execute(registrationToken);
        }
    }

    /**
     *
     * @param projectNumber
     * @param messagingPlatform
     * @param applicationName
     * @return PushRegistrationObject
     */
    PushRegistrationObject getDeviceToken(String projectNumber, String messagingPlatform, String applicationName, boolean reRegisterOnTokenRefresh) {
        PushRegistrationObject registrationObject = null;
        Context applicationContext = IterableApi.sharedInstance.getMainActivityContext();

        if (applicationContext != null) {
            try {
                int firebaseResourceId = getFirebaseResouceId(applicationContext);
                if (firebaseResourceId != 0 && messagingPlatform.equalsIgnoreCase(IterableConstants.MESSAGING_PLATFORM_FIREBASE)) {
                    //FCM
                    Class fireBaseMessaging = Class.forName(IterableConstants.FIREBASE_MESSAGING_CLASS);
                    if (fireBaseMessaging != null) {
                        FirebaseInstanceId instanceID = FirebaseInstanceId.getInstance();

                        SharedPreferences sharedPref = applicationContext.getSharedPreferences(IterableConstants.PUSH_APP_ID, Context.MODE_PRIVATE);
                        String pushIdPref = sharedPref.getString(IterableConstants.PUSH_APP_ID, null);
                        // If this is the initial upgrade to FCM invalidate the GCM token by cycling the current device push tokens
                        if (pushIdPref == null) {
                            //Cache application name to attempt to re-register in IterableFirebaseInstanceIDService
                            if (reRegisterOnTokenRefresh) {
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString(IterableConstants.PUSH_APP_ID, applicationName);
                                editor.commit();
                            }

                            //IterableFirebaseInstanceIDService.onTokenRefresh gets called after the current token is deleted
                            instanceID.deleteInstanceId();
                        }
                        registrationObject = new PushRegistrationObject(instanceID.getToken(), IterableConstants.MESSAGING_PLATFORM_FIREBASE);
                    }
                } else {
                    //GCM
                    Class instanceIdClass = Class.forName(IterableConstants.INSTANCE_ID_CLASS);
                    if (instanceIdClass != null) {
                        InstanceID instanceID = InstanceID.getInstance(applicationContext);
                        registrationObject = new PushRegistrationObject(instanceID.getToken(projectNumber, GoogleCloudMessaging.INSTANCE_ID_SCOPE), IterableConstants.MESSAGING_PLATFORM_GOOGLE);
                    }
                }
            } catch (ClassNotFoundException e) {
                IterableLogger.e(TAG, "ClassNotFoundException: Check that play-services is added to the build dependencies", e);
            }
            catch (IOException e) {
                IterableLogger.e(TAG, "Invalid projectNumber", e);
            }
        } else{
            IterableLogger.e(TAG, "MainActivity Context is null");
        }
        return registrationObject;
    }

    static int getFirebaseResouceId(Context applicationContext) {
        return applicationContext.getResources().getIdentifier(IterableConstants.FIREBASE_RESOURCE_ID, IterableConstants.ANDROID_STRING, applicationContext.getPackageName());
    }

    class PushRegistrationObject {
        String token;
        String messagingPlatform;

        public PushRegistrationObject(String token, String messagingPlatform){
            this.token = token;
            this.messagingPlatform = messagingPlatform;
        }
    }
}


