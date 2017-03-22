package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableFirebaseInstanceIDService extends FirebaseInstanceIdService {

    static final String TAG = "itblFCMInstanceService";

    /**
     * Register the new token if this the first upgrade to Firebase
     */
    @Override
    public void onTokenRefresh() {
        String registrationToken = FirebaseInstanceId.getInstance().getToken();

        Context mainContext = IterableApi.sharedInstance.getMainActivityContext();
        if (mainContext != null) {
            SharedPreferences sharedPref = mainContext.getSharedPreferences(IterableConstants.PUSH_APP_ID, Context.MODE_PRIVATE);
            String pushIdPref = sharedPref.getString(IterableConstants.PUSH_APP_ID, null);
            if (registrationToken != null && pushIdPref != null && !pushIdPref.equalsIgnoreCase(IterableConstants.PUSH_APP_ID)) {
                IterableLogger.w(TAG, "Refreshed fcm token: " + registrationToken);
                IterableApi.sharedInstance.registerDeviceToken(pushIdPref, registrationToken);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(IterableConstants.PUSH_APP_ID, IterableConstants.PUSH_APP_ID);
                editor.commit();
            }
        }
    }
}
