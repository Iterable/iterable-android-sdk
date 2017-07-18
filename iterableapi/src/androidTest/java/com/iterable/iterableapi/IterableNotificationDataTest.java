package com.iterable.iterableapi;

import android.app.Application;
import android.test.ApplicationTestCase;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class IterableNotificationDataTest extends ApplicationTestCase<Application> {
    public IterableNotificationDataTest() {
        super(Application.class);
    }

    public void testPayloadString() throws Exception {
        String userInfo = "{\"campaignId\": 1,\n" +
                "\"templateId\": 2," +
                "\"isGhostPush\": true," +
                "\"messageId\": \"abc123\"}";
        IterableNotificationData iterableNotificationData = new IterableNotificationData(userInfo);
        assertEquals(1, iterableNotificationData.getCampaignId());
        assertEquals(2, iterableNotificationData.getTemplateId());
        assertEquals("abc123", iterableNotificationData.getMessageId());
        assertEquals(true, iterableNotificationData.getIsGhostPush());
    }
}