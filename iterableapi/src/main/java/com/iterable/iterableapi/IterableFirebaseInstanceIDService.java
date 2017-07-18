package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.json.JSONException;
import org.json.JSONObject;

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
                JSONObject data = new JSONObject();
                try {
                    data.put(IterableConstants.FIREBASE_TOKEN_TYPE, IterableConstants.MESSAGING_PLATFORM_FIREBASE);
                    data.put(IterableConstants.FIREBASE_INITIAL_UPGRADE, true);
                } catch (JSONException e) {
                    IterableLogger.e(TAG, e.toString());
                }
                IterableApi.sharedInstance.registerDeviceToken(pushIdPref, registrationToken, IterableConstants.MESSAGING_PLATFORM_FIREBASE, data);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(IterableConstants.PUSH_APP_ID, IterableConstants.PUSH_APP_ID);
                editor.commit();
            }
        }
    }
}
