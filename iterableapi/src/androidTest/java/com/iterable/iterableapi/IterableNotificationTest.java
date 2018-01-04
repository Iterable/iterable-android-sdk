package com.iterable.iterableapi;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.test.ApplicationTestCase;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class IterableNotificationTest extends ApplicationTestCase<Application> {
    public IterableNotificationTest() {
        super(Application.class);
    }

    Context appContext;
    NotificationManager mNotificationManager;

    //EX: itbl_notif = "{\"itbl\":{\"templateId\":28,\"campaignId\":17,\"messageId\":\"85fbb5663ada4f40b5ae0d437e040fa6\",\"isGhostPush\":false},\"body\":\"Pushy\",\"badge\":2,\"sound\":\"me.wav\"}}";

    String body = "my first push message";
    String sound = "me.wav";
    String itbl_ghost = "{\"templateId\":1,\"campaignId\":1,\"messageId\":\"11111111111111111111111111111111\",\"isGhostPush\":true}";
    String itbl1 = "{\"templateId\":1,\"campaignId\":1,\"messageId\":\"11111111111111111111111111111111\",\"isGhostPush\":false}";
    String itbl2 = "{\"templateId\":2,\"campaignId\":2,\"messageId\":\"22222222222222222222222222222222\",\"isGhostPush\":false}}";

    public void setUp() throws Exception {
        super.setUp();

        appContext = getContext().getApplicationContext();
        mNotificationManager = (NotificationManager)
                getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
    }

    public void testEmptyBundle() throws Exception {
        IterableNotification iterableNotification = IterableNotification.createNotification(getContext(), new Bundle(), Application.class);
        assertTrue(iterableNotification.requestCode < System.currentTimeMillis());
    }

    public void testGhostPush() throws Exception {
        Bundle notif1 = new Bundle();
        notif1.putString(IterableConstants.ITERABLE_DATA_KEY, itbl_ghost);
        IterableNotification iterableNotification = IterableNotification.createNotification(getContext(), notif1, Application.class);
        IterableNotification.postNotificationOnDevice(appContext, iterableNotification);
        assertTrue(iterableNotification.iterableNotificationData.getIsGhostPush());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertEquals(0, mNotificationManager.getActiveNotifications().length);
        }
    }


    public void testNotificationText() throws Exception {
        Bundle notif = new Bundle();
        notif.putString(IterableConstants.ITERABLE_DATA_KEY, itbl2);
        notif.putString(IterableConstants.ITERABLE_DATA_BODY, body);
        notif.putString(IterableConstants.ITERABLE_DATA_SOUND, sound);

        getContext().getApplicationInfo().icon = android.R.drawable.sym_def_app_icon;

        IterableNotification iterableNotification = IterableNotification.createNotification(getContext(), notif, Application.class);
        IterableNotification.postNotificationOnDevice(appContext, iterableNotification);
        assertEquals("IterableAPI", iterableNotification.mContentTitle);
        Thread.sleep(100);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Bundle notificationExtras = mNotificationManager.getActiveNotifications()[0].getNotification().extras;
            assertEquals("IterableAPI", notificationExtras.get("android.title"));
            assertEquals(body, notificationExtras.get("android.text"));
        }
    }

    public void testMessage() throws Exception {
        Bundle notif1 = new Bundle();
        notif1.putString(IterableConstants.ITERABLE_DATA_KEY, itbl1);
        getContext().getApplicationInfo().icon = android.R.drawable.sym_def_app_icon;

        IterableNotification iterableNotification = IterableNotification.createNotification(getContext(), notif1, Application.class);
        IterableNotification.postNotificationOnDevice(appContext, iterableNotification);
        Thread.sleep(100);
        assertFalse(iterableNotification.iterableNotificationData.getIsGhostPush());
        assertEquals("11111111111111111111111111111111", iterableNotification.iterableNotificationData.getMessageId());
        assertEquals("11111111111111111111111111111111".hashCode(), iterableNotification.requestCode);
        assertEquals(1, iterableNotification.iterableNotificationData.getCampaignId());
        assertEquals(1, iterableNotification.iterableNotificationData.getTemplateId());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertEquals(1, mNotificationManager.getActiveNotifications().length);
        }

        Bundle notif2 = new Bundle();
        notif2.putString(IterableConstants.ITERABLE_DATA_KEY, itbl2);
        getContext().getApplicationInfo().icon = android.R.drawable.sym_def_app_icon;

        IterableNotification iterableNotification2 = IterableNotification.createNotification(getContext(), notif2, Application.class);
        IterableNotification.postNotificationOnDevice(appContext, iterableNotification2);
        Thread.sleep(100);
        assertFalse(iterableNotification2.iterableNotificationData.getIsGhostPush());
        assertEquals("22222222222222222222222222222222", iterableNotification2.iterableNotificationData.getMessageId());
        assertEquals("22222222222222222222222222222222".hashCode(), iterableNotification2.requestCode);
        assertEquals(2, iterableNotification2.iterableNotificationData.getCampaignId());
        assertEquals(2, iterableNotification2.iterableNotificationData.getTemplateId());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertEquals(2, mNotificationManager.getActiveNotifications().length);
        }

    }
}