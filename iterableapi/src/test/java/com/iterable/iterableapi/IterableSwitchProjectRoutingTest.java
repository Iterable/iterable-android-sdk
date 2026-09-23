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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Which project the SDK ends on when switch requests overlap.
 *
 * Every case here turns on the same rule: a request is resolved against the project the app most
 * recently <i>asked</i> for, which for the whole length of a teardown is not the project that is
 * live. Resolving against the live project instead is wrong in three ways, one per test below.
 *
 * {@link IterableSwitchProjectTest} covers the switch sequence itself, and
 * {@link IterableSwitchProjectQueueDrainTest} covers the drain.
 */
@RunWith(TestRunner.class)
public class IterableSwitchProjectRoutingTest extends BaseTest {

    private static final String API_KEY_A = "project-a-key";
    private static final String API_KEY_B = "project-b-key";
    private static final String API_KEY_C = "project-c-key";
    private static final String API_KEY_D = "project-d-key";
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
     * Rapid A to B to A. While B is tearing down, {@code _apiKey} is still A, so resolving against
     * it calls the second request a no-op: it reports a clean switch to A, tears nothing down, and
     * B lands anyway. The app is then told it is on A while every event it sends goes to B, with
     * nothing in the API to notice it with.
     */
    @Test
    public void testAskingToGoBackToTheProjectBeingLeftIsARealSwitchNotANoOp() throws Exception {
        CountDownLatch landedOnB = new CountDownLatch(1);
        CountDownLatch backOnA = new CountDownLatch(1);

        switchTo(API_KEY_B, ignored -> landedOnB.countDown());
        switchTo(API_KEY_A, ignored -> backOnA.countDown());

        assertTrue("the switch to B must still report", awaitSwitch(landedOnB));
        assertTrue("the request to go back to A must report, not be silently absorbed", awaitSwitch(backOnA));
        settle();
        assertEquals("the SDK must end on the project the app asked for last",
                API_KEY_A, IterableApi.getInstance()._apiKey);
    }

    /** With nothing in flight, asking for the live project really is a no-op. */
    @Test
    public void testAskingForTheLiveProjectWithNothingInFlightIsANoOp() throws Exception {
        CountDownLatch reported = new CountDownLatch(1);
        switchTo(API_KEY_A, ignored -> reported.countDown());

        assertTrue("a no-op still has to report", awaitSwitch(reported));
        assertFalse("a no-op must not start a chain", IterableBackgroundInitializer.isSwitchingProject());
        assertEquals(API_KEY_A, IterableApi.getInstance()._apiKey);
    }

    /**
     * B in flight, then C, D, C. Merging the last C into the queued one leaves the chain as C then
     * D, so the SDK settles on D, and reports the last request when the first C lands, two
     * switches early.
     */
    @Test
    public void testARepeatIsNotMergedAcrossAnotherDestination() throws Exception {
        List<String> reportedOrder = new ArrayList<>();

        switchTo(API_KEY_B, ignored -> reportedOrder.add(API_KEY_B));
        switchTo(API_KEY_C, ignored -> reportedOrder.add("first-c"));
        switchTo(API_KEY_D, ignored -> reportedOrder.add(API_KEY_D));
        switchTo(API_KEY_C, ignored -> reportedOrder.add("last-c"));

        settle();
        assertEquals("every request runs in the order it was made",
                List.of(API_KEY_B, "first-c", API_KEY_D, "last-c"), reportedOrder);
        assertEquals("the SDK must end on the project the app asked for last",
                API_KEY_C, IterableApi.getInstance()._apiKey);
    }

    /** An adjacent repeat still shares one teardown, which is the double-tapped picker. */
    @Test
    public void testAnAdjacentRepeatOfAQueuedDestinationSharesItsSwitch() throws Exception {
        List<String> reportedOrder = new ArrayList<>();

        switchTo(API_KEY_B, ignored -> reportedOrder.add(API_KEY_B));
        switchTo(API_KEY_C, ignored -> reportedOrder.add("first-c"));
        switchTo(API_KEY_C, ignored -> reportedOrder.add("second-c"));

        settle();
        assertEquals("two taps on one queued destination report together from a single switch",
                List.of(API_KEY_B, "first-c", "second-c"), reportedOrder);
        assertEquals(API_KEY_C, IterableApi.getInstance()._apiKey);
    }

    /**
     * A repeat of the destination in flight can only join it while nothing is queued behind it.
     * With C already queued the app's most recent ask is B again, so joining B's callbacks would
     * fire them and then let C land, leaving the SDK on the project asked for second to last while
     * both callbacks reported success.
     */
    @Test
    public void testARepeatOfTheInFlightProjectCannotJoinItOnceSomethingIsQueuedBehindIt() throws Exception {
        List<String> reportedOrder = new ArrayList<>();

        switchTo(API_KEY_B, ignored -> reportedOrder.add("first-b"));
        switchTo(API_KEY_C, ignored -> reportedOrder.add(API_KEY_C));
        switchTo(API_KEY_B, ignored -> reportedOrder.add("last-b"));

        settle();
        assertEquals("the repeat of B has to run after the destination already queued behind it",
                List.of("first-b", API_KEY_C, "last-b"), reportedOrder);
        assertEquals("the SDK must end on the project the app asked for last",
                API_KEY_B, IterableApi.getInstance()._apiKey);
    }

    /**
     * The call-queueing gate has to drop at the handover even though the chain stays up. At that
     * moment the SDK is fully live on the project that just landed, and the contract tells apps to
     * re-identify from the callback, so a setEmail made there has to reach that project. Holding
     * the gate across the handover queues it and then replays it into the next switch, sending B's
     * identity to C.
     */
    @Test
    public void testIdentitySetFromACallbackReachesTheProjectThatJustLanded() throws Exception {
        CountDownLatch landedOnB = new CountDownLatch(1);
        List<String> emailWhileOnB = new ArrayList<>();

        switchTo(API_KEY_B, ignored -> {
            IterableApi.getInstance().setEmail(EMAIL_B);
            // Read back straight away: if the call had been queued behind C, the SDK would still
            // have no email here and C's drain would deliver it to the wrong project.
            emailWhileOnB.add(IterableApi.getInstance().getEmail());
            landedOnB.countDown();
        });
        switchTo(API_KEY_C, null);

        assertTrue(awaitSwitch(landedOnB));
        assertEquals("identity set from B's callback must apply to B immediately, not be queued into C",
                List.of(EMAIL_B), emailWhileOnB);

        settle();
        assertEquals(API_KEY_C, IterableApi.getInstance()._apiKey);
        assertEquals("C starts with no identity: B's email must not have been replayed into it",
                null, IterableApi.getInstance().getEmail());
    }

    // region Helpers

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

    /** Waits for a switch callback, pumping the main looper so the posted callback can run. */
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

    /**
     * Lets the whole chain unwind, so a test asserts where the SDK ends up rather than somewhere
     * it passes through on the way.
     */
    private void settle() throws InterruptedException {
        for (int i = 0; i < 200 && IterableBackgroundInitializer.isSwitchingProject(); i++) {
            drainMainThread();
            Thread.sleep(10);
        }
        drainMainThread();
        assertFalse("the chain must be empty once every request has run",
                IterableBackgroundInitializer.isSwitchingProject());
    }

    // endregion
}
