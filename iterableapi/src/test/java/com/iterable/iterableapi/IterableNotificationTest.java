package com.iterable.iterableapi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowPendingIntent;

import static com.iterable.iterableapi.IterableTestUtils.getResourceString;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class IterableNotificationTest {

    private NotificationManager mNotificationManager;

    protected Context getContext() {
        return ApplicationProvider.getApplicationContext();
    }

    private IterableNotificationBuilder postNotification(Bundle notificationData) throws InterruptedException {
        getContext().getApplicationInfo().icon = android.R.drawable.sym_def_app_icon;
        IterableNotificationBuilder iterableNotification = IterableNotificationHelper.createNotification(getContext(), notificationData);
        IterableNotificationHelper.postNotificationOnDevice(getContext(), iterableNotification);
        return iterableNotification;
    }

    @Before
    public void setUp() throws Exception {
        mNotificationManager = (NotificationManager)
                getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
    }

    @After
    public void tearDown() {
        mNotificationManager.cancelAll();
    }

    @Test
    public void testActionButtons() throws Exception {
        Bundle notif = new Bundle();
        notif.putString(IterableConstants.ITERABLE_DATA_KEY, getResourceString("push_payload_action_buttons.json"));

        IterableNotificationBuilder iterableNotification = postNotification(notif);
        StatusBarNotification statusBarNotification = mNotificationManager.getActiveNotifications()[0];
        Notification notification = statusBarNotification.getNotification();
        assertEquals(3, notification.actions.length);

        // Match JSON titles and identifiers to the actual notification
        JSONObject itbl = new JSONObject(notif.getString(IterableConstants.ITERABLE_DATA_KEY));
        JSONArray actionButtonsJson = itbl.getJSONArray("actionButtons");
        int i = 0;
        for (Notification.Action action : notification.actions) {
            JSONObject buttonJson = actionButtonsJson.getJSONObject(i++);
            assertEquals(buttonJson.get("title"), action.title);
            assertEquals(buttonJson.get("identifier"), shadowOf(action.actionIntent)
                    .getSavedIntent().getStringExtra(IterableConstants.ACTION_IDENTIFIER));
        }
        assertTrue("Input action has a remote input", notification.actions[2].getRemoteInputs().length > 0);
    }

    @Test
    public void testNoAction() throws Exception {
        Bundle notif = new Bundle();
        notif.putString(IterableConstants.ITERABLE_DATA_KEY, getResourceString("push_payload_no_action.json"));

        IterableNotificationBuilder iterableNotification = postNotification(notif);
        StatusBarNotification statusBarNotification = mNotificationManager.getActiveNotifications()[0];
        Notification notification = statusBarNotification.getNotification();
        assertEquals(1, notification.actions.length);
        assertEquals("No action", notification.actions[0].title);

        ShadowPendingIntent shadowPendingIntent = shadowOf(notification.actions[0].actionIntent);
        Intent savedIntent = shadowPendingIntent.getSavedIntent();
        assertEquals(IterableConstants.ACTION_PUSH_ACTION, savedIntent.getAction());
        assertEquals("button1", savedIntent.getStringExtra(IterableConstants.ACTION_IDENTIFIER));
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
