package com.iterable.iterableapi;

import android.content.Intent;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by davidtruong on 4/29/16.
 */
public class IterableHelper {
    public static boolean isGhostPush(Bundle extras) {
        boolean isGhostPush = false;

//        Bundle extras = intent.getExtras();
        if (extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)) {
            String iterableData = extras.getString(IterableConstants.ITERABLE_DATA_KEY);
            IterableNotificationData data = new IterableNotificationData(iterableData);
            isGhostPush = data.getIsGhostPush();
        }

        return isGhostPush;
    }
}