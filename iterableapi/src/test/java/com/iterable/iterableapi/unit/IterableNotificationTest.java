package com.iterable.iterableapi.unit;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import com.iterable.iterableapi.IterableConstants;
import com.iterable.iterableapi.IterableNotificationBuilder;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowPendingIntent;

import static com.iterable.iterableapi.unit.IterableTestUtils.getResourceString;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.allOf;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class IterableNotificationTest {

    private NotificationManager mNotificationManager;

    protected Context getContext() {
        return RuntimeEnvironment.application;
    }

    private IterableNotificationBuilder postNotification(Bundle notificationData) throws InterruptedException {
        getContext().getApplicationInfo().icon = android.R.drawable.sym_def_app_icon;
        IterableNotificationBuilder iterableNotification = IterableNotificationBuilder.createNotification(getContext(), notificationData, Application.class);
        IterableNotificationBuilder.postNotificationOnDevice(getContext(), iterableNotification);
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

}
