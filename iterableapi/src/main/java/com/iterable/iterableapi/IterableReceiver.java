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

    private static final String INTENT_ACTION_GCM_RECEIVE = "com.google.android.c2dm.intent.RECEIVE";

    /**
     * IterableReceiver.onReceive
     * @param context
     * @param intent
     */
    @CallSuper
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        //TODO: should we handle all of the intent action here?
            //use a switch statement here for each type of notif action
        if (intentAction.equals(INTENT_ACTION_GCM_RECEIVE)){

        } else if (intentAction.equals(IterableConstants.NOTIF_OPENED)){
            Log.i(TAG, "track ");

            Bundle extras = intent.getExtras();
            if (extras != null && !extras.isEmpty() && extras.containsKey("itbl"))
            {
                //TODO: data validation
                String iterableData = extras.getString("itbl");
                Toast.makeText(context, "Sending Iterable push open data: " + iterableData, Toast.LENGTH_LONG).show();

                int campaignId = -1;
                try {
                    JSONObject iterableJson = new JSONObject(iterableData);
                    campaignId = iterableJson.getInt("campaignId");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //TODO: pass in additional requestJson from the system device
                JSONObject requestJSON = new JSONObject();

                //TODO: check that the sharedInstance exists before using it.
                IterableApi.sharedInstance.trackPushOpen(campaignId, requestJSON);

            }
        }


    }
}
