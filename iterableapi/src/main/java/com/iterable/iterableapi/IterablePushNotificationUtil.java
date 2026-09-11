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

    /**
     * Result of attempting to dispatch a pending push action.
     *
     * Used to cleanly separate two concerns that a single boolean cannot express:
     * (1) whether pendingAction should be cleared, and
     * (2) whether the openApp launcher fallback in handlePushAction should fire.
     */
    enum ActionDispatchResult {
        /** customActionHandler was null — SDK not yet initialized. Keep pendingAction for retry
         *  once initialize() is called (SDK-307). */
        NOT_DISPATCHED,
        /** Handler was invoked but returned false, or URL action failed to open.
         *  Clear pendingAction (action consumed by invocation); allow openApp fallback. */
        DISPATCHED_WITH_FALLBACK,
        /** Handler returned true, or URL was opened successfully.
         *  Clear pendingAction; suppress openApp fallback. */
        HANDLED
    }

    static void clearPendingAction() {
        pendingAction = null;
    }

    static boolean processPendingAction(Context context) {
        if (pendingAction == null) {
            return false;
        }
        ActionDispatchResult result = dispatchPendingAction(context, pendingAction);
        if (result != ActionDispatchResult.NOT_DISPATCHED) {
            pendingAction = null;
        }
        return result == ActionDispatchResult.HANDLED;
    }

    /**
     * Dispatch a pending action and return a typed result that distinguishes
     * "not dispatched" (retry later) from "dispatched but returned false" (consumed, allow fallback)
     * from "fully handled" (consumed, suppress fallback).
     */
    private static ActionDispatchResult dispatchPendingAction(Context context, PendingAction action) {
        // Automatic tracking
        IterableApi.sharedInstance.setPayloadData(action.intent);
        IterableApi.sharedInstance.setNotificationData(action.notificationData);
        IterableApi.sharedInstance.trackPushOpen(action.notificationData.getCampaignId(), action.notificationData.getTemplateId(),
                action.notificationData.getMessageId(), action.dataFields);

        // For custom actions, check handler presence BEFORE dispatching so we can tell
        // NOT_DISPATCHED (null handler, retry later) from DISPATCHED_WITH_FALLBACK
        // (handler ran but returned false — documented as "Reserved for future use").
        if (action.iterableAction != null
                && !action.iterableAction.isOfType(IterableAction.ACTION_TYPE_OPEN_URL)) {
            boolean handlerPresent = IterableApi.sharedInstance.config != null
                    && IterableApi.sharedInstance.config.customActionHandler != null;
            if (!handlerPresent) {
                return ActionDispatchResult.NOT_DISPATCHED;
            }
        }

        boolean handled = IterableActionRunner.executeAction(context, action.iterableAction, IterableActionSource.PUSH);
        return handled ? ActionDispatchResult.HANDLED : ActionDispatchResult.DISPATCHED_WITH_FALLBACK;
    }

    static boolean executeAction(Context context, PendingAction action) {
        return dispatchPendingAction(context, action) == ActionDispatchResult.HANDLED;
    }

    static void handlePushAction(Context context, Intent intent) {
        if (intent.getExtras() == null) {
            IterableLogger.e(TAG, "handlePushAction: extras == null, can't handle push action");
            return;
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
