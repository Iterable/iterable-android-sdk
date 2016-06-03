package com.iterable.iterableapi;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 *
 * Created by davidtruong on 4/19/16.
 */
public class IterablePushReceiver extends BroadcastReceiver{

    static final String TAG = "IterablePushReceiver";

    private static final String ACTION_GCM_RECEIVE_INTENT = "com.google.android.c2dm.intent.RECEIVE";
    private static final String ACTION_GCM_REGISTRATION_INTENT = "com.google.android.c2dm.intent.REGISTRATION";

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();

        if (intentAction.equals(IterableConstants.ACTION_PUSH_REGISTRATION)) {
           handlePushRegistration(context, intent);
        } else if (intentAction.equals(ACTION_GCM_RECEIVE_INTENT)) {
            handlePushReceived(context, intent);
        } else if (intentAction.equals(ACTION_GCM_REGISTRATION_INTENT)) {
            Log.d(TAG, "received GCM registration intent action");
        }
    }

    private void handlePushRegistration(Context context, Intent intent) {
        String iterableAppId = intent.getStringExtra(IterableConstants.PUSH_APPID);
        String projectNumber = intent.getStringExtra(IterableConstants.PUSH_PROJECTID);
        IterableGCMRegistrationData data = new IterableGCMRegistrationData(iterableAppId, projectNumber);
        new IterablePushRegistrationGCM().execute(data);
    }

    private void handlePushReceived(Context context, Intent intent) {
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

        int iconId = appContext.getResources().getIdentifier(
                IterableApi.getNotificationIcon(context),
                "drawable",
                appContext.getPackageName());

        if (iconId == 0) {
            iconId = appContext.getApplicationInfo().icon;
            if (iconId != 0){
                Log.d(TAG, "No Notification Icon defined - defaulting to app icon");
            } else {
                Log.w(TAG, "No Notification Icon defined - push notifications will not be displayed");
            }
        }

        IterableNotification notificationBuilder = IterableNotification.createNotification(
                appContext, intent.getExtras(), mainClass, iconId);

        IterableNotification.postNotificationOnDevice(appContext, notificationBuilder);
    }
}