package com.iterable.iterableapi;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 *
 * Created by David Truong dt@iterable.com
 */
public class IterablePushReceiver extends BroadcastReceiver{

    static final String TAG = "IterablePushReceiver";

    private static final String ACTION_GCM_RECEIVE_INTENT = "com.google.android.c2dm.intent.RECEIVE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();

        if (intentAction.equals(IterableConstants.ACTION_PUSH_REGISTRATION)) {
           handlePushRegistration(context, intent);
        } else if (intentAction.equals(ACTION_GCM_RECEIVE_INTENT)) {
            handlePushReceived(context, intent);
        }
    }

    private void handlePushRegistration(Context context, Intent intent) {
        String iterableAppId = intent.getStringExtra(IterableConstants.PUSH_APPID);
        String projectNumber = intent.getStringExtra(IterableConstants.PUSH_PROJECT_NUMBER);
        boolean disablePush = intent.getBooleanExtra(IterableConstants.PUSH_DISABLE_AFTER_REGISTRATION, false);
        IterableGCMRegistrationData data = new IterableGCMRegistrationData(iterableAppId, projectNumber, disablePush);
        new IterablePushRegistrationGCM().execute(data);
    }

    private void handlePushReceived(Context context, Intent intent) {
        if (intent.hasExtra(IterableConstants.ITERABLE_DATA_KEY)) {
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

            int resourceIconId = appContext.getResources().getIdentifier(
                    IterableApi.getNotificationIcon(context),
                    IterableConstants.ICON_FOLDER_IDENTIFIER,
                    appContext.getPackageName());

            int iconId = 0;
            if (resourceIconId != 0) {
                iconId = resourceIconId;
            } else if (appContext.getApplicationInfo().icon != 0) {
                IterableLogger.d(TAG, "No Notification Icon defined - defaulting to app icon");
                iconId = appContext.getApplicationInfo().icon;
            } else {
                IterableLogger.w(TAG, "No Notification Icon defined - push notifications will not be displayed");
            }

            IterableNotification notificationBuilder = IterableNotification.createNotification(
                    appContext, intent.getExtras(), mainClass, iconId);

            IterableNotification.postNotificationOnDevice(appContext, notificationBuilder);
        }
    }
}