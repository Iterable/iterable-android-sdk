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

public class IterablePushActionReceiver extends BroadcastReceiver {
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
                } else {
                    dataFields.put(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, actionIdentifier);
                    IterableNotificationData.Button button = notificationData.getActionButton(actionIdentifier);
                    action = button.action;
                    openApp = button.openApp;

                    if (button.buttonType.equals(IterableNotificationData.Button.BUTTON_TYPE_TEXT_INPUT)) {
                        String userInput = RemoteInput.getResultsFromIntent(intent).getString("userInput");
                        if (userInput != null) {
                            dataFields.putOpt("userText", userInput);
                            action.userInput = userInput;
                        }
                    }
                }
            } catch (JSONException e) {

            }
        }

        // Automatic tracking
        IterableApi.sharedInstance.trackPushOpen(notificationData.getCampaignId(), notificationData.getTemplateId(), notificationData.getMessageId(), dataFields);

        boolean handled = IterableActionRunner.executeAction(context, action);

        // Open the launcher activity if the action was not handled by anything, and openApp is true
        if (openApp && !handled) {
            PackageManager pm = context.getPackageManager();
            Intent launcherIntent = pm.getLaunchIntentForPackage(context.getPackageName());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(launcherIntent);
        }
    }
}
