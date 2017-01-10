package com.iterable.iterableapi;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.test.ApplicationTestCase;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class IterableNotificationTest extends ApplicationTestCase<Application> {
    public IterableNotificationTest() {
        super(Application.class);
    }
    
    public void testEmptyBundle() throws Exception {
        IterableNotification iterableNotification = IterableNotification.createNotification(getContext(), new Bundle(), Application.class);
        assertTrue(iterableNotification.requestCode < System.currentTimeMillis());
    }

    public void testGhostPushTrue() throws Exception {

        String itbl = "{\"templateId\":25,\"campaignId\":15,\"messageId\":\"6780f5b5cd394b80ba944b5c08d7f9a2\",\"isGhostPush\":true}";

        Bundle notif1 = new Bundle();
        notif1.putString(IterableConstants.ITERABLE_DATA_KEY, itbl);

        IterableNotification iterableNotification = IterableNotification.createNotification(getContext(), notif1, Application.class);
        assertTrue(iterableNotification.iterableNotificationData.getIsGhostPush());
    }

    public void testGhostPushFalse() throws Exception {

        String itbl = "{\"templateId\":25,\"campaignId\":15,\"messageId\":\"6780f5b5cd394b80ba944b5c08d7f9a2\",\"isGhostPush\":false}";

        Bundle notif1 = new Bundle();
        notif1.putString(IterableConstants.ITERABLE_DATA_KEY, itbl);

        IterableNotification iterableNotification = IterableNotification.createNotification(getContext(), notif1, Application.class);
        assertFalse(iterableNotification.iterableNotificationData.getIsGhostPush());
    }

    public void testMessage() throws Exception {
        String itbl = "{\"templateId\":25,\"campaignId\":15,\"messageId\":\"6780f5b5cd394b80ba944b5c08d7f9a2\",\"isGhostPush\":false}";

        Bundle notif1 = new Bundle();
        notif1.putString(IterableConstants.ITERABLE_DATA_KEY, itbl);

        IterableNotification iterableNotification = IterableNotification.createNotification(getContext(), notif1, Application.class);
        assertEquals("6780f5b5cd394b80ba944b5c08d7f9a2", iterableNotification.iterableNotificationData.getMessageId());
        assertEquals("6780f5b5cd394b80ba944b5c08d7f9a2".hashCode(), iterableNotification.requestCode);
    }
}