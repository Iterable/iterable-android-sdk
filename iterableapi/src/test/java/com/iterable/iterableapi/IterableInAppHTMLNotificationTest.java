package com.iterable.iterableapi;

import android.app.Activity;
import android.graphics.Rect;
import android.net.Uri;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;



public class IterableInAppHTMLNotificationTest extends BaseTest {

    @Test
    public void testDoNotCrashOnResizeAfterDismiss() {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create().start();
        Activity activity = controller.get();

        IterableInAppDisplayer.showIterableNotificationHTML(activity, "", "", null, 0.0, new Rect());

        IterableInAppHTMLNotification notification = IterableInAppHTMLNotification.getInstance();
        notification.dismiss();
        notification.resize(500.0f);
    }

    @Test
    public void testBackButtonPress() {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create().start();
        Activity activity = controller.get();

        IterableInAppDisplayer.showIterableNotificationHTML(activity, "", "", null, 0.0, new Rect());

        IterableInAppHTMLNotification notification = IterableInAppHTMLNotification.getInstance();
        notification.onBackPressed();
    }

    @Test
    public void testInsetPadding() {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create().start();
        Activity activity = controller.get();

        IterableInAppDisplayer.showIterableNotificationHTML(activity, "", "", null, 0.0, new Rect());

        IterableInAppHTMLNotification notification = IterableInAppHTMLNotification.getInstance();
        Rect padding = new Rect(0,10,0,0);
        notification.setPadding(padding);
        notification.resize(500);
    }

    @Test
    public void testOverrideUrlLoading() {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create().start();
        Activity activity = controller.get();

        IterableInAppDisplayer.showIterableNotificationHTML(activity, "", "", null, 0.0, new Rect());

        IterableInAppHTMLNotification notification = IterableInAppHTMLNotification.getInstance();
        notification.resize(500.0f);
    }


    @Test
    public void testOrientationChanged() {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create().start();
        Activity activity = controller.get();

        IterableInAppDisplayer.showIterableNotificationHTML(activity, "", "", null, 0.0, new Rect());

        IterableInAppHTMLNotification notification = IterableInAppHTMLNotification.getInstance();
        notification.orientationListener.onOrientationChanged(1);
    }
}
