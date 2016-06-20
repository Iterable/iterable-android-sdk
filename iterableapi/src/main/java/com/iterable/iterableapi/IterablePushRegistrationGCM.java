package com.iterable.iterableapi;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

/**
 * Created by David Truong dt@iterable.com
 */
class IterablePushRegistrationGCM extends AsyncTask<IterableGCMRegistrationData, Void, String> {
    static final String TAG = "IterableGCM";

    protected String doInBackground(IterableGCMRegistrationData... params) {
        try {
            IterableGCMRegistrationData iterableGCMRegistrationData = params[0];

            if (iterableGCMRegistrationData.iterableAppId != null) {
                Class instanceIdClass = Class.forName("com.google.android.gms.iid.InstanceID");
                if (instanceIdClass != null) {
                    InstanceID instanceID = InstanceID.getInstance(IterableApi.sharedInstance.getMainActivityContext());
                    String registrationToken = "";
                    String idInstance = instanceID.getId();
                    registrationToken = instanceID.getToken(iterableGCMRegistrationData.projectNumber,
                            GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                    if (!registrationToken.isEmpty()) {
                        IterableApi.sharedInstance.setPushToken(registrationToken);
                        IterableApi.sharedInstance.registerDeviceToken(iterableGCMRegistrationData.iterableAppId, registrationToken);
                    }
                }
            } else {
                Log.e("IterableGCM", "The IterableAppId has not been added to the AndroidManifest");
            }
        } catch (ClassNotFoundException e) {
            //Notes: If there is a ClassNotFoundException add
            // compile 'com.google.android.gms:play-services-gcm:7.5.0' (min version) to the gradle dependencies
            Log.e(TAG, "ClassNotFoundException: Check that play-services-gcm is added " +
                    "to the build dependencies");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Invalid projectNumber");
        }
        return null;
    }
}

class IterableGCMRegistrationData {
    String iterableAppId = "";
    String projectNumber = "";
    public IterableGCMRegistrationData(String iterableAppId, String projectNumber){
        this.iterableAppId = iterableAppId;
        this.projectNumber = projectNumber;
    }
}


