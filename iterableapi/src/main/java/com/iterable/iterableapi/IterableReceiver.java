package com.iterable.iterableapi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CallSuper;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by davidtruong on 4/6/16.
 *
 * The IterableReceiver should be used to handle broadcasts to track push opens.
 * The sending intent should use the action: IterableConstants.ACTION_NOTIF_OPENED
 * Additionally include the extra data passed down from GCM receive intent.
 */
public class IterableReceiver extends BroadcastReceiver {
    static final String TAG = "IterableReceiver";


    /**
     * IterableReceiver handles the broadcast for tracking a pushOpen.
     * @param context
     * @param intent
     */
    @CallSuper
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (intentAction.equals(IterableConstants.ACTION_NOTIF_OPENED)){
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
        }
    }
}
