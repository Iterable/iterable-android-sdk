package com.iterable.iterableapi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Handles incoming push actions built by {@link IterableNotificationBuilder}
 * Action id is passed in the Intent extras under {@link IterableConstants#REQUEST_CODE}
 */
public class IterablePushActionReceiver extends BroadcastReceiver {
    private static final String TAG = "IterablePushActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        IterablePushNotificationUtil.dismissNotification(context, intent);
        IterablePushNotificationUtil.dismissNotificationPanel(context);
        String actionName = intent.getAction();
        if (IterableConstants.ACTION_PUSH_ACTION.equalsIgnoreCase(actionName)) {
            IterablePushNotificationUtil.handlePushAction(context, intent);
        }
    }
}
