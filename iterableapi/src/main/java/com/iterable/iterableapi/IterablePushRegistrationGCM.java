package com.iterable.iterableapi;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

/**
 * Created by davidtruong on 5/4/16.
 */
class IterablePushRegistrationGCM extends AsyncTask<IterableGCMRegistrationData, Void, String> {
    static final String TAG = "IterableRequest";


    protected String doInBackground(IterableGCMRegistrationData... params) {
        try {
            //TODO: perhaps loop through all the request parameters
            IterableGCMRegistrationData iterableGCMRegistrationData = params[0];

            if (iterableGCMRegistrationData.iterableAppId != null) {
                Class instanceIdClass = Class.forName("com.google.android.gms.iid.InstanceID");
                if (instanceIdClass != null) {
                    InstanceID instanceID = InstanceID.getInstance(IterableApi.sharedInstance.getApplicationContext());
                    String registrationToken = "";
                    registrationToken = instanceID.getToken(iterableGCMRegistrationData.projectNumber,
                            GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

                    if (!registrationToken.isEmpty()) {
                        IterableApi.sharedInstance.registerDeviceToken(iterableGCMRegistrationData.iterableAppId, registrationToken);
                    }
                }
            } else {
                Log.e("IterableGCM", "The IterableAppId has not been added to the AndroidManifest");
            }
        } catch (ClassNotFoundException e) {
            //Notes: If there is a ClassNotFoundException add
            // compile 'com.google.android.gms:play-services-gcm:8.4.0' to the gradle dependencies
            //TODO: what is our min supported gcm version?
            Log.e("IterableGCM", "ClassNotFoundException: Check that play-services-gcm is added " +
                    "to the build dependencies");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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


