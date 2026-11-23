package com.iterable.iterableapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Requests that tear nothing down: asking for the project already in use, and a switchProject made
 * before the SDK was initialized. Each is ordered like any other switch, so a switch asked for
 * after it runs after its callback.
 */
@RunWith(TestRunner.class)
public class IterableSwitchProjectZeroLengthLegTest extends BaseTest {

    private static final String API_KEY_A = "project-a-key";
    private static final String API_KEY_B = "project-b-key";
    private static final String EMAIL_A = "user-a@example.com";
    private static final String EMAIL_REIDENTIFIED = "user-a-again@example.com";

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
    }

    @After
    public void tearDown() throws Exception {
        settle();
        IterableTestUtils.resetIterableApi();
        IterableRequestTask.overrideUrl = null;
        server.shutdown();
    }

    /**
     * On A, the app asks for A and re-identifies from the callback, then asks for B. The callback
     * reports A, so its setEmail has to reach A before B tears it down, not be replayed into B.
     */
    @Test
    public void testAnAlreadyThereCallbackRunsBeforeASwitchAskedForAfterIt() throws Exception {
        IterableApi.initialize(context, API_KEY_A, configWithoutAuth());
        IterableApi.getInstance().setEmail(EMAIL_A);
        drainMainThread();

        assertReIdentifyingFromTheFirstCallbackStaysOnA();
    }

    /** Same, with the first request made before the SDK was initialized. */
    @Test
    public void testAPreInitializationSwitchCallbackRunsBeforeASwitchAskedForAfterIt() throws Exception {
        assertReIdentifyingFromTheFirstCallbackStaysOnA();
    }

    /**
     * Asking for the project already in use tears nothing down, so the call gate stays down. Once a
     * switch is queued behind it, a call made from then on belongs to that switch.
     */
    @Test
    public void testACallMadeAfterASwitchIsQueuedBehindAnAlreadyThereRequestWaitsForIt() throws Exception {
        IterableApi.initialize(context, API_KEY_A, configWithoutAuth());
        drainMainThread();
        List<String> ran = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch landedOnB = new CountDownLatch(1);

        switchTo(API_KEY_A, null);
        switchTo(API_KEY_B, ignored -> landedOnB.countDown());
        IterableBackgroundInitializer.queueOrExecute(() -> ran.add("afterB@" + liveApiKey()), "afterB");

        assertTrue(awaitSwitch(landedOnB));
        settle();
        assertEquals(List.of("afterB@" + API_KEY_B), ran);
    }

    /**
     * A switchProject made before the SDK was initialized has no previous project, so a push open
     * made during it is queued for that initialization, even with another switch queued behind it.
     */
    @Test
    public void testAPushOpenDuringAPreInitializationSwitchIsQueuedForThatInitialization() throws Exception {
        List<String> ran = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch landedOnA = new CountDownLatch(1);
        CountDownLatch landedOnB = new CountDownLatch(1);

        switchTo(API_KEY_A, ignored -> landedOnA.countDown());
        switchTo(API_KEY_B, ignored -> landedOnB.countDown());
        assertTrue("a push open has to wait for the initialization",
                IterableBackgroundInitializer.queueOrExecuteUnlessSwitching(new IterableBackgroundInitializer.QueuedOperation() {
                    @Override
                    public void execute() {
                        ran.add("pushOpen@" + liveApiKey());
                    }

                    @Override
                    public String getDescription() {
                        return "pushOpen";
                    }
                }));

        assertTrue(awaitSwitch(landedOnA));
        assertTrue(awaitSwitch(landedOnB));
        settle();
        assertEquals(List.of("pushOpen@" + API_KEY_A), ran);
    }

    private void assertReIdentifyingFromTheFirstCallbackStaysOnA() throws Exception {
        AtomicReference<String> liveInFirstCallback = new AtomicReference<>();
        AtomicReference<IterableProjectSwitchResult> firstResult = new AtomicReference<>();
        CountDownLatch firstReported = new CountDownLatch(1);
        CountDownLatch landedOnB = new CountDownLatch(1);

        switchTo(API_KEY_A, result -> {
            firstResult.set(result);
            liveInFirstCallback.set(liveApiKey());
            IterableApi.getInstance().setEmail(EMAIL_REIDENTIFIED);
            firstReported.countDown();
        });
        switchTo(API_KEY_B, ignored -> landedOnB.countDown());

        assertTrue(awaitSwitch(firstReported));
        assertTrue(awaitSwitch(landedOnB));
        settle();
        assertEquals(IterableProjectSwitchResult.SWITCHED_CLEANLY, firstResult.get());
        assertEquals("the first callback has to run while A is still live", API_KEY_A, liveInFirstCallback.get());
        assertEquals(API_KEY_B, liveApiKey());
        assertNull("the user identified from A's callback must not land on B", IterableApi.getInstance().getEmail());
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
