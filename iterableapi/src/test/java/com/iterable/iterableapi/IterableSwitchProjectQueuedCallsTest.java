package com.iterable.iterableapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONObject;
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

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Calls made during a switch pass the gate once, whichever overload they go through.
 *
 * The gate stays raised while a switch drains its queue, so a queued call that went through the
 * gate a second time would be queued again at the tail: behind calls made after it, and under the
 * project asked for last rather than the one it was made for.
 */
@RunWith(TestRunner.class)
public class IterableSwitchProjectQueuedCallsTest extends BaseTest {

    private static final String API_KEY_A = "project-a-key";
    private static final String API_KEY_B = "project-b-key";
    private static final String API_KEY_C = "project-c-key";
    private static final String EMAIL_A = "user-a@example.com";
    private static final String EMAIL_B = "user-b@example.com";
    private static final String USER_ID_B = "user-id-b";

    private Context context;
    private MockWebServer server;
    private IterablePushRegistration.IterablePushRegistrationImpl originalPushRegistration;

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
        originalPushRegistration = IterablePushRegistration.instance;
        IterableTestUtils.resetIterableApi();
    }

    @After
    public void tearDown() throws Exception {
        settle();
        IterablePushRegistration.instance = originalPushRegistration;
        IterableTestUtils.resetIterableApi();
        IterableRequestTask.overrideUrl = null;
        server.shutdown();
    }

    /** A 4-argument track queued after a short setEmail must not run before the identity is set. */
    @Test
    public void testIdentityQueuedFirstIsSetBeforeALongOverloadQueuedAfterIt() throws Exception {
        queueDuringHeldSwitch(() -> {
            IterableApi.getInstance().setEmail(EMAIL_B);
            IterableApi.getInstance().track("afterIdentity", 0, 0, null);
        });

        List<RecordedRequest> tracks = takeTrackRequests(1);
        assertEquals("The track must be sent, not dropped for running before the identity", 1, tracks.size());
        JSONObject body = new JSONObject(tracks.get(0).getBody().readUtf8());
        assertEquals(API_KEY_B, tracks.get(0).getHeader(IterableConstants.HEADER_API_KEY));
        assertEquals("The track must carry the identity queued before it", EMAIL_B, body.getString(IterableConstants.KEY_EMAIL));
    }

    @Test
    public void testShortSetUserIdIsSetBeforeALongOverloadQueuedAfterIt() throws Exception {
        queueDuringHeldSwitch(() -> {
            IterableApi.getInstance().setUserId(USER_ID_B);
            IterableApi.getInstance().track("afterUserId", 0, 0, null);
        });

        List<RecordedRequest> tracks = takeTrackRequests(1);
        assertEquals("The track must be sent, not dropped for running before the identity", 1, tracks.size());
        JSONObject body = new JSONObject(tracks.get(0).getBody().readUtf8());
        assertEquals("The track must carry the identity queued before it", USER_ID_B, body.getString(IterableConstants.KEY_USER_ID));
    }

    @Test
    public void testShortUpdateEmailReplaysBeforeALongOverloadQueuedAfterIt() throws Exception {
        queueDuringHeldSwitch(() -> {
            IterableApi.getInstance().setEmail(EMAIL_B);
            IterableApi.getInstance().updateEmail("updated-" + EMAIL_B);
            IterableApi.getInstance().track("afterUpdateEmail", 0, 0, null);
        });

        assertEquals("Calls replay in the order they were made",
                List.of("/" + IterableConstants.ENDPOINT_UPDATE_EMAIL, "/" + IterableConstants.ENDPOINT_TRACK),
                takeRequestPaths(2, IterableConstants.ENDPOINT_UPDATE_EMAIL, IterableConstants.ENDPOINT_TRACK));
    }

    @Test
    public void testShortTrackPurchaseReplaysBeforeALongOverloadQueuedAfterIt() throws Exception {
        queueDuringHeldSwitch(() -> {
            IterableApi.getInstance().setEmail(EMAIL_B);
            IterableApi.getInstance().trackPurchase(10.0, List.of(new CommerceItem("sku-1", "Item", 10.0, 1)));
            IterableApi.getInstance().track("afterPurchase", 0, 0, null);
        });

        assertEquals("Calls replay in the order they were made",
                List.of("/" + IterableConstants.ENDPOINT_TRACK_PURCHASE, "/" + IterableConstants.ENDPOINT_TRACK),
                takeRequestPaths(2, IterableConstants.ENDPOINT_TRACK_PURCHASE, IterableConstants.ENDPOINT_TRACK));
    }

    @Test
    public void testMixedOverloadDepthsReplayInTheOrderTheyWereMade() throws Exception {
        IterableApi.initialize(context, API_KEY_A, configWithoutAuth());
        IterableApi.getInstance().setEmail(EMAIL_A);
        drainMainThread();
        while (server.takeRequest(50, TimeUnit.MILLISECONDS) != null) { /* drain setup traffic */ }

        // Raised directly so the window is deterministic. Nothing is torn down, so the identity
        // stays and both tracks can be sent.
        assertEquals(IterableBackgroundInitializer.BeginSwitchOutcome.RUN,
                IterableBackgroundInitializer.beginProjectSwitch(API_KEY_B, configWithoutAuth(), null, API_KEY_A));
        IterableApi.getInstance().track("first");
        IterableApi.getInstance().track("second", 0, 0, null);

        IterableApi.initialize(context, API_KEY_B, configWithoutAuth());
        IterableBackgroundInitializer.completeProjectSwitch(true);
        settle();

        List<String> sent = new ArrayList<>();
        for (RecordedRequest request : takeTrackRequests(2)) {
            sent.add(new JSONObject(request.getBody().readUtf8()).getString(IterableConstants.KEY_EVENT_NAME));
        }
        assertEquals("Calls replay in the order they were made", List.of("first", "second"), sent);
    }

    /**
     * A to B in flight, setEmail, then C requested. The email was asked for before C, so it belongs
     * to B, and C starts with no identity.
     */
    @Test
    public void testACallMadeBeforeALaterDestinationWasRequestedLandsOnTheSwitchInFlight() throws Exception {
        IterableApi.initialize(context, API_KEY_A, configWithoutAuth());
        drainMainThread();
        List<String> identifiedOn = Collections.synchronizedList(new ArrayList<>());

        assertEquals(IterableBackgroundInitializer.BeginSwitchOutcome.RUN,
                IterableBackgroundInitializer.beginProjectSwitch(API_KEY_B, configWithoutAuth(), null, API_KEY_A));
        IterableApi.getInstance().setEmail(EMAIL_B, data -> identifiedOn.add(IterableApi.getInstance()._apiKey), null);
        assertEquals(IterableBackgroundInitializer.BeginSwitchOutcome.JOINED_OR_QUEUED,
                IterableBackgroundInitializer.beginProjectSwitch(API_KEY_C, configWithoutAuth(), null, API_KEY_A));

        IterableApi.initialize(context, API_KEY_B, configWithoutAuth());
        IterableBackgroundInitializer.completeProjectSwitch(true);
        settle();

        assertEquals("The identity must be set on the project it was requested for", List.of(API_KEY_B), identifiedOn);
        assertEquals(API_KEY_C, IterableApi.getInstance()._apiKey);
        assertNull("C must not receive an identity requested before it was asked for", IterableApi.getInstance().getEmail());
    }

    /**
     * With unknown user activation on, updateUser without an identified user goes to the unknown
     * user manager, which the teardown has released. It must be queued for the new project rather
     * than dereference the released manager on the caller's thread.
     */
    @Test
    public void testUpdateUserDuringTheTeardownIsQueuedForTheNewProject() throws Exception {
        IterableConfig unknownUserConfig = new IterableConfig.Builder()
                .setKeychainEncryption(false)
                .setEnableUnknownUserActivation(true)
                .build();
        SwitchTeardownHold hold = SwitchTeardownHold.install();
        IterableApi.initialize(context, API_KEY_A, unknownUserConfig);
        IterableApi.getInstance().setVisitorUsageTracked(true);
        IterableApi.getInstance().setEmail(EMAIL_A);
        drainMainThread();

        CountDownLatch landed = new CountDownLatch(1);
        IterableApi.switchProject(context, new IterableProject(API_KEY_B, unknownUserConfig), ignored -> landed.countDown());
        hold.awaitTeardown();
        assertNull("Precondition: the teardown has cleared the identity", IterableApi.getInstance().getEmail());

        IterableApi.getInstance().updateUser(new JSONObject().put("favoriteColor", "blue"));
        assertTrue("updateUser must be queued while the switch is in flight",
                IterableBackgroundInitializer.getQueuedOperationDescriptions().contains("updateUser(dataFields)"));

        hold.release();
        assertTrue("Callback should fire", awaitSwitch(landed));
        String storedUpdate = context.getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                .getString(IterableConstants.SHARED_PREFS_USER_UPDATE_OBJECT_KEY, "");
        assertTrue("The update must be replayed into the new project's unknown user manager",
                storedUpdate.contains("favoriteColor"));
    }

    // region Helpers

    private IterableConfig configWithoutAuth() {
        return new IterableConfig.Builder()
                .setAutoPushRegistration(false)
                .setKeychainEncryption(false)
                .build();
    }

    private IterableConfig configWithAutoPushRegistration() {
        return new IterableConfig.Builder()
                .setKeychainEncryption(false)
                .build();
    }

    /**
     * Makes {@code calls} while a real switch from A to B is held in its teardown, so every one of
     * them is queued for B, then lets the switch land.
     */
    private void queueDuringHeldSwitch(Runnable calls) throws Exception {
        SwitchTeardownHold hold = SwitchTeardownHold.install();
        IterableApi.initialize(context, API_KEY_A, configWithAutoPushRegistration());
        IterableApi.getInstance().setEmail(EMAIL_A);
        drainMainThread();
        while (server.takeRequest(50, TimeUnit.MILLISECONDS) != null) { /* drain setup traffic */ }

        CountDownLatch landed = new CountDownLatch(1);
        IterableApi.switchProject(context, new IterableProject(API_KEY_B, configWithAutoPushRegistration()),
                ignored -> landed.countDown());
        calls.run();
        hold.awaitTeardown();
        hold.release();
        assertTrue("Callback should fire", awaitSwitch(landed));
    }

    /** Returns the paths of requests to {@code endpoints}, in arrival order, up to {@code expected}. */
    private List<String> takeRequestPaths(int expected, String... endpoints) throws InterruptedException {
        List<String> wanted = new ArrayList<>();
        for (String endpoint : endpoints) {
            wanted.add("/" + endpoint);
        }
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < 30 && paths.size() < expected; i++) {
            drainMainThread();
            RecordedRequest recorded = server.takeRequest(100, TimeUnit.MILLISECONDS);
            if (recorded != null && wanted.contains(recorded.getPath())) {
                paths.add(recorded.getPath());
            }
        }
        return paths;
    }

    /** Returns the events/track requests that reach the server, in arrival order, up to {@code expected}. */
    private List<RecordedRequest> takeTrackRequests(int expected) throws InterruptedException {
        List<RecordedRequest> tracks = new ArrayList<>();
        for (int i = 0; i < 30 && tracks.size() < expected; i++) {
            drainMainThread();
            RecordedRequest recorded = server.takeRequest(100, TimeUnit.MILLISECONDS);
            if (recorded != null && ("/" + IterableConstants.ENDPOINT_TRACK).equals(recorded.getPath())) {
                tracks.add(recorded);
            }
        }
        return tracks;
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
        assertFalse("The chain must be empty once every request has run",
                IterableBackgroundInitializer.isSwitchingProject());
    }

    // endregion
}
