package com.iterable.iterableapi;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;

/**
 *
 * Created by David Truong dt@iterable.com
 */
public class IterablePushReceiver extends BroadcastReceiver{

    static final String TAG = "IterablePushReceiver";

    private static final String ACTION_GCM_RECEIVE_INTENT = "com.google.android.c2dm.intent.RECEIVE";
    private static final String ACTION_FCM_RECEIVE_INTENT = "com.google.firebase.MESSAGING_EVENT";

    /**
     * Receives a new IterablePushIntent.
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();

        if (intentAction.equals(ACTION_FCM_RECEIVE_INTENT)) {
            IterableLogger.d(TAG, "FCM intent received" + intent);
            handlePushReceived(context, intent);
        } else if (intentAction.equals(ACTION_GCM_RECEIVE_INTENT)) {
            IterableLogger.d(TAG, "GCM intent received" + intent);
            handlePushReceived(context, intent);
        }
    }

    /**
     * Handles receiving an incoming push notification from the intent.
     * @param context
     * @param intent
     */
    private void handlePushReceived(Context context, Intent intent) {
        if (intent.hasExtra(IterableConstants.ITERABLE_DATA_KEY)) {
            Bundle extras =  intent.getExtras();
            if (!IterableNotification.isGhostPush(extras)) {
                if (!IterableNotification.isEmptyBody(extras)) {
                    IterableLogger.d(TAG, "Iterable push received " + extras);
                    Context appContext = context.getApplicationContext();
                    PackageManager packageManager = appContext.getPackageManager();
                    Intent packageIntent = packageManager.getLaunchIntentForPackage(appContext.getPackageName());
                    ComponentName componentPackageName = packageIntent.getComponent();
                    String mainClassName = componentPackageName.getClassName();
                    Class mainClass = null;
                    try {
                        mainClass = Class.forName(mainClassName);
                    } catch (ClassNotFoundException e) {
                        IterableLogger.w(TAG, e.toString());
                    }

                    IterableNotification notificationBuilder = IterableNotification.createNotification(
                            appContext, intent.getExtras(), mainClass);
                    new IterableNotificationBuilder().execute(notificationBuilder);
                } else {
                    IterableLogger.d(TAG, "Iterable OS notification push received");
                }
            } else {
                IterableLogger.d(TAG, "Iterable ghost silent push received");
            }
        }
    }
}

class IterableNotificationBuilder extends AsyncTask<IterableNotification, Void, Void> {

    @Override
    protected Void doInBackground(IterableNotification... params) {
        if ( params != null && params[0] != null) {
            IterableNotification notificationBuilder = params[0];
            IterableNotification.postNotificationOnDevice(notificationBuilder.mContext, notificationBuilder);
        }
        return null;
    }
}