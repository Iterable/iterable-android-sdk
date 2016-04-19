package com.iterable.iterableapi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.util.Log;
import android.widget.Toast;

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
        }

        Toast.makeText(context, "Intent Detected IterableReceiver.", Toast.LENGTH_LONG).show();

        //TODO: Process the intent data and send to the Iterable Server
        Bundle extras = intent.getExtras();
        if (extras != null && !extras.isEmpty())
        {
            String intentExtras = extras.toString();
            Log.i(TAG, "IntentExtras" + intentExtras);
        }
    }
}
