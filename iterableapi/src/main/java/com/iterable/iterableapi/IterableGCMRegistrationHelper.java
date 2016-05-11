package com.iterable.iterableapi;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

import iterable.com.iterableapi.R;

/**
 * Created by davidtruong on 5/4/16.
 */
public class IterableGCMRegistrationHelper extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public IterableGCMRegistrationHelper(String name) {
        super(name);
    }

    public IterableGCMRegistrationHelper() {
        super(IterableGCMRegistrationHelper.class.getName());
    }

    @Override
    public void onHandleIntent(Intent intent) {
        try {
            String iterableAppId = intent.getStringExtra("IterableAppId");
            String projectNumber = intent.getStringExtra("GCMProjectNumber");

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
                Class II = Class.forName("com.google.android.gms.iid.InstanceID");
                if (II != null) {
                    InstanceID instanceID = InstanceID.getInstance(this);
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
    }
}

