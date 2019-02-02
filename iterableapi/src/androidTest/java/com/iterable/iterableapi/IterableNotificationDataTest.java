package com.iterable.iterableapi;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class IterableNotificationDataTest {

    @Test
    public void testPayloadString() throws Exception {
        String userInfo = "{\"campaignId\": 1,\n" +
                "\"templateId\": 2," +
                "\"isGhostPush\": true," +
                "\"messageId\": \"abc123\"}";
        IterableNotificationData iterableNotificationData = new IterableNotificationData(userInfo);
        assertEquals(1, iterableNotificationData.getCampaignId());
        assertEquals(2, iterableNotificationData.getTemplateId());
        assertEquals("abc123", iterableNotificationData.getMessageId());
        assertTrue(iterableNotificationData.getIsGhostPush());
    }
}