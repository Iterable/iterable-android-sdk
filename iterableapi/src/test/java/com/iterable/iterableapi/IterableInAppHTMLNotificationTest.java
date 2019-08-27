package com.iterable.iterableapi;

import android.app.Activity;
import android.graphics.Rect;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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

    @Test
    public void testCallbackOnCancel() {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create().start();
        Activity activity = controller.get();

        IterableHelper.IterableUrlCallback clickCallback = mock(IterableHelper.IterableUrlCallback.class);
        IterableInAppDisplayer.showIterableNotificationHTML(activity, "", "", clickCallback, 0.0, new Rect(), true);
        IterableInAppHTMLNotification notification = IterableInAppHTMLNotification.getInstance();
        notification.cancel();
        verify(clickCallback, times(1)).execute(null);
    }
}
