package com.iterable.iterableapi;

import android.graphics.Rect;

import androidx.fragment.app.FragmentActivity;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

public class IterableInAppHTMLNotificationTest extends BaseTest {

    @Test
    public void testDoNotCrashOnResizeAfterDismiss() {
        ActivityController<FragmentActivity> controller = Robolectric.buildActivity(FragmentActivity.class).create().start().resume();
        FragmentActivity activity = controller.get();
        IterableInAppDisplayer.showIterableFragmentNotificationHTML(activity, "", "", null, 0.0, new Rect(), true, new IterableInAppMessage.InAppBgColor(null, 0.0f), false, IterableInAppLocation.IN_APP);

        IterableInAppFragmentHTMLNotification notification = IterableInAppFragmentHTMLNotification.getInstance();
        notification.dismiss();
        notification.resize(500.0f);
    }
}
