package com.iterable.iterableapi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Verifies that the manager getters fail gracefully (no-op stub instead of RuntimeException) when
 * the SDK has not been initialized. See SDK-417.
 */
public class IterableApiPreInitTest extends BaseTest {

    @Test
    public void getInAppManager_beforeInit_returnsNoOpInsteadOfThrowing() {
        IterableInAppManager manager = IterableApi.getInstance().getInAppManager();

        assertNotNull(manager);
        assertTrue(manager instanceof EmptyInAppManager);
    }

    @Test
    public void getInAppManager_noOp_returnsEmptyDefaults() {
        IterableInAppManager manager = IterableApi.getInstance().getInAppManager();

        assertTrue(manager.getMessages().isEmpty());
        assertTrue(manager.getInboxMessages().isEmpty());
        assertNull(manager.getMessageById("anyId"));
    }

    @Test
    public void getInAppManagerOrNull_beforeInit_returnsNull() {
        assertNull(IterableApi.getInstance().getInAppManagerOrNull());
    }

    @Test
    public void getInAppManager_beforeInit_returnsStableInstance() {
        IterableApi api = IterableApi.getInstance();
        assertSame(api.getInAppManager(), api.getInAppManager());
    }

    @Test
    public void getEmbeddedManager_beforeInit_returnsNoOpInsteadOfThrowing() {
        IterableEmbeddedManager manager = IterableApi.getInstance().getEmbeddedManager();

        assertNotNull(manager);
        assertTrue(manager instanceof EmptyEmbeddedManager);
    }

    @Test
    public void getEmbeddedManager_noOp_returnsEmptyDefaults() {
        IterableEmbeddedManager manager = IterableApi.getInstance().getEmbeddedManager();

        assertNull(manager.getMessages(0L));
        assertTrue(manager.getPlacementIds().isEmpty());
        assertTrue(manager.getUpdateHandlers().isEmpty());
        assertNotNull(manager.getEmbeddedSessionManager());
    }

    @Test
    public void getEmbeddedManagerOrNull_beforeInit_returnsNull() {
        assertNull(IterableApi.getInstance().getEmbeddedManagerOrNull());
    }
}
