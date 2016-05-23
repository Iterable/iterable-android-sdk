package com.iterable.iterableapi;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;

/**
 *
 * Created by davidtruong on 4/29/16.
 */
public class IterableNotification extends NotificationCompat.Builder {

    //TODO: update notificationID to use a bitwise timestamp so it doesn't overwrite old notifications
    public static final int NOTIFICATION_ID = 1;

    private boolean isGhostPush;

    protected IterableNotification(Context context) {
        super(context);
    }

        /**
         * Creates and returns an instance of IterableNotification.
         * @param context
         * @param intent
         * @param classToOpen
         * @param icon
         * @return Returns null if the intent comes from an Iterable ghostPush
         */
    public static IterableNotification createNotification(Context context, Intent intent, Class classToOpen, int icon) {
        Bundle extras = intent.getExtras();

        int stringId = context.getApplicationInfo().labelRes;
        String applicationName  = context.getString(stringId);
        String notificationBody = null;
        if (intent.hasExtra(IterableConstants.ITERABLE_DATA_KEY)) {
            if (extras.containsKey(IterableConstants.ITERABLE_DATA_BODY)) {
                notificationBody = extras.getString(IterableConstants.ITERABLE_DATA_BODY);
            }
            if (extras.containsKey(IterableConstants.ITERABLE_DATA_TITLE)) {
                applicationName = extras.getString(IterableConstants.ITERABLE_DATA_TITLE);
            }
        }

        Intent mainIntentWithExtras = new Intent(IterableConstants.ACTION_NOTIF_OPENED);
        mainIntentWithExtras.setClass(context, classToOpen);
        mainIntentWithExtras.putExtras(extras);
        mainIntentWithExtras.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent notificationClickedIntent = PendingIntent.getActivity(context, 0,
                mainIntentWithExtras, PendingIntent.FLAG_UPDATE_CURRENT);



        IterableNotification notificationBuilder = new IterableNotification(context);
        notificationBuilder.setSmallIcon(icon)
                .setContentTitle(applicationName)
                .setContentText(notificationBody)
                .setAutoCancel(true);

        notificationBuilder.setContentIntent(notificationClickedIntent);

        notificationBuilder.isGhostPush = IterableHelper.isGhostPush(intent);

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
        if ( !iterableNotification.isGhostPush) {
            NotificationManager mNotificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);

            //TODO: enable collapsing of notification via unique notification_id
            mNotificationManager.notify(NOTIFICATION_ID, iterableNotification.build());
        }
    }
}
