package com.iterable.iterableapi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import static org.robolectric.Shadows.shadowOf;

import org.json.JSONArray;
import org.json.JSONObject;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P})
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
    }

    @After
    public void tearDown() {
        mNotificationManager.cancelAll();
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

    @Test
    public void testMultipleNotification() throws Exception {
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

//        mNotificationManager.cancelAll();
        Bundle notif2 = new Bundle();
        notif2.putString(IterableConstants.ITERABLE_DATA_KEY, itbl2);

        IterableNotificationBuilder iterableNotification2 = postNotification(notif2);
        assertFalse(iterableNotification2.iterableNotificationData.getIsGhostPush());
        assertEquals("22222222222222222222222222222222", iterableNotification2.iterableNotificationData.getMessageId());
        assertEquals(Math.abs("22222222222222222222222222222222".hashCode()), iterableNotification2.requestCode);
        assertEquals(2, iterableNotification2.iterableNotificationData.getCampaignId());
        assertEquals(2, iterableNotification2.iterableNotificationData.getTemplateId());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertEquals(2, mNotificationManager.getActiveNotifications().length);
        }
    }

    @Test
    public void testActionButtonsAndRemoteInputs() throws Exception {
        // Read and parse the JSON file
        String jsonContent = getResourceString("push_payload_action_buttons.json");
        JSONObject jsonObject = new JSONObject(jsonContent);
        JSONArray actionButtons = jsonObject.getJSONArray("actionButtons");

        // Create and post the notification
        Bundle notif = new Bundle();
        notif.putString(IterableConstants.ITERABLE_DATA_KEY, jsonContent);

        IterableNotificationBuilder iterableNotification = postNotification(notif);
        StatusBarNotification statusBarNotification = mNotificationManager.getActiveNotifications()[0];
        Notification notification = statusBarNotification.getNotification();

        // Assert number of actions matches JSON
        assertEquals(actionButtons.length(), notification.actions.length);

        // Assert each action button matches JSON
        for (int i = 0; i < actionButtons.length(); i++) {
            JSONObject actionButton = actionButtons.getJSONObject(i);
            assertEquals(actionButton.getString("title"), notification.actions[i].title);

            // Check for remote input on text input type buttons
            if ("textInput".equals(actionButton.getString("buttonType"))) {
                assertTrue("Input action has a remote input", notification.actions[i].getRemoteInputs().length > 0);
                if (actionButton.has("inputPlaceholder")) {
                    assertEquals(actionButton.getString("inputPlaceholder"),
                        notification.actions[i].getRemoteInputs()[0].getLabel());
                }
            }
        }
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

    @Test
    public void testPendingIntentFlags() throws Exception {
        Bundle notif = new Bundle();
        notif.putString(IterableConstants.ITERABLE_DATA_KEY, getResourceString("push_payload_action_buttons.json"));

        IterableNotificationBuilder iterableNotification = postNotification(notif);
        StatusBarNotification statusBarNotification = mNotificationManager.getActiveNotifications()[0];
        Notification notification = statusBarNotification.getNotification();

        // Test contentIntent (default action)
        int contentIntentFlags = shadowOf(notification.contentIntent).getFlags();
        assertTrue((contentIntentFlags & PendingIntent.FLAG_UPDATE_CURRENT) != 0);
        assertTrue((contentIntentFlags & PendingIntent.FLAG_IMMUTABLE) != 0);  // Should be immutable for default action

        // Test deeplink button (default type, openApp=true)
        int deeplinkButtonFlags = shadowOf(notification.actions[0].actionIntent).getFlags();
        assertTrue((deeplinkButtonFlags & PendingIntent.FLAG_UPDATE_CURRENT) != 0);
        assertTrue((deeplinkButtonFlags & PendingIntent.FLAG_IMMUTABLE) != 0);  // Should be immutable for default type

        // Test silent action button (default type, openApp=false)
        int silentActionFlags = shadowOf(notification.actions[1].actionIntent).getFlags();
        assertTrue((silentActionFlags & PendingIntent.FLAG_UPDATE_CURRENT) != 0);
        assertTrue((silentActionFlags & PendingIntent.FLAG_IMMUTABLE) != 0);  // Should be immutable for default type

        // Test text input button
        int textInputFlags = shadowOf(notification.actions[2].actionIntent).getFlags();
        assertTrue((textInputFlags & PendingIntent.FLAG_UPDATE_CURRENT) != 0);
        assertTrue((textInputFlags & PendingIntent.FLAG_MUTABLE) != 0);  // Should be mutable for text input
    }
}