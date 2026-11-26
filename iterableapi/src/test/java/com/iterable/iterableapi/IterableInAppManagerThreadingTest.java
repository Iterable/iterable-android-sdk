package com.iterable.iterableapi;

import static android.os.Looper.getMainLooper;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Handler;
import android.os.HandlerThread;

import androidx.activity.ComponentActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
        // A looper thread, like the React Native native-modules thread.
        HandlerThread background = new HandlerThread("background");
        background.start();
        final AtomicReference<Throwable> thrown = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);
        new Handler(background.getLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    inAppManager.showMessage(message, true, null);
                } catch (Throwable t) {
                    thrown.set(t);
                } finally {
                    done.countDown();
                }
            }
        });
        assertTrue(done.await(5, TimeUnit.SECONDS));
        background.quitSafely();

        assertNull(thrown.get());
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
