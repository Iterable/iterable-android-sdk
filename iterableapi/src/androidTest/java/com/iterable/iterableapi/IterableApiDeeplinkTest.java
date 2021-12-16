package com.iterable.iterableapi;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class IterableApiDeepLinkTest {
    @Before
    public void setUp() {
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testUniversalDeepLinkNoRewrite() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        try {
            final String requestString = "http://links.iterable.com/u/60402396fbd5433eb35397b47ab2fb83?_e=joneng%40iterable.com&_m=93125f33ba814b13a882358f8e0852e0";
            IterableHelper.IterableActionHandler clickCallback = new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String result) {
                    assertEquals(requestString, result);
                    signal.countDown();
                }
            };

            IterableApi.getInstance().getAndTrackDeepLink(requestString, clickCallback);
            assertTrue("callback is called", signal.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNoURLRedirect() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        try {
            final String requestString = "https://httpbin.org/redirect-to?url=http://example.com";
            final String redirectString = "http://example.com";
            IterableHelper.IterableActionHandler clickCallback = new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String result) {
                    assertFalse(result.equalsIgnoreCase(redirectString));
                    assertEquals(requestString, result);
                    signal.countDown();
                }
            };

            IterableApi.getInstance().getAndTrackDeepLink(requestString, clickCallback);
            assertTrue("callback is called", signal.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testEmptyRedirect() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        try {
            final String requestString = "";
            IterableHelper.IterableActionHandler clickCallback = new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String result) {
                    assertEquals(requestString, result);
                    signal.countDown();
                }
            };
            IterableApi.getInstance().getAndTrackDeepLink(requestString, clickCallback);
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNullRedirect() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        try {
            final String requestString = null;
            IterableHelper.IterableActionHandler clickCallback = new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String result) {
                    assertEquals(requestString, result);
                    signal.countDown();
                }
            };
            IterableApi.getInstance().getAndTrackDeepLink(requestString, clickCallback);
            assertTrue("callback is called", signal.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMultiRedirectNoRewrite() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        try {
            final String requestString = "https://httpbin.org/redirect/3";
            final String redirectString = "https://httpbin.org/redirect/3";
            IterableHelper.IterableActionHandler clickCallback = new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String result) {
                    assertEquals(redirectString, result);
                    signal.countDown();
                }
            };
            IterableApi.getInstance().getAndTrackDeepLink(requestString, clickCallback);
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
