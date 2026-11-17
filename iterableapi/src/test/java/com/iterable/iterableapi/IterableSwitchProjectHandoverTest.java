package com.iterable.iterableapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * The moment one switch in a chain hands over to the next: the project that just landed is live
 * and serves the calls its own callbacks make, and every other call waits for the next switch.
 */
@RunWith(TestRunner.class)
public class IterableSwitchProjectHandoverTest extends BaseTest {

    private static final String API_KEY_A = "project-a-key";
    private static final String API_KEY_B = "project-b-key";
    private static final String API_KEY_C = "project-c-key";
    private static final String EMAIL_A = "user-a@example.com";
    private static final String EMAIL_B = "user-b@example.com";

    private Context context;
    private MockWebServer server;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(200).setBody("{}");
            }
        });
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        IterableTestUtils.resetIterableApi();
        IterableApi.initialize(context, API_KEY_A, configWithoutAuth());
        IterableApi.getInstance().setEmail(EMAIL_A);
        drainMainThread();
    }

    @After
    public void tearDown() throws Exception {
        settle();
        IterableTestUtils.resetIterableApi();
        IterableRequestTask.overrideUrl = null;
        server.shutdown();
    }

    /**
     * B lands with C queued. A call made from another thread at that moment was asked for after C,
     * so it has to wait for C, behind the call already queued for C, rather than run on B.
     */
    @Test
    public void testACallFromAnotherThreadDuringTheHandoverWaitsForTheNextSwitch() throws Exception {
        List<String> ran = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch landedOnB = new CountDownLatch(1);
        CountDownLatch landedOnC = new CountDownLatch(1);

        switchTo(API_KEY_B, ignored -> {
            Thread other = new Thread(() -> IterableBackgroundInitializer.queueOrExecute(
                    () -> ran.add("fromAnotherThread@" + liveApiKey()), "fromAnotherThread"));
            other.start();
            try {
                other.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            landedOnB.countDown();
        });
        switchTo(API_KEY_C, ignored -> landedOnC.countDown());
        IterableBackgroundInitializer.queueOrExecute(() -> ran.add("queuedForC@" + liveApiKey()), "queuedForC");

        assertTrue(awaitSwitch(landedOnB));
        assertTrue(awaitSwitch(landedOnC));
        settle();
        assertEquals(List.of("queuedForC@" + API_KEY_C, "fromAnotherThread@" + API_KEY_C), ran);
    }

    /**
     * Holding calls for C must not make B look uninitialized to its own callback: apps check
     * isSDKInitialized() before using the SDK, and B is live.
     */
    @Test
    public void testTheLandedProjectReportsInitializedInsideItsCallbackWhileAnotherSwitchIsQueued() throws Exception {
        AtomicBoolean initializedInCallback = new AtomicBoolean(false);
        AtomicBoolean initializingInCallback = new AtomicBoolean(true);
        CountDownLatch landedOnB = new CountDownLatch(1);
        CountDownLatch landedOnC = new CountDownLatch(1);

        switchTo(API_KEY_B, ignored -> {
            IterableApi.getInstance().setEmail(EMAIL_B);
            initializedInCallback.set(IterableApi.isSDKInitialized());
            initializingInCallback.set(IterableApi.isSDKInitializing());
            landedOnB.countDown();
        });
        switchTo(API_KEY_C, ignored -> landedOnC.countDown());

        assertTrue(awaitSwitch(landedOnB));
        assertTrue(awaitSwitch(landedOnC));
        assertTrue("B is live and identified inside its own callback", initializedInCallback.get());
        assertFalse("B is not initializing inside its own callback", initializingInCallback.get());
    }

    /**
     * An initializeInBackground callback parked during B's teardown is delivered as B lands. It is
     * a landed callback like the switch's own, so a call made from it reaches B.
     */
    @Test
    public void testAnInitializationCallbackDeliveredAtTheHandoverReachesTheLandedProject() throws Exception {
        List<String> ran = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch initialized = new CountDownLatch(1);
        CountDownLatch landedOnC = new CountDownLatch(1);

        switchTo(API_KEY_B, null);
        IterableApi.initializeInBackground(context, API_KEY_B, configWithoutAuth(), () -> {
            IterableBackgroundInitializer.queueOrExecute(
                    () -> ran.add("fromInitCallback@" + liveApiKey()), "fromInitCallback");
            initialized.countDown();
        });
        switchTo(API_KEY_C, ignored -> landedOnC.countDown());

        assertTrue(awaitSwitch(initialized));
        assertTrue(awaitSwitch(landedOnC));
        assertEquals(List.of("fromInitCallback@" + API_KEY_B), ran);
    }

    // region Helpers

    private static String liveApiKey() {
        return IterableApi.getInstance()._apiKey;
    }

    private IterableConfig configWithoutAuth() {
        return new IterableConfig.Builder()
                .setAutoPushRegistration(false)
                .setKeychainEncryption(false)
                .build();
    }

    private void switchTo(String apiKey, IterableProjectSwitchCallback callback) {
        IterableApi.switchProject(context, new IterableProject(apiKey, configWithoutAuth()), callback);
    }

    private void drainMainThread() {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }

    private boolean awaitSwitch(CountDownLatch latch) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            drainMainThread();
            if (latch.await(50, TimeUnit.MILLISECONDS)) {
                drainMainThread();
                return true;
            }
        }
        return false;
    }

    private void settle() throws InterruptedException {
        for (int i = 0; i < 200 && IterableBackgroundInitializer.isSwitchingProject(); i++) {
            drainMainThread();
            Thread.sleep(10);
        }
        drainMainThread();
    }

    // endregion
}
