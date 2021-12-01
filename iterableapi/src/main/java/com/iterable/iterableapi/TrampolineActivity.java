package com.iterable.iterableapi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class TrampolineActivity extends AppCompatActivity {

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
            finish();
            return;
        }
        String action = notificationIntent.getAction();
        if (action == null) {
            IterableLogger.d(TAG, "Notification trampoline activity received intent with null action. Doing nothing.");
            finish();
            return;
        }
        Intent pushContentIntent = new Intent(IterableConstants.ACTION_PUSH_ACTION);
        pushContentIntent.setClass(this, IterablePushActionReceiver.class);
        pushContentIntent.putExtras(notificationIntent.getExtras());
        this.sendBroadcast(pushContentIntent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();  IterableLogger.v(TAG, "Notification Trampoline Activity on pause");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IterableLogger.v(TAG, "Notification Trampoline Activity destroyed");
    }
}