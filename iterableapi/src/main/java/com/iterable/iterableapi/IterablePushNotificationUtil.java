package com.iterable.iterableapi;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.app.RemoteInput;

import org.json.JSONException;
import org.json.JSONObject;

class IterablePushNotificationUtil {
    private static PendingAction pendingAction = null;
    private static final String TAG = "IterablePushNotificationUtil";

    static boolean processPendingAction(Context context) {
        boolean handled = false;
        if (pendingAction != null) {
            handled = executeAction(context, pendingAction);
            // Only clear pending action if it was handled.
            // This allows the action to be processed later when SDK is fully initialized
            // (e.g., when customActionHandler becomes available after initialize() is called).
            if (handled) {
                pendingAction = null;
            }
        }
        return handled;
    }

    static boolean executeAction(Context context, PendingAction action) {
        // Automatic tracking
        IterableApi.sharedInstance.setPayloadData(action.intent);
        IterableApi.sharedInstance.setNotificationData(action.notificationData);
        IterableApi.sharedInstance.trackPushOpen(action.notificationData.getCampaignId(), action.notificationData.getTemplateId(),
                action.notificationData.getMessageId(), action.dataFields);

        return IterableActionRunner.executeAction(context, action.iterableAction, IterableActionSource.PUSH);
    }

    static void handlePushAction(Context context, Intent intent) {
        if (intent.getExtras() == null) {
            IterableLogger.e(TAG, "handlePushAction: extras == null, can't handle push action");
            return;
        }

        // Clear any previous pending action that was never processed.
        // This prevents residual actions from accumulating if they were never handled
        // (e.g., if the SDK was never initialized or the action handler wasn't available).
        if (pendingAction != null) {
            IterableLogger.d(TAG, "Clearing previous unhandled pending action");
            pendingAction = null;
        }

        // Initialize minimal context for push handling if SDK hasn't been fully initialized.
        // This ensures custom actions can be processed even when the app is in the background
        // and the SDK hasn't been initialized yet (e.g., openApp=false scenarios).
        IterableApi.initializeForPush(context);

        IterableNotificationData notificationData = new IterableNotificationData(intent.getExtras());
        String actionIdentifier = intent.getStringExtra(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER);
        IterableAction action = null;
        JSONObject dataFields = new JSONObject();

        boolean openApp = true;

        if (actionIdentifier != null) {
            try {
                if (actionIdentifier.equals(IterableConstants.ITERABLE_ACTION_DEFAULT)) {
                    // Default action (click on a push)
                    dataFields.put(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, IterableConstants.ITERABLE_ACTION_DEFAULT);
                    action = notificationData.getDefaultAction();
                    if (action == null) {
                        action = getLegacyDefaultActionFromPayload(intent.getExtras());
                    }
                } else {
                    dataFields.put(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, actionIdentifier);
                    IterableNotificationData.Button button = notificationData.getActionButton(actionIdentifier);
                    action = button.action;
                    openApp = button.openApp;

                    if (button.buttonType.equals(IterableNotificationData.Button.BUTTON_TYPE_TEXT_INPUT)) {
                        Bundle results = RemoteInput.getResultsFromIntent(intent);
                        if (results != null) {
                            String userInput = results.getString(IterableConstants.USER_INPUT);
                            if (userInput != null) {
                                dataFields.putOpt(IterableConstants.KEY_USER_TEXT, userInput);
                                action.userInput = userInput;
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                IterableLogger.e(TAG, "Encountered an exception while trying to handle the push action", e);
            }
        }
        pendingAction = new PendingAction(intent, notificationData, action, openApp, dataFields);

        boolean handled = false;
        if (IterableApi.getInstance().getMainActivityContext() != null) {
            handled = processPendingAction(context);
        }

        // Open the launcher activity if the action was not handled by anything, and openApp is true
        if (openApp && !handled) {
            Intent launcherIntent = IterableNotificationHelper.getMainActivityIntent(context);
            launcherIntent.putExtras(intent.getExtras());
            launcherIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (launcherIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(launcherIntent);
            }
        }
    }

    private static IterableAction getLegacyDefaultActionFromPayload(Bundle extras) {
        try {
            if (extras.containsKey(IterableConstants.ITERABLE_DATA_DEEP_LINK_URL)) {
                JSONObject actionJson = new JSONObject();
                actionJson.put("type", IterableAction.ACTION_TYPE_OPEN_URL);
                actionJson.put("data", extras.getString(IterableConstants.ITERABLE_DATA_DEEP_LINK_URL));
                return IterableAction.from(actionJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class PendingAction {
        Intent intent;
        IterableNotificationData notificationData;
        IterableAction iterableAction;
        boolean openApp;
        JSONObject dataFields;

        PendingAction(Intent intent, IterableNotificationData notificationData, IterableAction iterableAction, boolean openApp, JSONObject dataFields) {
            this.intent = intent;
            this.notificationData = notificationData;
            this.iterableAction = iterableAction;
            this.openApp = openApp;
            this.dataFields = dataFields;
        }
    }

    static void dismissNotificationPanel(Context context) {
        // On Android 12 and above, ACTION_CLOSE_SYSTEM_DIALOGS is deprecated and requires system permission
        // The notification shade will automatically close when launching an activity, so we don't need to do anything
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            try {
                context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            } catch (SecurityException e) {
                IterableLogger.w(TAG, e.getLocalizedMessage());
            }
        }
    }

    static void dismissNotification(Context context, Intent notificationIntent) {
        // Dismiss the notification
        int requestCode = notificationIntent.getIntExtra(IterableConstants.REQUEST_CODE, 0);
        NotificationManager mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(requestCode);
    }
}
