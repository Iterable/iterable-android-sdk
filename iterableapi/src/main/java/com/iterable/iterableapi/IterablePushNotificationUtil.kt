package com.iterable.iterableapi

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.RemoteInput
import org.json.JSONException
import org.json.JSONObject

internal class IterablePushNotificationUtil {
    companion object {
        private var pendingAction: PendingAction? = null
        private const val TAG = "IterablePushNotificationUtil"

        @JvmStatic
        fun processPendingAction(context: Context): Boolean {
            var handled = false
            pendingAction?.let { action ->
                handled = executeAction(context, action)
                pendingAction = null
            }
            return handled
        }

        @JvmStatic
        fun executeAction(context: Context, action: PendingAction): Boolean {
            // Automatic tracking
            IterableApi.sharedInstance.setPayloadData(action.intent)
            IterableApi.sharedInstance.setNotificationData(action.notificationData)
            IterableApi.sharedInstance.trackPushOpen(
                action.notificationData.campaignId,
                action.notificationData.templateId,
                action.notificationData.messageId,
                action.dataFields
            )

            return IterableActionRunner.executeAction(context, action.iterableAction, IterableActionSource.PUSH)
        }

        @JvmStatic
        fun handlePushAction(context: Context, intent: Intent) {
            if (intent.extras == null) {
                IterableLogger.e(TAG, "handlePushAction: extras == null, can't handle push action")
                return
            }
            val notificationData = IterableNotificationData(intent.extras!!)
            val actionIdentifier = intent.getStringExtra(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER)
            var action: IterableAction? = null
            val dataFields = JSONObject()

            var openApp = true

            if (actionIdentifier != null) {
                try {
                    if (actionIdentifier == IterableConstants.ITERABLE_ACTION_DEFAULT) {
                        // Default action (click on a push)
                        dataFields.put(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, IterableConstants.ITERABLE_ACTION_DEFAULT)
                        action = notificationData.defaultAction
                        if (action == null) {
                            action = getLegacyDefaultActionFromPayload(intent.extras!!)
                        }
                    } else {
                        dataFields.put(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, actionIdentifier)
                        val button = notificationData.getActionButton(actionIdentifier)
                        action = button.action
                        openApp = button.openApp

                        if (button.buttonType == IterableNotificationData.Button.BUTTON_TYPE_TEXT_INPUT) {
                            val results = RemoteInput.getResultsFromIntent(intent)
                            if (results != null) {
                                val userInput = results.getString(IterableConstants.USER_INPUT)
                                if (userInput != null) {
                                    dataFields.putOpt(IterableConstants.KEY_USER_TEXT, userInput)
                                    action!!.userInput = userInput
                                }
                            }
                        }
                    }
                } catch (e: JSONException) {
                    IterableLogger.e(TAG, "Encountered an exception while trying to handle the push action", e)
                }
            }
            pendingAction = PendingAction(intent, notificationData, action, openApp, dataFields)

            var handled = false
            if (IterableApi.getInstance().mainActivityContext != null) {
                handled = processPendingAction(context)
            }

            // Open the launcher activity if the action was not handled by anything, and openApp is true
            if (openApp && !handled) {
                val launcherIntent = IterableNotificationHelper.getMainActivityIntent(context)
                launcherIntent.putExtras(intent.extras!!)
                launcherIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (launcherIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(launcherIntent)
                }
            }
        }

        private fun getLegacyDefaultActionFromPayload(extras: Bundle): IterableAction? {
            return try {
                if (extras.containsKey(IterableConstants.ITERABLE_DATA_DEEP_LINK_URL)) {
                    val actionJson = JSONObject()
                    actionJson.put("type", IterableAction.ACTION_TYPE_OPEN_URL)
                    actionJson.put("data", extras.getString(IterableConstants.ITERABLE_DATA_DEEP_LINK_URL))
                    IterableAction.from(actionJson)
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        private class PendingAction(
            val intent: Intent,
            val notificationData: IterableNotificationData,
            val iterableAction: IterableAction?,
            val openApp: Boolean,
            val dataFields: JSONObject
        )

        @JvmStatic
        fun dismissNotificationPanel(context: Context) {
            // On Android 12 and above, ACTION_CLOSE_SYSTEM_DIALOGS is deprecated and requires system permission
            // The notification shade will automatically close when launching an activity, so we don't need to do anything
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                try {
                    context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
                } catch (e: SecurityException) {
                    IterableLogger.w(TAG, e.localizedMessage)
                }
            }
        }

        @JvmStatic
        fun dismissNotification(context: Context, notificationIntent: Intent) {
            // Dismiss the notification
            val requestCode = notificationIntent.getIntExtra(IterableConstants.REQUEST_CODE, 0)
            val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.cancel(requestCode)
        }
    }
}