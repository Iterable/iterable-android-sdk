package com.iterable.iterableapi

import androidx.appcompat.app.AppCompatActivity

import android.content.Intent
import android.os.Bundle

class IterableTrampolineActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TrampolineActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IterableLogger.v(TAG, "Notification Trampoline Activity created")
    }

    override fun onResume() {
        super.onResume()
        IterableLogger.v(TAG, "Notification Trampoline Activity resumed")

        val notificationIntent = intent
        if (notificationIntent == null) {
            IterableLogger.d(TAG, "Intent is null. Doing nothing.")
            finish()
            return
        }

        val actionName = notificationIntent.action
        if (actionName == null) {
            IterableLogger.d(TAG, "Intent action is null. Doing nothing.")
            finish()
            return
        }

        IterablePushNotificationUtil.dismissNotification(this, notificationIntent)
        IterablePushNotificationUtil.dismissNotificationPanel(this)
        if (IterableConstants.ACTION_PUSH_ACTION.equals(actionName, ignoreCase = true)) {
            IterablePushNotificationUtil.handlePushAction(this, notificationIntent)
        }
        finish()
    }

    override fun onPause() {
        super.onPause()
        IterableLogger.v(TAG, "Notification Trampoline Activity on pause")
    }

    override fun onDestroy() {
        super.onDestroy()
        IterableLogger.v(TAG, "Notification Trampoline Activity destroyed")
    }
}