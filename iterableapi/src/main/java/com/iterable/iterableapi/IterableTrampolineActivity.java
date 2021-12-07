package com.iterable.iterableapi;

import static com.iterable.iterableapi.IterablePushNotificationUtil.handlePushAction;

import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationManager;
import android.content.Context;
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
            IterableLogger.d(TAG, "Intent actino is null. Doing nothing.");
            finish();
            return;
        }


        // Dismiss the notification
        int requestCode = notificationIntent.getIntExtra(IterableConstants.REQUEST_CODE, 0);
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(requestCode);

        // Dismiss the notifications panel
        try {
            this.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        } catch (SecurityException e) {
            IterableLogger.w(TAG, e.getLocalizedMessage());
        }

        if (IterableConstants.ACTION_PUSH_ACTION.equalsIgnoreCase(actionName)) {
            handlePushAction(this, notificationIntent);
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