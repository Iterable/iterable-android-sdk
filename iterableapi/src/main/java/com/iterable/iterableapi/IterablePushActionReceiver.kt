package com.iterable.iterableapi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Handles incoming push actions built by [IterableNotificationBuilder]
 * Action id is passed in the Intent extras under [IterableConstants.REQUEST_CODE]
 */
class IterablePushActionReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "IterablePushActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        IterablePushNotificationUtil.dismissNotification(context, intent)
        IterablePushNotificationUtil.dismissNotificationPanel(context)
        val actionName = intent.action
        if (IterableConstants.ACTION_PUSH_ACTION.equals(actionName, ignoreCase = true)) {
            IterablePushNotificationUtil.handlePushAction(context, intent)
        }
    }
}
