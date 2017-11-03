package com.iterable.iterableapi;

import android.app.Application;
import android.graphics.Rect;
import android.test.ApplicationTestCase;
import android.view.Gravity;

/**
 * Tests
 * Created by David Truong dt@iterable.com.
 */
public class IterableInAppTest extends ApplicationTestCase<Application> {
    public IterableInAppTest() {
        super(Application.class);
    }

    IterableInAppHTMLNotification notification;

    public void setUp() throws Exception {
        super.setUp();

        notification = IterableInAppHTMLNotification.createInstance(getContext().getApplicationContext(), "");
    }

    public void testGetLocationFull() {
        Rect padding = new Rect(0,0,0,0);
        int verticalLocation = notification.getVerticalLocation(padding);
        assertEquals(Gravity.CENTER_VERTICAL, verticalLocation);
    }

    public void testGetLocationTop() {
        Rect padding = new Rect(0,0,0,-1);
        int verticalLocation = notification.getVerticalLocation(padding);
        assertEquals(Gravity.TOP, verticalLocation);
    }

    public void testGetLocationBottom() throws Exception {
        Rect padding = new Rect(0,-1,0,0);
        int verticalLocation = notification.getVerticalLocation(padding);
        assertEquals(Gravity.BOTTOM, verticalLocation);
    }

    public void testGetLocationCenter() {
        Rect padding = new Rect(0,-1,0,-1);
        int verticalLocation = notification.getVerticalLocation(padding);
        assertEquals(Gravity.CENTER_VERTICAL, verticalLocation);
    }
    
    public void testGetLocationRandom() {
        Rect padding = new Rect(0,20,0,30);
        int verticalLocation = notification.getVerticalLocation(padding);
        assertEquals(Gravity.CENTER_VERTICAL, verticalLocation);
    }
}
