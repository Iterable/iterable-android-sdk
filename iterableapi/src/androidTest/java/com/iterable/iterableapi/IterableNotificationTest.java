package com.iterable.iterableapi;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class IterableNotificationTest {
    Context appContext;
    NotificationManager mNotificationManager;

    //EX: itbl_notif = "{\"itbl\":{\"templateId\":28,\"campaignId\":17,\"messageId\":\"85fbb5663ada4f40b5ae0d437e040fa6\",\"isGhostPush\":false},\"body\":\"Pushy\",\"badge\":2,\"sound\":\"me.wav\"}}";

    String body = "my first push message";
    String sound = "me.wav";
    String itbl_ghost = "{\"templateId\":1,\"campaignId\":1,\"messageId\":\"11111111111111111111111111111111\",\"isGhostPush\":true}";
    String itbl1 = "{\"templateId\":1,\"campaignId\":1,\"messageId\":\"11111111111111111111111111111111\",\"isGhostPush\":false}";
    String itbl2 = "{\"templateId\":2,\"campaignId\":2,\"messageId\":\"22222222222222222222222222222222\",\"isGhostPush\":false}}";
    String itbl_image = "{\"templateId\":1,\"campaignId\":1,\"messageId\":\"11111111111111111111111111111111\",\"isGhostPush\":false,\"attachment-url\":\"https://assets.iterable.com/assets/images/logos/itbl-logo-full-gray-800x300.png\"}";

    private Context getContext() {
        return getApplicationContext();
    }

    private IterableNotificationBuilder postNotification(Bundle notificationData) throws InterruptedException {
        getContext().getApplicationInfo().icon = android.R.drawable.sym_def_app_icon;
        IterableNotificationBuilder iterableNotification = IterableNotificationHelper.createNotification(getContext(), notificationData);
        IterableNotificationHelper.postNotificationOnDevice(appContext, iterableNotification);
//        It looks like mNotificationManager.notify(iterableNotification.requestCode, iterableNotification.build());
//        is the culprit here for the flaky tests. This thread is spun up by the android system. Unless we do dependency injection and mock the notificationManager, it'll be hard to make this unflake.
        Thread.sleep(1000);
        return iterableNotification;
    }

    private String getResourceString(String fileName) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String receiveString = "";
        StringBuilder stringBuilder = new StringBuilder();

        while ((receiveString = bufferedReader.readLine()) != null) {
            stringBuilder.append(receiveString);
        }

        inputStream.close();
        return stringBuilder.toString();
    }

    @Before
    public void setUp() throws Exception {
        appContext = getContext().getApplicationContext();
        mNotificationManager = (NotificationManager)
                getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
        Intents.init();
    }

    @After
    public void tearDown() {
        mNotificationManager.cancelAll();
        Intents.release();
    }

    @Test
    public void testEmptyBundle() throws Exception {
        IterableNotificationBuilder iterableNotification = IterableNotificationHelper.createNotification(getContext(), new Bundle());
        assertNull(iterableNotification);
    }

    @Test
    public void testGhostPush() throws Exception {
        Bundle notif1 = new Bundle();
        notif1.putString(IterableConstants.ITERABLE_DATA_KEY, itbl_ghost);
        IterableNotificationBuilder iterableNotification = IterableNotificationHelper.createNotification(getContext(), notif1);
        assertNull(iterableNotification);
    }

    /**
     * Tests loading a notification with an image.
     * @throws Exception
     */
    @Test
    @Ignore("notification.extras.containsKey(Notification.EXTRA_PICTURE_ICON) was passed as null by SDK when creating the notification. Hence removed the line.")
    public void testNotificationImage() throws Exception {
        Bundle notif = new Bundle();
        notif.putString(IterableConstants.ITERABLE_DATA_KEY, itbl_image);
        notif.putString(IterableConstants.ITERABLE_DATA_BODY, body);

        IterableNotificationBuilder iterableNotification = postNotification(notif);
        assertEquals("IterableAPI", iterableNotification.build().extras.getString(Notification.EXTRA_TITLE));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            assertEquals(1, mNotificationManager.getActiveNotifications().length);
            Notification notification = mNotificationManager.getActiveNotifications()[0].getNotification();
            assertTrue(notification.extras.containsKey(Notification.EXTRA_PICTURE));
        }
    }

    @Test
    public void testNotificationText() throws Exception {
        Bundle notif = new Bundle();
        notif.putString(IterableConstants.ITERABLE_DATA_KEY, itbl2);
        notif.putString(IterableConstants.ITERABLE_DATA_BODY, body);
        notif.putString(IterableConstants.ITERABLE_DATA_SOUND, sound);

        IterableNotificationBuilder iterableNotification = postNotification(notif);
        assertEquals("IterableAPI", iterableNotification.build().extras.getString(Notification.EXTRA_TITLE));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Bundle notificationExtras = mNotificationManager.getActiveNotifications()[0].getNotification().extras;
            assertEquals("IterableAPI", notificationExtras.get("android.title"));
            assertEquals(body, notificationExtras.get("android.text"));
        }
    }

     @Ignore ("Posting two notifications in a row is not giving back 3 in length. Nor does it increase in linear format where 3." +
             "For .eg, posting 2 notification gives back 3 in notification array. Posting 3 gives back 4. Posting just one gives back one. " +
             "Cancelling all notification before posting the second one and then checking gives back 1 which seems correct. Its the multiple notification thats needs an eye.")
     @Test
    public void testMessage() throws Exception {
        Bundle notif1 = new Bundle();
        notif1.putString(IterableConstants.ITERABLE_DATA_KEY, itbl1);

        IterableNotificationBuilder iterableNotification = postNotification(notif1);
        assertFalse(iterableNotification.iterableNotificationData.getIsGhostPush());
        assertEquals("11111111111111111111111111111111", iterableNotification.iterableNotificationData.getMessageId());
        assertEquals(Math.abs("11111111111111111111111111111111".hashCode()), iterableNotification.requestCode);
        assertEquals(1, iterableNotification.iterableNotificationData.getCampaignId());
        assertEquals(1, iterableNotification.iterableNotificationData.getTemplateId());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertEquals(1, mNotificationManager.getActiveNotifications().length);
        }

        mNotificationManager.cancelAll();
        Bundle notif2 = new Bundle();
        notif2.putString(IterableConstants.ITERABLE_DATA_KEY, itbl2);

        IterableNotificationBuilder iterableNotification2 = postNotification(notif2);
        assertFalse(iterableNotification2.iterableNotificationData.getIsGhostPush());
        assertEquals("22222222222222222222222222222222", iterableNotification2.iterableNotificationData.getMessageId());
        assertEquals(Math.abs("22222222222222222222222222222222".hashCode()), iterableNotification2.requestCode);
        assertEquals(2, iterableNotification2.iterableNotificationData.getCampaignId());
        assertEquals(2, iterableNotification2.iterableNotificationData.getTemplateId());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertEquals(1, mNotificationManager.getActiveNotifications().length);
        }
    }

    @Test
    public void testActionButtons() throws Exception {
        Bundle notif = new Bundle();
        notif.putString(IterableConstants.ITERABLE_DATA_KEY, getResourceString("push_payload_action_buttons.json"));

        IterableNotificationBuilder iterableNotification = postNotification(notif);
        StatusBarNotification statusBarNotification = mNotificationManager.getActiveNotifications()[0];
        Notification notification = statusBarNotification.getNotification();
        assertEquals(3, notification.actions.length);
        assertEquals("Open Deeplink", notification.actions[0].title);
        assertEquals("Silent Action", notification.actions[1].title);
        assertEquals("Text input", notification.actions[2].title);
    }

    @Test
    public void testNotificationTimeStamp() throws Exception {
        Bundle notificationBundle = new Bundle();
        notificationBundle.putString(IterableConstants.ITERABLE_DATA_KEY, itbl2);
        notificationBundle.putString(IterableConstants.ITERABLE_DATA_BODY, body);
        IterableNotificationBuilder iterableNotification = postNotification(notificationBundle);
        StatusBarNotification statusBarNotification = mNotificationManager.getActiveNotifications()[0];
        Notification notification = statusBarNotification.getNotification();
        //Checking if the notification time is close to system time when received. 5000ms is to compensate with delay that might occur during creation, posting and checking the notification.
        assertTrue(System.currentTimeMillis() - notification.when < 5000);
    }
}