package com.iterable.iterableapi;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

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

//    /**
//     * Handles the push registration data from the intent.
//     * @param context
//     * @param intent
//     */
//    private void handlePushRegistration(Context context, Intent intent) {
//        String iterableAppId = intent.getStringExtra(IterableConstants.PUSH_APP_ID);
//        String projectNumber = intent.getStringExtra(IterableConstants.PUSH_GCM_PROJECT_NUMBER);
//        boolean disablePush = intent.getBooleanExtra(IterableConstants.PUSH_DISABLE_AFTER_REGISTRATION, false);
//        String messagingPlatform = intent.getStringExtra(IterableConstants.MESSAGING_PUSH_SERVICE_PLATFORM);
//
////        IterablePushRegistrationData data = new IterablePushRegistrationData(iterableAppId, projectNumber, disablePush, messagingPlatform);
////        new IterablePushRegistration().execute(data);
//        IterableApi sharedInstance = IterableApi.sharedInstance;
//        String token = sharedInstance.getDeviceToken(projectNumber, messagingPlatform);
//        sharedInstance.registerDeviceToken(iterableAppId, token);
//    }

    /**
     * Handles receiving an incoming push notification from the intent.
     * @param context
     * @param intent
     */
    private void handlePushReceived(Context context, Intent intent) {
        if (intent.hasExtra(IterableConstants.ITERABLE_DATA_KEY)) {
            Bundle extras =  intent.getExtras();
            if (!IterableNotification.isGhostPush(extras)) {
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
                    e.printStackTrace();
                }

                IterableNotification notificationBuilder = IterableNotification.createNotification(
                        appContext, intent.getExtras(), mainClass);

                IterableNotification.postNotificationOnDevice(appContext, notificationBuilder);
            } else {
                IterableLogger.d(TAG, "Iterable ghost silent push received");
            }
        }
    }
}