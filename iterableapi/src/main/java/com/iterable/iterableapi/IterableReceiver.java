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
 */
public class IterableReceiver extends BroadcastReceiver {
    static final String TAG = "IterableReceiver";

    private static final String INTENT_ACTION_GCM_RECEIVE = "com.google.android.c2dm.intent.RECEIVE";

    @CallSuper
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        //TODO: should we handle all of the intent action here to queue up different
        //use a switch statement here for each type of notif action
        if (intentAction.equals(INTENT_ACTION_GCM_RECEIVE)){

        } else if (intentAction.equals(IterableConstants.NOTIF_OPENED)){
            Log.i(TAG, "notif_opened");
            //TODO: Process the intent data and send to the Iterable Server
            Bundle extras = intent.getExtras();
            if (extras != null && !extras.isEmpty())
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

                //TODO: pass in additional requestJson
                JSONObject requestJSON = new JSONObject();

                //TODO: check that the sharedInstance exists before using it.
                IterableApi.sharedInstance.trackPushOpen(campaignId, requestJSON);

            }
        }


    }
}
