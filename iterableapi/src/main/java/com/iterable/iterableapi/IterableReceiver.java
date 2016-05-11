package com.iterable.iterableapi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by davidtruong on 4/6/16.
 *
 * The IterableReceiver should be used to handle broadcasts to track push opens.
 * The sending intent should use the action: IterableConstants.NOTIF_OPENED
 * Additionally include the extra data passed down from GCM receive intent.
 */
public class IterableReceiver extends BroadcastReceiver {
    static final String TAG = "IterableReceiver";

    private static final String ACTION_GCM_RECEIVE_INTENT = "com.google.android.c2dm.intent.RECEIVE";
    private static final String ACTION_GCM_REGISTRATION_INTENT = "com.google.android.c2dm.intent.REGISTRATION";

    /**
     * IterableReceiver handles the broadcast for tracking a pushOpen.
     * @param context
     * @param intent
     */
    @CallSuper
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();

        if (intentAction.equals(ACTION_GCM_RECEIVE_INTENT)) {
            //TODO: handle the case where the app was already opened in the foreground, send  in-app notif instead
            //http://stackoverflow.com/questions/3667022/checking-if-an-android-application-is-running-in-the-background
            //http://stackoverflow.com/questions/23558986/gcm-how-do-i-detect-if-app-is-open-and-if-so-pop-up-an-alert-box-instead-of-nor

            //TODO: Process deeplinks here Create an override for the defaultClass opened.
            //Deep Link Example
            //Passed in from the Push Payload: { "deepLink": "main_page" }

            Class classObj = IterableApi.sharedInstance._mainActivity.getClass();

            //TODO: set the notification icon in a config file (set by developer)
            //int notificationIconId = context.getResources().getIdentifier("notification_icon", "drawable", context.getPackageName());
            int iconId = IterableApi.sharedInstance._mainActivity.getApplicationContext().getApplicationInfo().icon;

            if (iconId != 0) {
                //TODO: ensure that this is never null since people might call notificationBuilder.something()
                IterableNotification notificationBuilder = IterableNotification.createIterableNotification(
                        context, intent, classObj, iconId); //have a default for no icon.

                IterableNotification.postNotificationOnDevice(context, notificationBuilder);
            }
            else {
                //no default notif icon defined.
            }
        } else if (intentAction.equals(IterableConstants.NOTIF_OPENED)){
            Bundle extras = intent.getExtras();
            if (extras != null && !extras.isEmpty() && extras.containsKey("itbl"))
            {
                String iterableData = extras.getString("itbl");

                //DEBUG
//                Toast.makeText(context, "Sending Iterable push open data: " + iterableData, Toast.LENGTH_LONG).show();

                //TODO: storeCampaignID/Template for 24 hrs to match web
                //Need local storage on device
                //Currently this is only set for the given session

                int campaignId = 0;
                int templateId = 0;
                try {
                    JSONObject iterableJson = new JSONObject(iterableData);
                    //TODO: do we need data validation for the params?
                    campaignId = iterableJson.getInt("campaignId");
                    templateId = iterableJson.getInt("templateId");

                    //TODO: do we need to parse out any additional dataFields to pass to trackPushOpen?

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (IterableApi.sharedInstance != null) {
                    IterableApi.sharedInstance.trackPushOpen(campaignId, templateId);
                }
            } else {
                //TODO: Tried to track a push open that was did not contain iterable extraData
            }
        } else if (intentAction.equals(ACTION_GCM_REGISTRATION_INTENT)) {
            Log.d("IterableReceiver", "received GCM registration intent action");
        }
    }
}
