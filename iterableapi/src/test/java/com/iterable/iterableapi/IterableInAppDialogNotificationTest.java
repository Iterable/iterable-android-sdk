package com.iterable.iterableapi;

import android.graphics.Rect;

import androidx.activity.ComponentActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class IterableInAppDialogNotificationTest extends BaseTest {

    private ActivityController<ComponentActivity> controller;
    private ComponentActivity activity;

    @Before
    public void setUp() {
        IterableTestUtils.createIterableApiNew();
        controller = Robolectric.buildActivity(ComponentActivity.class).create().start().resume();
        activity = controller.get();
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

    // ===== Singleton Lifecycle Tests =====

    @Test
    public void getInstance_shouldReturnNull_whenNoDialogCreated() {
        assertNull(IterableInAppDialogNotification.getInstance());
    }

    @Test
    public void getInstance_shouldReturnInstance_afterCreateInstance() {
        createDialog();
        assertNotNull(IterableInAppDialogNotification.getInstance());
    }

    @Test
    public void getInstance_shouldReturnNull_afterDismiss() {
        IterableInAppDialogNotification dialog = createDialog();
        dialog.show();
        dialog.dismiss();
        assertNull(IterableInAppDialogNotification.getInstance());
    }

    // ===== Show/Dismiss Tests =====

    @Test
    public void show_shouldDisplayDialog() {
        IterableInAppDialogNotification dialog = createDialog();
        dialog.show();
        assertTrue(dialog.isShowing());
    }

    @Test
    public void dismiss_shouldCleanupSingletonState() {
        IterableInAppDialogNotification dialog = createDialog();
        dialog.show();
        assertNotNull(IterableInAppDialogNotification.getInstance());

        dialog.dismiss();
        assertNull(IterableInAppDialogNotification.getInstance());
    }

    @Test
    public void testDoNotCrashOnResizeAfterDismiss() {
        IterableInAppDialogNotification dialog = createDialog();
        dialog.show();
        dialog.dismiss();
        dialog.runResizeScript();
    }

    // ===== URL Click Tests =====

    @Test
    public void onUrlClicked_shouldNotCrash_whenUrlIsNull() {
        IterableInAppDialogNotification dialog = createDialog();
        dialog.show();
        dialog.onUrlClicked(null);
    }

    @Test
    public void onUrlClicked_shouldDismissDialog() {
        IterableInAppDialogNotification dialog = createDialog();
        dialog.show();
        assertTrue(dialog.isShowing());

        dialog.onUrlClicked("https://example.com");
        assertNull(IterableInAppDialogNotification.getInstance());
    }

    // ===== Layout Variant Tests =====

    @Test
    public void showDialog_shouldDisplay_withFullscreenPadding() {
        IterableInAppDialogNotification dialog = createDialogWithPadding(new Rect(0, 0, 0, 0));
        dialog.show();
        assertTrue(dialog.isShowing());
    }

    @Test
    public void showDialog_shouldDisplay_withTopPadding() {
        IterableInAppDialogNotification dialog = createDialogWithPadding(new Rect(0, 10, 0, 0));
        dialog.show();
        assertTrue(dialog.isShowing());
    }

    @Test
    public void showDialog_shouldDisplay_withBottomPadding() {
        IterableInAppDialogNotification dialog = createDialogWithPadding(new Rect(0, 0, 0, 10));
        dialog.show();
        assertTrue(dialog.isShowing());
    }

    @Test
    public void showDialog_shouldDisplay_withCenterPadding() {
        IterableInAppDialogNotification dialog = createDialogWithPadding(new Rect(0, 10, 0, 10));
        dialog.show();
        assertTrue(dialog.isShowing());
    }

    // ===== Displayer Integration Tests =====

    @Test
    public void displayer_shouldRejectDuplicate_whenDialogAlreadyShowing() {
        createDialog().show();

        boolean result = IterableInAppDisplayer.showIterableDialogNotificationHTML(
                activity, "<html></html>", "msg-2", uri -> { },
                0.5, new Rect(), false,
                new IterableInAppMessage.InAppBgColor(null, 0.0),
                true, IterableInAppLocation.IN_APP
        );

        // Should reject since one is already showing
        assertTrue(!result);
    }

    // ===== Helper Methods =====

    private IterableInAppDialogNotification createDialog() {
        return createDialogWithPadding(new Rect(0, 0, 0, 0));
    }

    private IterableInAppDialogNotification createDialogWithPadding(Rect padding) {
        return IterableInAppDialogNotification.createInstance(
                activity,
                "<html><body>Test</body></html>",
                true,
                uri -> { },
                IterableInAppLocation.IN_APP,
                "test-message",
                0.5,
                padding,
                false,
                new IterableInAppMessage.InAppBgColor(null, 0.0)
        );
    }
}
