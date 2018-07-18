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

    @Override
    public void onTokenRefresh() {
        String registrationToken = FirebaseInstanceId.getInstance().getToken();
        IterableLogger.d(TAG, "New Firebase Token generated: " + registrationToken);
        IterableApi.getInstance().registerForPush();
    }
}