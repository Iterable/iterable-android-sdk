package com.iterable.iterableapi;

import android.graphics.Rect;
import android.view.Gravity;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static junit.framework.Assert.assertEquals;

/**
 * Tests
 * Created by David Truong dt@iterable.com.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class IterableInAppTest {

    IterableInAppHTMLNotification notification;

    @Before
    @UiThreadTest
    public void setUp() throws Exception {
        notification = IterableInAppHTMLNotification.createInstance(getApplicationContext(), "");
    }

    @Test
    @UiThreadTest
    public void testGetLocationFull() {
        Rect padding = new Rect(0,0,0,0);
        int verticalLocation = notification.getVerticalLocation(padding);
        assertEquals(Gravity.CENTER_VERTICAL, verticalLocation);
    }

    @Test
    @UiThreadTest
    public void testGetLocationTop() {
        Rect padding = new Rect(0,0,0,-1);
        int verticalLocation = notification.getVerticalLocation(padding);
        assertEquals(Gravity.TOP, verticalLocation);
    }

    @Test
    @UiThreadTest
    public void testGetLocationBottom() throws Exception {
        Rect padding = new Rect(0,-1,0,0);
        int verticalLocation = notification.getVerticalLocation(padding);
        assertEquals(Gravity.BOTTOM, verticalLocation);
    }

    @Test
    @UiThreadTest
    public void testGetLocationCenter() {
        Rect padding = new Rect(0,-1,0,-1);
        int verticalLocation = notification.getVerticalLocation(padding);
        assertEquals(Gravity.CENTER_VERTICAL, verticalLocation);
    }

    @Test
    @UiThreadTest
    public void testGetLocationRandom() {
        Rect padding = new Rect(0,20,0,30);
        int verticalLocation = notification.getVerticalLocation(padding);
        assertEquals(Gravity.CENTER_VERTICAL, verticalLocation);
    }
}
