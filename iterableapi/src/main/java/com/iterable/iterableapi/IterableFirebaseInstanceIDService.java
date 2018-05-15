package com.iterable.iterableapi;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableFirebaseInstanceIDService extends FirebaseInstanceIdService {

    static final String TAG = "itblFCMInstanceService";

    @Override
    public void onTokenRefresh() {
        String registrationToken = FirebaseInstanceId.getInstance().getToken();
        IterableLogger.d(TAG, "New Firebase Token generated: " + registrationToken);
    }
}