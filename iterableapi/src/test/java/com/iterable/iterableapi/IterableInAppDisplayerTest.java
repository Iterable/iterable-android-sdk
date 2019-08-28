package com.iterable.iterableapi;


import android.app.Activity;
import android.graphics.Rect;
import android.net.Uri;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

public class IterableInAppDisplayerTest extends BaseTest {
    @Test
    public void testCallbackOnCancel() {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create().start();
        Activity activity = controller.get();

        IterableHelper.IterableUrlCallback clickCallback = Mockito.mock(IterableHelper.IterableUrlCallback.class);
        IterableInAppDisplayer.showIterableNotificationHTML(activity, "", "", clickCallback, 0.0, new Rect(), true);
        IterableInAppHTMLNotification notification = IterableInAppHTMLNotification.getInstance();

        notification.cancel();
    }
}


