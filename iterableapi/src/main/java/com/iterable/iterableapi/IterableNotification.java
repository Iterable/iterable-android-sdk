package com.iterable.iterableapi;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;

/**
 * An extension of {@link android.support.v7.app.NotificationCompat.Builder}
 * In addition to the builder in v7, this builder also includes a factory for creating
 * IterableNotification objects.
 *
 * IterableNotification takes care of ghostPushes which are sent to determine whether an
 * application is currently installed or uninstalled.
 *
 * Created by davidtruong on 4/29/16.
 */
public class IterableNotification extends NotificationCompat.Builder {

    //TODO: update notificationID to use a bitwise timestamp so it doesn't overwrite old notifications
    public static final int NOTIFICATION_ID = 1;

    protected IterableNotification(Context context) {
        super(context);
    }

    public static IterableNotification createIterableNotification(Context context, Intent intent, Class classToOpen, int icon) {
        return createIterableNotification(context, intent, classToOpen, icon, null);
    }

    /**
     * Creates and returns an instance of IterableNotification.
     * If the notification is a ghostPush, then the function returns null.
     * @param context
     * @param intent
     * @param classToOpen
     * @param icon
     * @param defaultMessageBody
     * @return Returns null if the intent comes from an Iterable ghostPush
     */
    public static IterableNotification createIterableNotification(Context context, Intent intent, Class classToOpen, int icon, String defaultMessageBody) {

        if (IterableHelper.isGhostPush(intent)) {
            return null;
        }

        Bundle extras = intent.getExtras();

        Intent mainIntentWithExtras = new Intent(IterableConstants.NOTIF_OPENED);
        mainIntentWithExtras.setClass(context, classToOpen);
        mainIntentWithExtras.putExtras(extras);

        PendingIntent notificationClickedIntent = PendingIntent.getActivity(context, 0,
                mainIntentWithExtras, 0);

        String notificationBody = (defaultMessageBody == null) ? defaultMessageBody : "";

        /**
         * If it is a notification sent from Iterable, set the notification body to use that data.
         */
        if (intent.hasExtra("itbl")) {
            notificationBody = extras.getString("body");
        }
        //TODO: should we be checking for other default values for the body text?
//        else if (intent.hasExtra("default")) {
//            notificationBody = extras.getString("default");
//        }
//        else if (intent.hasExtra("message")) {
//            notificationBody = extras.getString("message");
//        }

        int stringId = context.getApplicationInfo().labelRes;
        String applicationName  = context.getString(stringId);

        IterableNotification notificationBuilder = new IterableNotification(context);
        notificationBuilder.setSmallIcon(icon)
                .setContentTitle(applicationName)
                .setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle()
                .bigText(notificationBody))
                .setContentText(notificationBody);

        notificationBuilder.setContentIntent(notificationClickedIntent);

        return notificationBuilder;
    }

    /**
     * Posts the notification on device.
     * Only sets the notification if it is not a ghostPush/null iterableNotification.
     * @param context
     * @param iterableNotification Function assumes that the iterableNotification is a ghostPush
     *                             if the IterableNotification passed in is null.
     */
    public static void postNotificationOnDevice(Context context, IterableNotification iterableNotification) {
        if (iterableNotification != null) {

            NotificationManager mNotificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);

            //TODO: enable collapsing of notification via unique notification_id
            mNotificationManager.notify(NOTIFICATION_ID, iterableNotification.build());
        }
    }
}
