package com.iterable.iterableapi;

import android.os.Looper;
import android.support.test.annotation.UiThreadTest;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

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
public class IterableApiDeeplinkTest {

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

            IterableApi.getAndTrackDeeplink(requestString, clickCallback);
            assertTrue("callback is called", signal.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testUniversalDeepLinkRewrite() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        try {
            final String requestString = "http://links.iterable.com/a/60402396fbd5433eb35397b47ab2fb83?_e=joneng%40iterable.com&_m=93125f33ba814b13a882358f8e0852e0";
            final String redirectString = "https://links.iterable.com/api/docs#!/email";
            IterableHelper.IterableActionHandler clickCallback = new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String result) {
                    assertFalse(result.equalsIgnoreCase(requestString));
                    assertEquals(redirectString, result);
                    assertTrue("Callback is called on the main thread", Looper.getMainLooper().getThread() == Thread.currentThread());
                    signal.countDown();
                }
            };

            IterableApi.getAndTrackDeeplink(requestString, clickCallback);
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

            IterableApi.getAndTrackDeeplink(requestString, clickCallback);
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
            IterableApi.getAndTrackDeeplink(requestString, clickCallback);
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
            IterableApi.getAndTrackDeeplink(requestString, clickCallback);
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
            IterableApi.getAndTrackDeeplink(requestString, clickCallback);
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Check re-written link that is a redirected link: links.iterable -> http -> https.
    @Test
    public void testMultiRedirectRewrite() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        try {
            final String requestString = "http://links.iterable.com/a/d89cb7bb7cfb4a56963e0e9abae0f761?_e=dt%40iterable.com&_m=f285fd5320414b3d868b4a97233774fe";
            final String redirectString = "http://iterable.com/product/";
            final String redirectFinalString = "https://iterable.com/product/";
            IterableHelper.IterableActionHandler clickCallback = new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String result) {
                    assertEquals(redirectString, result);
                    assertFalse(redirectFinalString.equalsIgnoreCase(result));
                    signal.countDown();
                }
            };
            IterableApi.getAndTrackDeeplink(requestString, clickCallback);
            assertTrue("callback is called", signal.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Check 404 after Redirect
    @Test
    public void testDNSRedirect() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        try {
            final String requestString = "http://links.iterable.com/a/f4c55a1474074acba6ddbcc4e5a9eb38?_e=dt%40iterable.com&_m=f285fd5320414b3d868b4a97233774fe";
            final String redirectString = "http://iterable.com/product/fakeTest";
            final String redirectFinalString = "https://iterable.com/product/fakeTest";
            IterableHelper.IterableActionHandler clickCallback = new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String result) {
                    assertEquals(redirectString, result);
                    assertFalse(redirectFinalString.equalsIgnoreCase(result));
                    signal.countDown();
                }
            };
            IterableApi.getAndTrackDeeplink(requestString, clickCallback);
            assertTrue("callback is called", signal.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Check 404
    @Test
    public void testDNS() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        try {
            final String userId = "xxx";
            final String requestString = "http://links.iterable.com/a/"+userId+"?_e=email&_m=123";
            IterableHelper.IterableActionHandler clickCallback = new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String result) {
                    assertTrue(requestString.equalsIgnoreCase(result));
                    signal.countDown();
                }
            };
            IterableApi.getAndTrackDeeplink(requestString, clickCallback);
            assertTrue("callback is called", signal.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Check 400
    @Test
    public void testDNS400() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        try {
            final String requestString = "http://links.iterable.com/a/a";
            IterableHelper.IterableActionHandler clickCallback = new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String result) {
                    assertTrue(requestString.equalsIgnoreCase(result));
                    signal.countDown();
                }
            };
            IterableApi.getAndTrackDeeplink(requestString, clickCallback);
            assertTrue("callback is called", signal.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
