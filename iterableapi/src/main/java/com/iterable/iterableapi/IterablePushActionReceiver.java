package com.iterable.iterableapi;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.v4.app.RemoteInput;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handles incoming push actions built by {@link IterableNotificationBuilder}
 * Action id is passed in the Intent extras under {@link IterableConstants#REQUEST_CODE}
 */
public class IterablePushActionReceiver extends BroadcastReceiver {
    private static final String TAG = "IterablePushActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();

        // Dismiss the notification
        int requestCode = extras.getInt(IterableConstants.REQUEST_CODE, 0);
        NotificationManager mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(requestCode);

        // Dismiss the notifications panel
        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        String actionName = intent.getAction();
        if (IterableConstants.ACTION_PUSH_ACTION.equalsIgnoreCase(actionName)) {
            handlePushAction(context, intent);
        }
    }

    private void handlePushAction(Context context, Intent intent) {
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

        // Automatic tracking
        IterableApi.sharedInstance.setPayloadData(intent);
        IterableApi.sharedInstance.setNotificationData(notificationData);
        IterableApi.sharedInstance.trackPushOpen(notificationData.getCampaignId(), notificationData.getTemplateId(), notificationData.getMessageId(), dataFields);

        boolean handled = IterableActionRunner.executeAction(context, action, IterableActionSource.PUSH);

        // Open the launcher activity if the action was not handled by anything, and openApp is true
        if (openApp && !handled) {
            Intent launcherIntent = IterableNotificationBuilder.getMainActivityIntent(context);
            launcherIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if(launcherIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(launcherIntent);
            }
        }
    }

    private IterableAction getLegacyDefaultActionFromPayload(Bundle extras) {
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

}
