package com.iterable.iterableapi;

import org.junit.Test;

import static org.junit.Assert.*;

public class IterableInboxSessionTest {

    @Test
    public void testInitializer() {
        IterableInboxSession inboxSession = new IterableInboxSession();
        assertNull(inboxSession.sessionEndTime);
        assertNull(inboxSession.sessionStartTime);
        assertNull(inboxSession.impressions);
        assertEquals(0,inboxSession.endTotalMessageCount);
        assertEquals(0,inboxSession.endUnreadMessageCount);
        assertEquals(0,inboxSession.startTotalMessageCount);
        assertEquals(0,inboxSession.endTotalMessageCount);
    }
}