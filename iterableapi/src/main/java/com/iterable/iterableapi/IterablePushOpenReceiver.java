package com.iterable.iterableapi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CallSuper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Dictionary;
import java.util.Map;


/**
 * Created by davidtruong on 4/6/16.
 *
 * The IterablePushOpenReceiver should be used to handle broadcasts to track push opens.
 * The sending intent should use the action: IterableConstants.ACTION_NOTIF_OPENED
 * Additionally include the extra data passed down from GCM receive intent.
 */
public class IterablePushOpenReceiver extends BroadcastReceiver {
    static final String TAG = "IterablePushOpenReceiver";

    /**
     * IterablePushOpenReceiver handles the broadcast for tracking a pushOpen.
     * @param context
     * @param intent
     */
    @CallSuper
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (intentAction.equals(IterableConstants.ACTION_NOTIF_OPENED)){
            Bundle extras = intent.getExtras();
            if (extras != null && !extras.isEmpty() && extras.containsKey(IterableConstants.ITERABLE_DATA_KEY))
            {
                String iterableDataString = extras.getString(IterableConstants.ITERABLE_DATA_KEY);
                IterableNotificationData iterableNotificationData = new IterableNotificationData(iterableDataString);

                //TODO: storeCampaignID/Template for 24 hrs to match web
                //Need local storage on device
                //Currently this is only set for the given session

                if (IterableApi.sharedInstance != null) {
                    IterableApi.sharedInstance.setPayloadData(extras);
                    IterableApi.sharedInstance.setNotificationData(iterableNotificationData);
                    IterableApi.sharedInstance.trackPushOpen(iterableNotificationData.getCampaignId(), iterableNotificationData.getTemplateId());
                }
            } else {
                //TODO: Tried to track a push open that was did not contain iterable extraData
            }
        }
    }
}
