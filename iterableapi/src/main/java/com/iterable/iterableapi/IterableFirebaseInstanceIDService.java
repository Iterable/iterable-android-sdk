package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import iterable.com.iterableapi.R;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableFirebaseInstanceIDService extends FirebaseInstanceIdService {

    static final String TAG = "itblFCMInstanceService";

    @Override
    public void onTokenRefresh() {
        String registrationToken = FirebaseInstanceId.getInstance().getToken();

        Context mainContext = IterableApi.sharedInstance.getMainActivityContext();
        if (mainContext != null) {
            SharedPreferences sharedPref = mainContext.getSharedPreferences(IterableConstants.PUSH_APP_ID, Context.MODE_PRIVATE);
            String pushIdPref = sharedPref.getString(IterableConstants.PUSH_APP_ID, null);
            if (registrationToken != null && !registrationToken.isEmpty() && pushIdPref != null
                    && !pushIdPref.equalsIgnoreCase(IterableConstants.PUSH_APP_ID)) {
                Log.w(TAG, "Refreshed fcm token: " + registrationToken);
                IterableApi.sharedInstance.registerDeviceToken(pushIdPref, registrationToken);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(IterableConstants.PUSH_APP_ID, IterableConstants.PUSH_APP_ID);
                editor.commit();
            }
        }
    }
}
