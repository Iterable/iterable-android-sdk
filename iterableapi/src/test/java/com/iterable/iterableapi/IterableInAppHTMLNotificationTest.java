package com.iterable.iterableapi;

import android.app.Activity;
import android.graphics.Rect;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

public class IterableInAppHTMLNotificationTest extends BasePowerMockTest {

    @Test
    public void testDoNotCrashOnResizeAfterDismiss() {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create().start();
        Activity activity = controller.get();

        IterableInAppDisplayer.showIterableNotificationHTML(activity, "", "", null, 0.0, new Rect());

        IterableInAppHTMLNotification notification = IterableInAppHTMLNotification.getInstance();
        notification.dismiss();
        notification.resize(500.0f);
    }
}
