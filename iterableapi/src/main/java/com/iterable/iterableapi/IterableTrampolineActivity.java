package com.iterable.iterableapi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class IterableTrampolineActivity extends AppCompatActivity {

    private static final String TAG = "TrampolineActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IterableLogger.v(TAG, "Notification Trampoline Activity created");
    }

    @Override
    protected void onResume() {
        super.onResume();
        IterableLogger.v(TAG, "Notification Trampoline Activity resumed");

        Intent notificationIntent = getIntent();
        if (notificationIntent == null) {
            IterableLogger.d(TAG, "Intent is null. Doing nothing.");
            finish();
            return;
        }

        String actionName = notificationIntent.getAction();
        if (actionName == null) {
            IterableLogger.d(TAG, "Intent action is null. Doing nothing.");
            finish();
            return;
        }

        IterablePushNotificationUtil.dismissNotification(this, notificationIntent);
        IterablePushNotificationUtil.dismissNotificationPanel(this);
        if (IterableConstants.ACTION_PUSH_ACTION.equalsIgnoreCase(actionName)) {
            IterablePushNotificationUtil.handlePushAction(this, notificationIntent);
        }
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        IterableLogger.v(TAG, "Notification Trampoline Activity on pause");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IterableLogger.v(TAG, "Notification Trampoline Activity destroyed");
    }
}