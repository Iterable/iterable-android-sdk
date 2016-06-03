package com.iterable.iterableapi;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;

import java.util.Date;

/**
 *
 * Created by David Truong dt@iterable.com
 */
public class IterableNotification extends NotificationCompat.Builder {
    private boolean isGhostPush;

    protected IterableNotification(Context context) {
        super(context);
    }

        /**
         * Creates and returns an instance of IterableNotification.
         * @param context
         * @param extras
         * @param classToOpen
         * @param icon
         * @return Returns null if the intent comes from an Iterable ghostPush
         */
    public static IterableNotification createNotification(Context context, Bundle extras, Class classToOpen, int icon) {
        int stringId = context.getApplicationInfo().labelRes;
        String applicationName  = context.getString(stringId);
        String notificationBody = null;
        if (extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)) {
            notificationBody = extras.getString(IterableConstants.ITERABLE_DATA_BODY, notificationBody);
            applicationName = extras.getString(IterableConstants.ITERABLE_DATA_TITLE, applicationName);
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

        notificationBuilder.isGhostPush = IterableHelper.isGhostPush(extras);

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

            long dateInMilli = new Date().getTime();
            int notifID = (int) (dateInMilli % Integer.MAX_VALUE);

            mNotificationManager.notify(notifID, iterableNotification.build());
        }
    }
}
