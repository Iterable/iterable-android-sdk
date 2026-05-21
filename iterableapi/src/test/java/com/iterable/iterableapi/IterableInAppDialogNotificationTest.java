package com.iterable.iterableapi;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.graphics.Rect;
import android.view.KeyEvent;

import androidx.activity.ComponentActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;

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
                activity, "<html></html>", mockMessage("msg-2"), uri -> { },
                0.5, new Rect(), false,
                new IterableInAppMessage.InAppBgColor(null, 0.0),
                true, IterableInAppLocation.IN_APP
        );

        // Should reject since one is already showing
        assertTrue(!result);
    }

    // ===== Host Lifecycle Tests =====

    @Test
    public void hostDestroy_shouldDismissDialog_andClearSingleton() {
        IterableInAppDialogNotification dialog = createDialog();
        dialog.show();
        assertNotNull(IterableInAppDialogNotification.getInstance());

        controller.pause().stop().destroy();
        controller = null;

        assertNull("singleton must clear when host activity is destroyed",
                IterableInAppDialogNotification.getInstance());
        assertFalse("dialog window should be torn down after host destroy",
                dialog.isShowing());
    }

    @Test
    public void hostDestroy_afterDismiss_shouldNotReassertTeardown() {
        IterableInAppDialogNotification dialog = createDialog();
        dialog.show();
        dialog.dismiss();
        assertNull(IterableInAppDialogNotification.getInstance());

        // Stand up a fresh in-app on a different host BEFORE destroying the original
        // host. If dismiss() failed to remove its observer, the original host's
        // ON_DESTROY would call dismiss() on the (now stale) first dialog reference,
        // which is a no-op — but more importantly we want to confirm the teardown
        // doesn't accidentally clear the new singleton.
        ActivityController<ComponentActivity> controller2 =
                Robolectric.buildActivity(ComponentActivity.class).create().start().resume();
        try {
            IterableInAppDialogNotification second =
                    IterableInAppDialogNotification.createInstance(
                            controller2.get(),
                            "<html></html>",
                            true,
                            uri -> { },
                            IterableInAppLocation.IN_APP,
                            mockMessage("msg-2"),
                            0.5,
                            new Rect(),
                            false,
                            new IterableInAppMessage.InAppBgColor(null, 0.0)
                    );
            assertNotNull(second);
            second.show();
            assertNotNull(IterableInAppDialogNotification.getInstance());

            // Destroying the original host must not reach into the singleton (the
            // observer was removed in dismiss()) and must not crash.
            controller.pause().stop().destroy();
            controller = null;

            assertNotNull("destroying the original host must not affect the new dialog",
                    IterableInAppDialogNotification.getInstance());
        } finally {
            if (IterableInAppDialogNotification.getInstance() != null) {
                IterableInAppDialogNotification.getInstance().dismiss();
            }
            controller2.pause().stop().destroy();
        }
    }

    // ===== createInstance Guard Tests =====

    @Test
    public void createInstance_shouldReturnNull_whenHostAlreadyDestroyed() {
        // Build and immediately destroy a host so its lifecycle is in DESTROYED.
        ActivityController<ComponentActivity> destroyedController =
                Robolectric.buildActivity(ComponentActivity.class).create().start().resume();
        ComponentActivity destroyedActivity = destroyedController.get();
        destroyedController.pause().stop().destroy();

        IterableInAppDialogNotification dialog =
                IterableInAppDialogNotification.createInstance(
                        destroyedActivity,
                        "<html></html>",
                        true,
                        uri -> { },
                        IterableInAppLocation.IN_APP,
                        mockMessage("msg-destroyed"),
                        0.5,
                        new Rect(),
                        false,
                        new IterableInAppMessage.InAppBgColor(null, 0.0)
                );

        assertNull("createInstance must refuse a destroyed host", dialog);
        assertNull("singleton must remain unset when createInstance refuses",
                IterableInAppDialogNotification.getInstance());
    }

    @Test
    public void createInstance_shouldReturnNull_whenHostNotLifecycleOwner() {
        // A plain android.app.Activity does not implement LifecycleOwner, so the
        // observer-based teardown path can't attach — createInstance must refuse
        // rather than silently leak the singleton.
        ActivityController<android.app.Activity> plainController =
                Robolectric.buildActivity(android.app.Activity.class).create().start().resume();
        try {
            IterableInAppDialogNotification dialog =
                    IterableInAppDialogNotification.createInstance(
                            plainController.get(),
                            "<html></html>",
                            true,
                            uri -> { },
                            IterableInAppLocation.IN_APP,
                            mockMessage("msg-plain"),
                            0.5,
                            new Rect(),
                            false,
                            new IterableInAppMessage.InAppBgColor(null, 0.0)
                    );

            assertNull("createInstance must refuse a non-LifecycleOwner host", dialog);
            assertNull("singleton must remain unset when createInstance refuses",
                    IterableInAppDialogNotification.getInstance());
        } finally {
            plainController.pause().stop().destroy();
        }
    }

    @Test
    public void hostDestroy_shouldUnblockNextInApp() {
        IterableInAppDialogNotification first = createDialog();
        first.show();

        controller.pause().stop().destroy();
        controller = null;

        // A fresh activity should be able to show the next in-app, which would not be the
        // case if the prior dialog had leaked its singleton on host destroy.
        ActivityController<ComponentActivity> controller2 =
                Robolectric.buildActivity(ComponentActivity.class).create().start().resume();
        try {
            boolean shown = IterableInAppDisplayer.showIterableDialogNotificationHTML(
                    controller2.get(), "<html></html>", mockMessage("msg-2"), uri -> { },
                    0.5, new Rect(), false,
                    new IterableInAppMessage.InAppBgColor(null, 0.0),
                    true, IterableInAppLocation.IN_APP
            );

            assertTrue("next in-app must be shown after prior host destroyed", shown);
        } finally {
            if (IterableInAppDialogNotification.getInstance() != null) {
                IterableInAppDialogNotification.getInstance().dismiss();
            }
            controller2.pause().stop().destroy();
        }
    }

    // ===== Cross-Singleton Guard Tests =====

    @Test
    public void displayer_shouldRejectFragmentRequest_whenDialogShowing() {
        createDialog().show();
        assertNotNull(IterableInAppDialogNotification.getInstance());

        // Even though we pass a FragmentActivity-shaped context, the displayer must not
        // create a Fragment in-app while a Dialog in-app is already up.
        boolean shown = IterableInAppDisplayer.showIterableFragmentNotificationHTML(
                Robolectric.buildActivity(androidx.fragment.app.FragmentActivity.class)
                        .create().start().resume().get(),
                "<html></html>", "msg-2", uri -> { }, 0.0, new Rect(), false,
                new IterableInAppMessage.InAppBgColor(null, 0.0f),
                false, IterableInAppLocation.IN_APP
        );

        assertFalse(shown);
        assertNull(IterableInAppFragmentHTMLNotification.getInstance());
    }

    // ===== Back-press Telemetry Tests =====

    @Test
    public void backPress_shouldTrackClickCloseAndRemove() {
        InAppTrackingService trackingService = Mockito.mock(InAppTrackingService.class);
        IterableInAppMessage message = mockMessage("backpress-msg");
        IterableInAppDialogNotification dialog = createDialogWithTrackingService(message, trackingService);

        dialog.show();

        // Synthesize a back-press: the dialog installs a key listener via setOnKeyListener
        // that fires on KEYCODE_BACK / ACTION_UP.
        dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));

        verify(trackingService, times(1))
                .trackInAppClick(eq(message), eq("itbl://backButton"), any());
        verify(trackingService, times(1))
                .trackInAppClose(eq(message), eq("itbl://backButton"),
                        eq(IterableInAppCloseAction.BACK), any());
        verify(trackingService, times(1)).removeMessage(message);
    }

    // ===== Lifecycle Dismiss Telemetry Tests =====

    @Test
    public void hostDestroy_shouldCallProcessMessageRemoval_beforeDismiss() {
        // Use createInstance so the real lifecycle observer attaches; pre-publish a
        // mocked tracking service into the singleton's services slot via a spied
        // companion. We can't hot-swap InAppServices.tracking after lazy init, so we
        // instead replace the IterableApi singleton's in-app manager with a mock and
        // verify removeMessage propagates through the real tracking service.
        IterableInAppManager mockManager = Mockito.mock(IterableInAppManager.class);
        IterableApi originalApi = IterableApi.sharedInstance;
        IterableApi.sharedInstance = new IterableApi(mockManager);

        IterableInAppMessage message = mockMessage("destroy-msg");
        Mockito.when(message.isMarkedForDeletion()).thenReturn(true);
        Mockito.when(message.isConsumed()).thenReturn(false);

        // The InAppServices.tracking field is lazy and binds to whatever
        // IterableApi.sharedInstance was at first access — so for tests, the tracking
        // service may already hold a reference to the original API. Skip directly to
        // the constructor-injected dialog with a real tracking service bound to the
        // mock manager.
        InAppTrackingService trackingService = new InAppTrackingService(IterableApi.sharedInstance);

        try {
            IterableInAppDialogNotification dialog = createDialogWithTrackingServiceAndAttachLifecycle(
                    message, trackingService);
            dialog.show();
            assertTrue(dialog.isShowing());

            controller.pause().stop().destroy();
            controller = null;

            // The lifecycle observer's onDestroy calls processMessageRemoval() →
            // trackingService.removeMessage → inAppManager.removeMessage so the server
            // consume goes out for the marked-for-deletion message.
            verify(mockManager, times(1)).removeMessage(message);
            assertFalse(dialog.isShowing());
            assertNull(IterableInAppDialogNotification.getInstance());
        } finally {
            IterableApi.sharedInstance = originalApi;
        }
    }

    // ===== Resize Tests =====

    @Test
    public void runResizeScript_shouldNotCrash_andCancelsOnDismiss() {
        IterableInAppDialogNotification dialog = createDialogWithPadding(new Rect(0, 10, 0, 10));
        dialog.show();

        // Schedules a debounced resize; without removal in dismiss() the runnable could
        // fire after teardown and touch a destroyed window.
        dialog.runResizeScript();
        dialog.dismiss();

        // dismiss() removed the pending resize — running the looper should be a no-op.
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertNull(IterableInAppDialogNotification.getInstance());
    }

    // ===== Helper Methods =====

    /**
     * Builds a dialog via the primary constructor with the given tracking service. Used
     * by tests that need to assert tracking-service interactions without going through
     * createInstance (which uses InAppServices.tracking, a lazy field). Does NOT attach
     * the lifecycle observer — use createDialogWithTrackingServiceAndAttachLifecycle if
     * you need the host-destroy teardown path.
     */
    private IterableInAppDialogNotification createDialogWithTrackingService(
            IterableInAppMessage message, InAppTrackingService trackingService) {
        return new IterableInAppDialogNotification(
                activity,
                "<html><body>Test</body></html>",
                true,
                message,
                0.5,
                new Rect(0, 0, 0, 0),
                false,
                0.0,
                null,
                InAppServices.INSTANCE.getLayout(),
                InAppServices.INSTANCE.getAnimation(),
                trackingService,
                InAppServices.INSTANCE.getWebView(),
                InAppServices.INSTANCE.getOrientation()
        );
    }

    /**
     * Same as createDialogWithTrackingService but also publishes the dialog into the
     * singleton slot and attaches the lifecycle observer that production code attaches
     * inside createInstance — so that controller.destroy() actually runs the teardown
     * path under test.
     */
    private IterableInAppDialogNotification createDialogWithTrackingServiceAndAttachLifecycle(
            IterableInAppMessage message, InAppTrackingService trackingService) {
        IterableInAppDialogNotification dialog = createDialogWithTrackingService(message, trackingService);
        // Reflect on private companion-object fields to simulate what createInstance
        // does: publish the singleton + attach the observer. Going through reflection
        // because the companion fields are intentionally @JvmStatic private.
        try {
            java.lang.reflect.Field notifField =
                    IterableInAppDialogNotification.class.getDeclaredField("notificationRef");
            notifField.setAccessible(true);
            notifField.set(null, new java.lang.ref.WeakReference<>(dialog));

            java.lang.reflect.Field observerField =
                    IterableInAppDialogNotification.class.getDeclaredField("hostLifecycleObserver");
            observerField.setAccessible(true);
            androidx.lifecycle.DefaultLifecycleObserver observer =
                    (androidx.lifecycle.DefaultLifecycleObserver) observerField.get(dialog);
            ((androidx.lifecycle.LifecycleOwner) activity).getLifecycle().addObserver(observer);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Reflection failed setting up lifecycle test", e);
        }
        return dialog;
    }


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
                mockMessage("test-message"),
                0.5,
                padding,
                false,
                new IterableInAppMessage.InAppBgColor(null, 0.0)
        );
    }

    private IterableInAppMessage mockMessage(String messageId) {
        IterableInAppMessage message = Mockito.mock(IterableInAppMessage.class);
        Mockito.when(message.getMessageId()).thenReturn(messageId);
        return message;
    }
}
