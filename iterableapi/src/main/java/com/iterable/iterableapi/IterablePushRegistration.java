package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.firebase.iid.FirebaseInstanceId;

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
        String registrationToken = null;
        iterablePushRegistrationData = params[0];
        if (iterablePushRegistrationData.iterableAppId != null) {
            if (iterablePushRegistrationData.pushRegistrationAction == IterablePushRegistrationData.PushRegistrationAction.ENABLE) {
                registrationToken = getDeviceToken(iterablePushRegistrationData.projectNumber, iterablePushRegistrationData.messagingPlatform, iterablePushRegistrationData.iterableAppId, true);
                IterableApi.sharedInstance.registerDeviceToken(iterablePushRegistrationData.iterableAppId, registrationToken);
            } else if (iterablePushRegistrationData.pushRegistrationAction == IterablePushRegistrationData.PushRegistrationAction.DISABLE) {
                registrationToken = getDeviceToken(iterablePushRegistrationData.projectNumber, iterablePushRegistrationData.messagingPlatform, iterablePushRegistrationData.iterableAppId, false);
                IterableApi.sharedInstance.disablePush(registrationToken);
            }
        } else {
            IterableLogger.e("IterablePush", "The IterableAppId has not been added");
        }
        return registrationToken;
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
     * @return
     */
    protected String getDeviceToken(String projectNumber, String messagingPlatform, String applicationName, boolean reRegisterOnTokenRefresh) {
        String registrationToken = null;
        Context applicationContext = IterableApi.sharedInstance.getMainActivityContext();

        if (applicationContext != null) {
            try {
                int firebaseResourceId = applicationContext.getResources().getIdentifier(IterableConstants.FIREBASE_RESOURCE_ID, "string", applicationContext.getPackageName());
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

                            //IterableFirebaseInstanceIDService.onTokenRefresh gets called after delete
                            instanceID.deleteInstanceId();
                        }
                        registrationToken = instanceID.getToken();
                    }
                } else {
                    //GCM
                    Class instanceIdClass = Class.forName(IterableConstants.INSTANCE_ID_CLASS);
                    if (instanceIdClass != null) {
                        InstanceID instanceID = InstanceID.getInstance(applicationContext);
                        registrationToken = instanceID.getToken(projectNumber, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
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
        return registrationToken;
    }
}


