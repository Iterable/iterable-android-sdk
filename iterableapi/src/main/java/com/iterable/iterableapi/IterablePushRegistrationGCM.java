package com.iterable.iterableapi;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

/**
 * Created by davidtruong on 5/4/16.
 */
public class IterablePushRegistrationGCM extends AsyncTask<String, Integer, String> {
    static final String TAG = "IterableRequest";


    protected String doInBackground(String... params) {
        try {
            String iterableAppId = params[0];
            String projectNumber = params[1];
//            String iterableAppId = intent.getStringExtra("IterableAppId");
//            String projectNumber = intent.getStringExtra("GCMProjectNumber");

            //TODO: look onto passing the AppID in via the androidManifest
//            PackageManager pm = IterableApi.sharedInstance._mainActivity.getPackageManager();
//            ApplicationInfo ai = pm.getApplicationInfo(IterableApi.sharedInstance._mainActivity
//                            .getPackageName(), PackageManager.GET_META_DATA);
//            Bundle bundle = ai.metaData;
//
//            //Add <meta-data android:name="IterableAppId" android:value="<registerd_app_id>" />
//            //to the androidmanifest.xml
//            String iterableAppId = bundle.getString("IterableAppId");

            if (iterableAppId != null) {
                Class instanceIdClass = Class.forName("com.google.android.gms.iid.InstanceID");
                if (instanceIdClass != null) {
                    InstanceID instanceID = InstanceID.getInstance(IterableApi.sharedInstance.getApplicationContext());
                    String registrationId = "";
                    registrationId = instanceID.getToken(projectNumber,
                            GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

                    if (!registrationId.isEmpty()) {
                        IterableApi.sharedInstance.registerDeviceToken(iterableAppId, registrationId);
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
        } //catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
        return null;
    }
}

