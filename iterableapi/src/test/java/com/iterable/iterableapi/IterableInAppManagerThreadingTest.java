package com.iterable.iterableapi;

import static android.os.Looper.getMainLooper;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import androidx.activity.ComponentActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;

public class IterableInAppManagerThreadingTest extends BaseTest {

    private ActivityController<ComponentActivity> controller;
    private IterableInAppManager inAppManager;
    private IterableInAppMessage message;

    @Before
    public void setUp() throws Exception {
        IterableTestUtils.createIterableApiNew();
        controller = Robolectric.buildActivity(ComponentActivity.class).create().start().resume();
        shadowOf(getMainLooper()).idle();
        inAppManager = IterableApi.getInstance().getInAppManager();
        message = InAppTestUtils.getTestInAppMessage();
    }

    @After
    public void tearDown() {
        if (ShadowDialog.getLatestDialog() != null) {
            ShadowDialog.getLatestDialog().dismiss();
        }
        if (controller != null) {
            controller.pause().stop().destroy();
        }
        IterableTestUtils.resetIterableApi();
    }

    @Test
    public void showMessage_fromBackgroundLooperThread_displaysOnMainThreadWithoutCrashing() throws InterruptedException {
        LooperTestUtils.runOnBackgroundLooperAndWait(
                () -> inAppManager.showMessage(message, true, null));

        assertNull(IterableInAppDialogNotification.getInstance());
        assertFalse(message.isRead());
        assertFalse(message.isMarkedForDeletion());

        shadowOf(getMainLooper()).idle();

        assertNotNull(IterableInAppDialogNotification.getInstance());
        assertTrue(message.isRead());
        assertTrue(message.isMarkedForDeletion());
    }

    @Test
    public void showMessage_fromMainThread_displaysSynchronously() {
        inAppManager.showMessage(message, true, null);

        assertNotNull(IterableInAppDialogNotification.getInstance());
        assertTrue(message.isRead());
        assertTrue(message.isMarkedForDeletion());
    }
}
