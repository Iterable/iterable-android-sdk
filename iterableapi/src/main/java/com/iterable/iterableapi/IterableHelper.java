package com.iterable.iterableapi;

import android.content.Intent;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by davidtruong on 4/29/16.
 */
public class IterableHelper {
    public static boolean isGhostPush(Intent intent) {

        boolean isGhostPush = false;

        Bundle extras = intent.getExtras();
        if (intent.hasExtra("itbl")) {
            String iterableData = extras.getString("itbl");

            //TODO: should we change the android architecture to handle ghost pushes automatically
            try {
                JSONObject iterableJson = new JSONObject(iterableData);
                if (iterableJson.has("isGhostPush")) {
                    isGhostPush = iterableJson.getBoolean("isGhostPush");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return isGhostPush;
    }
}