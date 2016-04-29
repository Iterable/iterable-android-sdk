package com.iterable.iterableapi;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
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

    protected IterableNotification(Context context) {
        super(context);
    }

    public static IterableNotification createIterableNotification(Context context, Intent intent, Class classToOpen, int icon) {
        return createIterableNotification(context, intent, classToOpen, icon, null);
    }

    /**
     * Creates and returns a IterableNotification.
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
         * If it is an Iterable sent notification set the notification body to use that data.
         */
        if (intent.hasExtra("itbl")) {
            notificationBody = extras.getString("body");
        }

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
}
