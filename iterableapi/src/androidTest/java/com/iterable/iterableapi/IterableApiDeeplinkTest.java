package com.iterable.iterableapi;

import android.app.Application;
import android.test.ApplicationTestCase;

import java.util.concurrent.CountDownLatch;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableApiDeeplinkTest extends ApplicationTestCase<Application> {

    public IterableApiDeeplinkTest() {
        super(Application.class);
    }

    public final String ITERABLE_IN_APP_TYPE_TOP     = "bad uuid";

    @Override
    public void setUp() {
        createApplication();
    }

    @Override
    public void tearDown() throws Exception {

    }

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
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

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
                    signal.countDown();
                }
            };

            IterableApi.getAndTrackDeeplink(requestString, clickCallback);
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

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
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

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
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Check 404 after Redirect
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
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Check 404
    public void testDNS() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        try {
            final String requestString = "http://links.iterable.com/a/xxx?_e=xx%40iterable.com&_m=xxx";
            final String redirectString = "http://links.iterable.com/a/xxx?_e=xx%40iterable.com&_m=xxx";
            final String badUuid = "bad uuid xxx";
            IterableHelper.IterableActionHandler clickCallback = new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String result) {
                    assertFalse(redirectString.equalsIgnoreCase(result));
                    assertEquals(badUuid, result);
                    signal.countDown();
                }
            };
            IterableApi.getAndTrackDeeplink(requestString, clickCallback);
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Check 400
    public void testDNS400() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        try {
            final String requestString = "http://links.iterable.com/a/a";
            IterableHelper.IterableActionHandler clickCallback = new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String result) {
                    assertFalse(requestString.equalsIgnoreCase(result));
                    signal.countDown();
                }
            };
            IterableApi.getAndTrackDeeplink(requestString, clickCallback);
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
