package com.iterable.iterableapi;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class IterableActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();

        //TOOD: might not need the extra requestCode since the messageId is always in the original payload.
        int requestCode = extras.getInt(IterableConstants.REQUEST_CODE, 0);
        System.out.print("requestCode: "+requestCode);
        NotificationManager mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(requestCode);

        Class mainClass = null;
        try {
            mainClass = Class.forName(extras.getString(IterableConstants.MAIN_CLASS, ""));
        } catch (ClassNotFoundException e) {
            IterableLogger.e("IterableActionReceiver", "Invalid main class", e);
            return;
        }

        //get the notification action here
        String actionName = intent.getAction();

        if(IterableConstants.ACTION_NOTIF_OPENED.equalsIgnoreCase(actionName)) {
            //Handles opens and deeplinks
            Intent mainIntent = new Intent(context, mainClass);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            // If android:launchMode="singleInstance" is specified in the manifest, the activity
            // won't be relaunched, instead it will receive onNewIntent
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mainIntent.putExtras(extras);
            context.startActivity(mainIntent);
        }

        //TODO: if custom event - open up main class with custom event as the action name.

        //Check if the action should not open the application
        if (false) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
            context.startActivity(browserIntent);
        } else {
            //Handles opens and deeplinks
            Intent mainIntent = new Intent(context, mainClass);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mainIntent.putExtras(extras);
            context.startActivity(mainIntent);
        }
    }
}
