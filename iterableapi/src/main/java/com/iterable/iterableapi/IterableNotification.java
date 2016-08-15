package com.iterable.iterableapi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;

import java.util.Date;

/**
 *
 * Created by David Truong dt@iterable.com
 */
public class IterableNotification extends NotificationCompat.Builder {
    static final String TAG = "IterableNotification";
    private boolean isGhostPush;

    protected IterableNotification(Context context) {
        super(context);
    }

        /**
         * Creates and returns an instance of IterableNotification.
         * @param context
         * @param extras
         * @param classToOpen
         * @return Returns null if the intent comes from an Iterable ghostPush
         */
    public static IterableNotification createNotification(Context context, Bundle extras, Class classToOpen) {
        int stringId = context.getApplicationInfo().labelRes;
        String applicationName  = context.getString(stringId);
        String notificationBody = null;
        String soundName = null;

        if (extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)) {
            notificationBody = extras.getString(IterableConstants.ITERABLE_DATA_BODY, notificationBody);
            applicationName = extras.getString(IterableConstants.ITERABLE_DATA_TITLE, applicationName);
            soundName = extras.getString(IterableConstants.ITERABLE_DATA_SOUND, soundName);
        }

        Intent mainIntentWithExtras = new Intent(IterableConstants.ACTION_NOTIF_OPENED);
        mainIntentWithExtras.setClass(context, classToOpen);
        mainIntentWithExtras.putExtras(extras);
        mainIntentWithExtras.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent notificationClickedIntent = PendingIntent.getActivity(context, 0,
                mainIntentWithExtras, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notifPermissions = new Notification();
        notifPermissions.defaults |= Notification.DEFAULT_LIGHTS;

        IterableNotification notificationBuilder = new IterableNotification(context);
        notificationBuilder
            .setSmallIcon(getIconId(context))
            .setTicker(applicationName).setWhen(0)
            .setAutoCancel(true)
            .setContentTitle(applicationName)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationBody))
            .setPriority(Notification.PRIORITY_HIGH)
            .setContentText(notificationBody);

        if (soundName != null) {
            int soundID = context.getResources().getIdentifier(soundName, "raw", context.getPackageName());
            Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + soundID);
            notificationBuilder.setSound(soundUri);
        } else {
            notifPermissions.defaults |= Notification.DEFAULT_SOUND;
        }

        notificationBuilder.setContentIntent(notificationClickedIntent);
        notificationBuilder.isGhostPush = IterableHelper.isGhostPush(extras);

        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            notificationBuilder.setColor(info.metaData.getInt(IterableConstants.NOTIFICATION_COLOR));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        PackageManager pm = context.getPackageManager();
        if (pm.checkPermission(android.Manifest.permission.VIBRATE, context.getPackageName()) == PackageManager.PERMISSION_GRANTED) {
            notifPermissions.defaults |= Notification.DEFAULT_VIBRATE;
        }


        notificationBuilder.setDefaults(notifPermissions.defaults);

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

    /**
     * Returns the iconId from potential resource locations
     * @param context
     * @return
     */
    private static int getIconId(Context context) {
        int iconId = 0;

        //Get the iconId set in the AndroidManifest.xml
        if (iconId == 0) {
            try {
                ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                iconId = info.metaData.getInt(IterableConstants.NOTIFICATION_ICON_NAME, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        //Get the iconId set in code
        if (iconId == 0) {
            iconId = context.getResources().getIdentifier(
                IterableApi.getNotificationIcon(context),
                IterableConstants.ICON_FOLDER_IDENTIFIER,
                context.getPackageName());
        }

        //Get id from the default app settings
        if (iconId == 0) {
            if (context.getApplicationInfo().icon != 0) {
                IterableLogger.d(TAG, "No Notification Icon defined - defaulting to app icon");
                iconId = context.getApplicationInfo().icon;
            }
            else {
                IterableLogger.w(TAG, "No Notification Icon defined - push notifications will not be displayed");
            }
        }

        return iconId;
    }
}
