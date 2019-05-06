package com.iterable.iterableapi;

import android.app.Activity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

public class IterableActivityMonitorTest extends BaseTest {

    @Before
    public void setUp() {
        IterableActivityMonitor.getInstance().registerLifecycleCallbacks(RuntimeEnvironment.application);
    }

    @After
    public void tearDown() {
        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(RuntimeEnvironment.application);
        IterableActivityMonitor.instance = new IterableActivityMonitor();
    }

    @Test
    public void testOneActivityStarted() {
        Robolectric.buildActivity(Activity.class).create().start().resume();

        Robolectric.flushForegroundThreadScheduler();
        assertTrue(IterableActivityMonitor.getInstance().isInForeground());
    }

    @Test
    public void testOneActivityStartedStopped() {
        Robolectric.buildActivity(Activity.class).create().start().resume().pause().stop();

        Robolectric.flushForegroundThreadScheduler();
        assertFalse(IterableActivityMonitor.getInstance().isInForeground());
    }

    @Test
    public void testMultipleActivities() {
        ActivityController<Activity> activity1 = Robolectric.buildActivity(Activity.class).create().start().resume();
        ActivityController<Activity> activity2 = Robolectric.buildActivity(Activity.class).create().start().resume();

        Robolectric.flushForegroundThreadScheduler();
        assertTrue(IterableActivityMonitor.getInstance().isInForeground());

        activity1.pause().stop();

        Robolectric.flushForegroundThreadScheduler();
        assertTrue(IterableActivityMonitor.getInstance().isInForeground());

        activity2.pause().stop();

        Robolectric.flushForegroundThreadScheduler();
        assertFalse(IterableActivityMonitor.getInstance().isInForeground());
    }

    @Test
    public void testCallbacks() {
        IterableActivityMonitor.AppStateCallback callback = mock(IterableActivityMonitor.AppStateCallback.class);
        IterableActivityMonitor.getInstance().addCallback(callback);
        ActivityController<Activity> activity = Robolectric.buildActivity(Activity.class).create().start().resume();

        Robolectric.flushForegroundThreadScheduler();
        verify(callback).onSwitchToForeground();

        activity.pause().stop();

        Robolectric.flushForegroundThreadScheduler();
        verify(callback).onSwitchToBackground();

        // Make sure the callback isn't called once it is removed
        reset(callback);
        IterableActivityMonitor.getInstance().removeCallback(callback);

        activity.start().resume();
        activity.pause().stop();
        Robolectric.flushForegroundThreadScheduler();
        verify(callback, never()).onSwitchToForeground();
        verify(callback, never()).onSwitchToBackground();
    }

}
