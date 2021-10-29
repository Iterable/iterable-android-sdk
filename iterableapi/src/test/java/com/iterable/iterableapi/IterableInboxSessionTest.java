package com.iterable.iterableapi;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IterableInboxSessionTest {
    @Mock
    IterableInAppManager mockInAppManager = mock(IterableInAppManager.class);

    @Before
    public void setUp() {
        IterableApi.sharedInstance = new IterableApi(mockInAppManager);

//        IterableApi.sharedInstance = new IterableApi();

//        IterableTestUtils.createIterableApi();
    }

    @Test
    public void testInitializer() {
        IterableInboxSession inboxSession = new IterableInboxSession();
        assertNull(inboxSession.sessionEndTime);
        assertNull(inboxSession.sessionStartTime);
        assertNull(inboxSession.impressions);
        assertEquals(0, inboxSession.endTotalMessageCount);
        assertEquals(0, inboxSession.endUnreadMessageCount);
        assertEquals(0, inboxSession.startTotalMessageCount);
        assertEquals(0, inboxSession.endTotalMessageCount);
    }

    @Ignore("incomplete test")
    @Test
    public void testUpdateVisibleRows() {
        IterableInAppMessage msg1 = InAppTestUtils.getTestInboxInAppWithId("1");
        IterableInAppMessage msg2 = InAppTestUtils.getTestInboxInAppWithId("2");

        // load messages into inAppManager


        InboxSessionManager sessionManager = new InboxSessionManager();

        // create new session here

        IterableInboxSession inboxSession = new IterableInboxSession();

        IterableInboxSession.Impression imp1 = new IterableInboxSession.Impression(msg1.getMessageId(), true, 1, 1.0f);
        IterableInboxSession.Impression imp2 = new IterableInboxSession.Impression(msg2.getMessageId(), true, 2, 2.0f);
        IterableInboxSession.Impression elapsedImp1 = new IterableInboxSession.Impression(msg1.getMessageId(), true, 1, 3.0f);

        sessionManager.updateVisibleRows(Arrays.asList(imp1));

        assertTrue(sessionManager.impressions.containsValue(imp1));
        assertFalse(sessionManager.impressions.containsValue(imp2));

        sessionManager.updateVisibleRows(Arrays.asList(imp1, imp2));

        assertTrue(sessionManager.impressions.containsValue(imp1));
        assertTrue(sessionManager.impressions.containsValue(imp2));

        sessionManager.updateVisibleRows(Arrays.asList(elapsedImp1, imp2));

        assertFalse(sessionManager.impressions.containsValue(imp1));
        assertTrue(sessionManager.impressions.containsValue(imp2));
        assertTrue(sessionManager.impressions.containsValue(elapsedImp1));

        //get and assert data integrity of impressions at each step?
    }
}