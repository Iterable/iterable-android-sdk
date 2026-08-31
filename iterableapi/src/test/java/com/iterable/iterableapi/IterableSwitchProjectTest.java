package com.iterable.iterableapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Covers {@link IterableApi#switchProject(Context, IterableProject, IterableProjectSwitchCallback)}:
 * the guard cases, the teardown steps, the switch window, and the callback contract.
 */
@RunWith(TestRunner.class)
public class IterableSwitchProjectTest extends BaseTest {

    private static final String API_KEY_A = "project-a-key";
    private static final String API_KEY_B = "project-b-key";
    private static final String EMAIL_A = "user-a@example.com";
    private static final String EMAIL_B = "user-b@example.com";
    private static final String PROJECT_A_EVENT = "projectAOnlyEvent";

    private Context context;
    private MockWebServer server;
    private IterablePushRegistrationTask.Util.UtilImpl originalPushRegistrationUtil;
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
        originalPushRegistrationUtil = IterablePushRegistrationTask.Util.instance;
        originalPushRegistration = IterablePushRegistration.instance;
        IterableTestUtils.resetIterableApi();
    }

    @After
    public void tearDown() throws Exception {
        IterablePushRegistrationTask.Util.instance = originalPushRegistrationUtil;
        IterablePushRegistration.instance = originalPushRegistration;
        // The switch gate and the background executor are process-wide statics. A switch still in
        // flight would re-initialize IterableApi.sharedInstance from another thread part-way through
        // the next test, so let it finish before resetting.
        for (int i = 0; i < 100 && IterableBackgroundInitializer.isSwitchingProject(); i++) {
            drainMainThread();
            Thread.sleep(20);
        }
        IterableTestUtils.resetIterableApi();
        IterableRequestTask.overrideUrl = null;
        server.shutdown();
        server = null;
    }

    // ========================================
    // Helpers
    // ========================================

    private IterableConfig configWithoutAuth() {
        return new IterableConfig.Builder()
                .setAutoPushRegistration(false)
                .setKeychainEncryption(false)
                .build();
    }

    /**
     * The public API takes a paired {@link IterableProject}. These tests are about switch behaviour
     * rather than the shape of the call, so they go through this instead of repeating the pairing at
     * every call site. The tests that are about the API surface itself call it directly.
     */
    private static void switchTo(Context context, String apiKey, IterableConfig config,
                                 IterableProjectSwitchCallback callback) {
        IterableApi.switchProject(context, new IterableProject(apiKey, config), callback);
    }

    /** Real-world default: auto push registration on, so logout actually disables the token. */
    private IterableConfig configWithAutoPushRegistration() {
        return new IterableConfig.Builder()
                .setKeychainEncryption(false)
                .build();
    }

    private void initializeProjectA() {
        IterableApi.initialize(context, API_KEY_A, configWithoutAuth());
        IterableApi.getInstance().setEmail(EMAIL_A);
        drainMainThread();
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

    private boolean firstForegroundHandled() throws Exception {
        Field field = IterableApi.class.getDeclaredField("_firstForegroundHandled");
        field.setAccessible(true);
        return (boolean) field.get(IterableApi.getInstance());
    }

    private String storedEmail() {
        IterableKeychain keychain = IterableApi.getInstance().getKeychain();
        return keychain != null ? keychain.getEmail() : null;
    }

    private String storedUserIdUnknown() {
        IterableKeychain keychain = IterableApi.getInstance().getKeychain();
        return keychain != null ? keychain.getUserIdUnknown() : null;
    }

    private void waitForQueueToDrain() throws InterruptedException {
        for (int i = 0; i < 100 && IterableBackgroundInitializer.getQueuedOperationCount() > 0; i++) {
            drainMainThread();
            Thread.sleep(20);
        }
        drainMainThread();
    }

    /**
     * Drains the mock server and returns the first request whose path starts with {@code endpoint},
     * or null if none arrives.
     */
    private RecordedRequest takeRequestFor(String endpoint) throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            drainMainThread();
            RecordedRequest recorded = server.takeRequest(100, TimeUnit.MILLISECONDS);
            if (recorded == null) {
                continue;
            }
            if (recorded.getPath() != null && recorded.getPath().startsWith("/" + endpoint)) {
                return recorded;
            }
        }
        return null;
    }

    // ========================================
    // Step 1: guards
    // ========================================

    @Test
    public void testSwitchBeforeInitializeBehavesAsInitialize() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> verdict = new AtomicReference<>();

        switchTo(context, API_KEY_B, configWithoutAuth(), clean -> {
            verdict.set(clean);
            latch.countDown();
        });

        assertTrue("Callback should fire", awaitSwitch(latch));
        assertEquals("The SDK should end up initialized with the requested key",
                API_KEY_B, IterableApi.getInstance()._apiKey);
        // initializeInBackground's callback fires even when initialization times out or throws, so
        // this path cannot honestly claim a clean teardown. It reports false, like every other path
        // with no confirmed device disable.
        assertEquals("An initialize dressed up as a switch reports false",
                Boolean.FALSE, verdict.get());
    }

    @Test
    public void testSwitchWithUnchangedApiKeyIsANoOp() throws Exception {
        initializeProjectA();

        IterableInAppManager inAppManagerBefore = IterableApi.getInstance().getInAppManagerOrNull();
        IterableEmbeddedManager embeddedManagerBefore = IterableApi.getInstance().getEmbeddedManagerOrNull();
        IterableAuthManager authManagerBefore = IterableApi.getInstance().getAuthManager();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean cleanTeardown = new AtomicBoolean(false);
        switchTo(context, API_KEY_A, configWithoutAuth(), clean -> {
            cleanTeardown.set(clean);
            latch.countDown();
        });

        assertTrue("Callback should fire", awaitSwitch(latch));
        assertTrue("A no-op switch reports a clean teardown", cleanTeardown.get());
        assertSame("In-app manager must not be reset", inAppManagerBefore, IterableApi.getInstance().getInAppManagerOrNull());
        assertSame("Embedded manager must not be reset", embeddedManagerBefore, IterableApi.getInstance().getEmbeddedManagerOrNull());
        assertSame("Auth manager must not be reset", authManagerBefore, IterableApi.getInstance().getAuthManager());
        assertEquals("Identity must be untouched", EMAIL_A, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testSwitchWhileASwitchIsInProgressQueuesTheCallbackInsteadOfTearingDownAgain() throws Exception {
        initializeProjectA();

        AtomicInteger callbackCount = new AtomicInteger(0);
        CountDownLatch bothCallbacks = new CountDownLatch(2);
        IterableProjectSwitchCallback first = ignored -> {
            callbackCount.incrementAndGet();
            bothCallbacks.countDown();
        };
        IterableProjectSwitchCallback second = ignored -> {
            callbackCount.incrementAndGet();
            bothCallbacks.countDown();
        };

        assertTrue("First caller owns the switch", IterableBackgroundInitializer.beginProjectSwitch(first));
        assertFalse("Second caller must not start a second teardown", IterableBackgroundInitializer.beginProjectSwitch(second));
        assertTrue("Gate should be up", IterableBackgroundInitializer.isSwitchingProject());

        IterableBackgroundInitializer.completeProjectSwitch(true);

        assertTrue("Both callbacks should fire", awaitSwitch(bothCallbacks));
        assertEquals("Every registered callback fires exactly once", 2, callbackCount.get());
        assertFalse("Gate should be down", IterableBackgroundInitializer.isSwitchingProject());
    }

    // ========================================
    // Teardown correctness
    // ========================================

    @Test
    public void testSwitchClearsIdentityInMemoryAndInTheKeychain() throws Exception {
        initializeProjectA();
        assertEquals(EMAIL_A, storedEmail());

        CountDownLatch latch = new CountDownLatch(1);
        switchTo(context, API_KEY_B, configWithoutAuth(), ignored -> latch.countDown());
        assertTrue("Callback should fire", awaitSwitch(latch));

        assertNull("Email must be cleared in memory", IterableApi.getInstance().getEmail());
        assertNull("UserId must be cleared in memory", IterableApi.getInstance().getUserId());
        assertNull("Auth token must be cleared in memory", IterableApi.getInstance().getAuthToken());
        assertNull("Unknown user id must be cleared in memory", IterableApi.getInstance()._userIdUnknown);
        assertNull("Email must be cleared in storage so re-init cannot repopulate it", storedEmail());
        assertNull("The stored unknown user id must be cleared too, since storeAuthData writes it",
                storedUserIdUnknown());
        assertEquals(API_KEY_B, IterableApi.getInstance()._apiKey);
    }

    @Test
    public void testRequestAfterSwitchCarriesTheNewProjectKeyAndNotTheOldIdentifier() throws Exception {
        initializeProjectA();
        // Drain everything project A queued so the assertions below only see post-switch traffic.
        while (server.takeRequest(50, TimeUnit.MILLISECONDS) != null) { /* drain */ }

        CountDownLatch latch = new CountDownLatch(1);
        switchTo(context, API_KEY_B, configWithoutAuth(), ignored -> latch.countDown());
        assertTrue("Callback should fire", awaitSwitch(latch));

        while (server.takeRequest(50, TimeUnit.MILLISECONDS) != null) { /* drain */ }

        IterableApi.getInstance().setEmail(EMAIL_B);
        drainMainThread();
        IterableApi.getInstance().track("postSwitchEvent");
        drainMainThread();

        RecordedRequest trackRequest = null;
        for (int i = 0; i < 20; i++) {
            RecordedRequest recorded = server.takeRequest(200, TimeUnit.MILLISECONDS);
            if (recorded == null) {
                break;
            }
            if (recorded.getPath() != null && recorded.getPath().startsWith("/" + IterableConstants.ENDPOINT_TRACK)) {
                trackRequest = recorded;
                break;
            }
        }

        assertNotNull("A track request should reach the server after the switch", trackRequest);
        assertEquals("Requests must carry the new project's key", API_KEY_B, trackRequest.getHeader(IterableConstants.HEADER_API_KEY));
        JSONObject body = new JSONObject(trackRequest.getBody().readUtf8());
        assertEquals("Requests must not carry the previous project's identifier", EMAIL_B, body.getString(IterableConstants.KEY_EMAIL));
    }

    @Test
    public void testManagersAreDistinctInstancesAndCarryNoPreviousProjectContent() throws Exception {
        IterableInAppManager inAppManagerA = mock(IterableInAppManager.class);
        IterableEmbeddedManager embeddedManagerA = mock(IterableEmbeddedManager.class);
        IterableApi.sharedInstance = new IterableApi(inAppManagerA, embeddedManagerA);

        initializeProjectA();
        UnknownUserManager unknownUserManagerA = IterableApi.getInstance().unknownUserManager;
        IterableAuthManager authManagerA = IterableApi.getInstance().getAuthManager();
        // setEmail during setup already went through logout; only count what the switch does.
        clearInvocations(inAppManagerA, embeddedManagerA);

        CountDownLatch latch = new CountDownLatch(1);
        switchTo(context, API_KEY_B, configWithoutAuth(), ignored -> latch.countDown());
        assertTrue("Callback should fire", awaitSwitch(latch));

        // The existing logout path is what clears the previous project's cached content.
        verify(inAppManagerA).reset();
        verify(embeddedManagerA).reset();

        assertNotSame("In-app manager must be rebuilt", inAppManagerA, IterableApi.getInstance().getInAppManagerOrNull());
        assertNotSame("Embedded manager must be rebuilt", embeddedManagerA, IterableApi.getInstance().getEmbeddedManagerOrNull());
        assertNotSame("Unknown user manager must be rebuilt", unknownUserManagerA, IterableApi.getInstance().unknownUserManager);
        assertNotSame("Auth manager must be rebuilt", authManagerA, IterableApi.getInstance().getAuthManager());

        assertTrue("In-app messages must not carry over from the previous project",
                IterableApi.getInstance().getInAppManager().getMessages().isEmpty());
        List<IterableEmbeddedMessage> embeddedMessages = IterableApi.getInstance().getEmbeddedManager().getMessages(0L);
        assertTrue("Embedded messages must not carry over from the previous project",
                embeddedMessages == null || embeddedMessages.isEmpty());
    }

    @Test
    public void testAuthManagerIsRebuiltWithTheNewAuthHandler() throws Exception {
        AtomicInteger handlerACalls = new AtomicInteger(0);
        CountDownLatch handlerBCalled = new CountDownLatch(1);
        IterableAuthHandler handlerA = new CountingAuthHandler("token-a", handlerACalls, null);
        IterableAuthHandler handlerB = new CountingAuthHandler("token-b", new AtomicInteger(0), handlerBCalled);

        IterableApi.initialize(context, API_KEY_A, new IterableConfig.Builder()
                .setAutoPushRegistration(false)
                .setKeychainEncryption(false)
                .setAuthHandler(handlerA)
                .build());
        drainMainThread();

        CountDownLatch latch = new CountDownLatch(1);
        switchTo(context, API_KEY_B, new IterableConfig.Builder()
                .setAutoPushRegistration(false)
                .setKeychainEncryption(false)
                .setAuthHandler(handlerB)
                .build(), ignored -> latch.countDown());
        assertTrue("Callback should fire", awaitSwitch(latch));

        assertSame("The new config's handler must be the live one", handlerB, IterableApi.getInstance().config.authHandler);

        int callsToHandlerABeforeRequest = handlerACalls.get();
        IterableApi.getInstance().getAuthManager().requestNewAuthToken(false, null);
        assertTrue("The new project's auth handler must be the one invoked", handlerBCalled.await(5, TimeUnit.SECONDS));
        assertEquals("The previous project's auth handler must not be invoked again",
                callsToHandlerABeforeRequest, handlerACalls.get());
    }

    /** {@link IterableAuthHandler} is not a functional interface, so tests need a small stub. */
    private static class CountingAuthHandler implements IterableAuthHandler {
        private final String token;
        private final AtomicInteger calls;
        private final CountDownLatch called;

        CountingAuthHandler(String token, AtomicInteger calls, CountDownLatch called) {
            this.token = token;
            this.calls = calls;
            this.called = called;
        }

        @Override
        public String onAuthTokenRequested() {
            calls.incrementAndGet();
            if (called != null) {
                called.countDown();
            }
            return token;
        }

        @Override
        public void onTokenRegistrationSuccessful(String authToken) { }

        @Override
        public void onAuthFailure(AuthFailure authFailure) { }
    }

    @Test
    public void testFirstForegroundFlagIsResetSoTheNewProjectFetchesRemoteConfiguration() throws Exception {
        initializeProjectA();
        Field field = IterableApi.class.getDeclaredField("_firstForegroundHandled");
        field.setAccessible(true);
        field.set(IterableApi.getInstance(), true);
        assertTrue(firstForegroundHandled());

        CountDownLatch latch = new CountDownLatch(1);
        switchTo(context, API_KEY_B, configWithoutAuth(), ignored -> latch.countDown());
        assertTrue("Callback should fire", awaitSwitch(latch));

        assertFalse("The new project must get its own first-foreground remote config fetch", firstForegroundHandled());
    }

    @Test
    public void testDeviceIdIsProjectAgnosticAndSurvivesTheSwitch() throws Exception {
        initializeProjectA();
        // The device id is created lazily, so seed the value the SDK would have stored.
        String deviceIdBefore = "0f7c7ac8-project-agnostic-uuid";
        context.getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                .edit()
                .putString(IterableConstants.SHARED_PREFS_DEVICEID_KEY, deviceIdBefore)
                .apply();

        CountDownLatch latch = new CountDownLatch(1);
        switchTo(context, API_KEY_B, configWithoutAuth(), ignored -> latch.countDown());
        assertTrue("Callback should fire", awaitSwitch(latch));

        String deviceIdAfter = context
                .getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                .getString(IterableConstants.SHARED_PREFS_DEVICEID_KEY, null);
        assertEquals("_deviceId must be left alone", deviceIdBefore, deviceIdAfter);
    }

    @Test
    public void testOfflineQueuePurgeOnSwitchKeepsTheQueuedDeviceDisable() throws Exception {
        initializeProjectA();
        IterableApi.getInstance().apiClient.setOfflineProcessingEnabled(true);

        IterableTaskStorage taskStorage = IterableTaskStorage.sharedInstance(context);
        taskStorage.deleteAllTasks();

        IterableApiRequest trackRequest = new IterableApiRequest(API_KEY_A, IterableDataRegion.US.getEndpoint(), IterableConstants.ENDPOINT_TRACK, new JSONObject(), IterableApiRequest.POST, null, null, null);
        IterableApiRequest disableRequest = new IterableApiRequest(API_KEY_A, IterableDataRegion.US.getEndpoint(), IterableConstants.ENDPOINT_DISABLE_DEVICE, new JSONObject(), IterableApiRequest.POST, null, null, null);
        taskStorage.createTask(IterableConstants.ENDPOINT_TRACK, IterableTaskType.API, trackRequest.toJSONObject().toString());
        String disableTaskId = taskStorage.createTask(IterableConstants.ENDPOINT_DISABLE_DEVICE, IterableTaskType.API, disableRequest.toJSONObject().toString());

        CountDownLatch latch = new CountDownLatch(1);
        switchTo(context, API_KEY_B, configWithoutAuth(), ignored -> latch.countDown());
        assertTrue("Callback should fire", awaitSwitch(latch));

        List<String> remainingTaskIds = taskStorage.getAllTaskIds();
        assertEquals("Only the device disable survives the switch purge", 1, remainingTaskIds.size());
        assertEquals(disableTaskId, remainingTaskIds.get(0));

        IterableTask preserved = taskStorage.getTask(disableTaskId);
        assertNotNull(preserved);
        IterableApiRequest rehydrated = IterableApiRequest.fromJSON(new JSONObject(preserved.data), null, null);
        assertNotNull(rehydrated);
        assertEquals("The preserved disable must still reach the project it was created for", API_KEY_A, rehydrated.apiKey);
        assertEquals("The preserved disable must still reach the region it was created for",
                IterableDataRegion.US.getEndpoint(), rehydrated.baseUrl);

        taskStorage.deleteAllTasks();
        IterableApi.getInstance().apiClient.setOfflineProcessingEnabled(false);
    }

    // ========================================
    // Switch window
    // ========================================

    @Test
    public void testCallsDuringTheSwitchWindowAreQueuedAndDrainedAgainstTheNewProject() {
        initializeProjectA();

        // Raise the gate exactly as switchProject does, so the window is deterministic.
        assertTrue(IterableBackgroundInitializer.beginProjectSwitch(null));

        IterableApi.getInstance().setEmail(EMAIL_B);
        IterableApi.getInstance().track("queuedDuringSwitch");

        List<String> descriptions = IterableBackgroundInitializer.getQueuedOperationDescriptions();
        assertEquals("Both calls must be queued in FIFO order", 2, descriptions.size());
        assertTrue("setEmail should be queued first", descriptions.get(0).startsWith("setEmail("));
        assertTrue("Queued descriptions must mask PII", descriptions.get(0).contains("u***"));
        assertFalse("Queued descriptions must not leak the raw email", descriptions.get(0).contains(EMAIL_B));
        assertEquals("track(queuedDuringSwitch)", descriptions.get(1));

        // Re-initialize on the new project, then drain, mirroring steps 7 and 8.
        IterableApi.initialize(context, API_KEY_B, configWithoutAuth());
        IterableBackgroundInitializer.completeProjectSwitch(true);

        for (int i = 0; i < 100 && IterableBackgroundInitializer.getQueuedOperationCount() > 0; i++) {
            drainMainThread();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        drainMainThread();

        assertEquals("The queue must drain after the gate is lowered", 0, IterableBackgroundInitializer.getQueuedOperationCount());
        assertEquals("Drained calls run against the new project", API_KEY_B, IterableApi.getInstance()._apiKey);
        assertEquals("The queued setEmail must have executed", EMAIL_B, IterableApi.getInstance().getEmail());
    }

    @Test
    public void testPublicMethodReturnsBeforeAnyTeardownHappens() throws Exception {
        initializeProjectA();

        CountDownLatch latch = new CountDownLatch(1);
        switchTo(context, API_KEY_B, configWithoutAuth(), ignored -> latch.countDown());

        // Asserting on state rather than on wall clock: what matters is that the caller's thread did
        // not run the teardown, and that is observable without timing.
        assertTrue("The gate must be raised synchronously so calls made after this point are queued",
                IterableBackgroundInitializer.isSwitchingProject());
        assertEquals("The API key must not have been swapped on the calling thread",
                API_KEY_A, IterableApi.getInstance()._apiKey);
        assertEquals("Identity must not have been cleared on the calling thread",
                EMAIL_A, IterableApi.getInstance().getEmail());

        assertTrue("Awaited so the switch does not land in a later test", awaitSwitch(latch));
    }

    // ========================================
    // Callback contract
    // ========================================

    @Test
    public void testCallbackIsDeliveredOnTheMainThread() throws Exception {
        initializeProjectA();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean onMainThread = new AtomicBoolean(false);
        switchTo(context, API_KEY_B, configWithoutAuth(), ignored -> {
            onMainThread.set(Looper.myLooper() == Looper.getMainLooper());
            latch.countDown();
        });

        assertTrue("Callback should fire", awaitSwitch(latch));
        assertTrue("The switch callback must be delivered on the main thread", onMainThread.get());
    }

    @Test
    public void testThrowingTeardownStepStillCompletesTheSwapAndReportsFalse() throws Exception {
        initializeProjectA();

        IterableApiClient throwingApiClient = mock(IterableApiClient.class);
        doThrow(new RuntimeException("logout blew up")).when(throwingApiClient).onLogout();
        IterableApi.getInstance().apiClient = throwingApiClient;

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean cleanTeardown = new AtomicBoolean(true);
        switchTo(context, API_KEY_B, configWithoutAuth(), clean -> {
            cleanTeardown.set(clean);
            latch.countDown();
        });

        assertTrue("Callback should fire", awaitSwitch(latch));
        assertFalse("A noisy teardown step must report false", cleanTeardown.get());
        assertEquals("The swap must still complete", API_KEY_B, IterableApi.getInstance()._apiKey);
        assertNull("Identity must still be cleared", IterableApi.getInstance().getEmail());
        verify(throwingApiClient).onLogout();
    }

    @Test
    public void testRapidSwitchesRunOneTeardownAndFireEveryCallback() throws Exception {
        // The first switch's teardown is parked inside inAppManager.reset() until the second
        // switchProject call has been made, so the second call is guaranteed to arrive while the
        // first switch is still running rather than racing it.
        IterableInAppManager parkedInAppManager = mock(IterableInAppManager.class);
        CountDownLatch secondSwitchIssued = new CountDownLatch(1);
        AtomicBoolean parkOnReset = new AtomicBoolean(false);
        doAnswer(invocation -> {
            if (parkOnReset.get()) {
                secondSwitchIssued.await(10, TimeUnit.SECONDS);
            }
            return null;
        }).when(parkedInAppManager).reset();
        IterableApi.sharedInstance = new IterableApi(parkedInAppManager, mock(IterableEmbeddedManager.class));

        initializeProjectA();
        parkOnReset.set(true);

        CountDownLatch bothCallbacks = new CountDownLatch(2);
        switchTo(context, API_KEY_B, configWithoutAuth(), ignored -> bothCallbacks.countDown());
        switchTo(context, "project-c-key", configWithoutAuth(), ignored -> bothCallbacks.countDown());
        secondSwitchIssued.countDown();

        assertTrue("Both callbacks should fire", awaitSwitch(bothCallbacks));
        assertEquals("Only the first switch tears down; the second only registers its callback",
                API_KEY_B, IterableApi.getInstance()._apiKey);
        assertFalse("No switch should still be in progress", IterableBackgroundInitializer.isSwitchingProject());
    }

    @Test
    public void testSwitchingAToBToALeavesNoResidueFromTheIntermediateProject() throws Exception {
        initializeProjectA();

        CountDownLatch toB = new CountDownLatch(1);
        switchTo(context, API_KEY_B, configWithoutAuth(), ignored -> toB.countDown());
        assertTrue("Switch to B should complete", awaitSwitch(toB));

        IterableApi.getInstance().setEmail(EMAIL_B);
        drainMainThread();
        IterableInAppManager inAppManagerB = IterableApi.getInstance().getInAppManagerOrNull();
        IterableAuthManager authManagerB = IterableApi.getInstance().getAuthManager();

        CountDownLatch backToA = new CountDownLatch(1);
        switchTo(context, API_KEY_A, configWithoutAuth(), ignored -> backToA.countDown());
        assertTrue("Switch back to A should complete", awaitSwitch(backToA));

        assertEquals(API_KEY_A, IterableApi.getInstance()._apiKey);
        assertNull("Project B's identity must not survive", IterableApi.getInstance().getEmail());
        assertNull("Project B's identity must not survive in storage", storedEmail());
        assertNotSame("Project B's in-app manager must not survive", inAppManagerB, IterableApi.getInstance().getInAppManagerOrNull());
        assertNotSame("Project B's auth manager must not survive", authManagerB, IterableApi.getInstance().getAuthManager());
        assertTrue("No project B in-app content may survive", IterableApi.getInstance().getInAppManager().getMessages().isEmpty());
    }

    @Test
    public void testSwitchWithNullProjectThrowsWithoutTearingDown() {
        initializeProjectA();
        IterableInAppManager inAppManagerBefore = IterableApi.getInstance().getInAppManagerOrNull();

        // project is @NonNull, so null is a programmer error. Reporting it through the callback would
        // mean the same false that means "switched, but noisily" also means "nothing happened".
        assertThrows(IllegalArgumentException.class,
                () -> IterableApi.switchProject(context, null, ignored -> { }));

        assertEquals("The live project must not change", API_KEY_A, IterableApi.getInstance()._apiKey);
        assertSame("Nothing may be torn down", inAppManagerBefore, IterableApi.getInstance().getInAppManagerOrNull());
        assertEquals(EMAIL_A, IterableApi.getInstance().getEmail());
        assertFalse("The gate must not be left raised", IterableBackgroundInitializer.isSwitchingProject());
    }

    @Test
    public void testSwitchWithNullContextThrowsWithoutTearingDown() {
        initializeProjectA();

        assertThrows(IllegalArgumentException.class,
                () -> IterableApi.switchProject(null, new IterableProject(API_KEY_B, configWithoutAuth()), ignored -> { }));

        assertEquals(API_KEY_A, IterableApi.getInstance()._apiKey);
        assertFalse("The gate must not be left raised", IterableBackgroundInitializer.isSwitchingProject());
    }

    /**
     * A blank key is what a failed region lookup or a missing remote config entry produces. It cannot
     * reach the switch any more, because {@link IterableProject} refuses to hold one, which is the
     * point of pairing the key with its config: the unusable state is not constructible.
     */
    @Test
    public void testBlankApiKeyCannotBeMadeIntoAProject() {
        initializeProjectA();
        IterableInAppManager inAppManagerBefore = IterableApi.getInstance().getInAppManagerOrNull();

        for (String blankKey : new String[] {"", "   ", null}) {
            assertThrows("A blank key must not be constructible: " + blankKey,
                    IllegalArgumentException.class,
                    () -> new IterableProject(blankKey, configWithoutAuth()));
        }
        assertThrows("A null config must not be constructible", IllegalArgumentException.class,
                () -> new IterableProject(API_KEY_B, null));

        assertEquals("The live project must not change", API_KEY_A, IterableApi.getInstance()._apiKey);
        assertEquals("Identity must survive", EMAIL_A, IterableApi.getInstance().getEmail());
        assertSame("Nothing may be torn down", inAppManagerBefore,
                IterableApi.getInstance().getInAppManagerOrNull());
        assertFalse("The gate must not be left raised",
                IterableBackgroundInitializer.isSwitchingProject());
    }

    /**
     * The switcher keeps its own blank-key guard even though IterableProject makes it unreachable from
     * the public API, because the internal entry point is also used by initialize-time paths and by
     * Kotlin callers whose platform types can carry a null through.
     */
    @Test
    public void testInternalSwitcherStillRefusesABlankKey() throws Exception {
        initializeProjectA();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> verdict = new AtomicReference<>();
        IterableProjectSwitcher.switchProject(context, "  ", configWithoutAuth(), clean -> {
            verdict.set(clean);
            latch.countDown();
        });

        assertTrue("Callback should fire for a blank key", awaitSwitch(latch));
        assertEquals("A blank key must report false", Boolean.FALSE, verdict.get());
        assertEquals("The live project must not change", API_KEY_A, IterableApi.getInstance()._apiKey);
        assertFalse("The gate must not be left raised",
                IterableBackgroundInitializer.isSwitchingProject());
    }

    @Test
    public void testProjectExposesItsPairAndMasksTheKeyWhenLogged() {
        IterableConfig config = configWithoutAuth();
        IterableProject project = new IterableProject(API_KEY_B, config);

        assertEquals(API_KEY_B, project.getApiKey());
        assertSame("The config must be the one it was built with", config, project.getConfig());
        assertFalse("toString must not leak the key", project.toString().contains(API_KEY_B));
    }

    // ========================================
    // The previous project's device disable (B1)
    // ========================================

    /**
     * The FCM token lookup that the disable waits on is network-bound, so the live API key can change
     * underneath it. The key has to be the one captured when the disable was initiated.
     */
    @Test
    public void testDisableCarriesTheKeyCapturedWhenItWasInitiatedNotTheLiveOne() throws Exception {
        IterableApi.initialize(context, API_KEY_A, configWithAutoPushRegistration());
        IterableApi.getInstance().setEmail(EMAIL_A);
        drainMainThread();
        while (server.takeRequest(50, TimeUnit.MILLISECONDS) != null) { /* drain setup traffic */ }

        // Stand in for the FCM round trip: the key is swapped while the lookup is in flight, exactly
        // as steps 4 to 7 of a switch would do.
        IterablePushRegistrationTask.Util.instance = new IterablePushRegistrationTask.Util.UtilImpl() {
            @Override
            String getFirebaseToken() {
                IterableApi.getInstance()._apiKey = API_KEY_B;
                return "device-token";
            }

            @Override
            String getSenderId(Context applicationContext) {
                return "12345";
            }
        };

        IterableApi.getInstance().disablePush();

        RecordedRequest disableRequest = takeRequestFor(IterableConstants.ENDPOINT_DISABLE_DEVICE);
        assertNotNull("A disableDevice request should reach the server", disableRequest);
        assertEquals("users/disableDevice is project-scoped, so it must carry the key that was live "
                        + "when the disable was initiated",
                API_KEY_A, disableRequest.getHeader(IterableConstants.HEADER_API_KEY));
    }

    /**
     * The disable also resolves its data region from the live config at send time, so the swap has to
     * wait for the hand-off rather than racing it.
     */
    @Test
    public void testSwitchSendsThePreviousProjectsDisableBeforeSwappingTheKey() throws Exception {
        IterableApi.initialize(context, API_KEY_A, configWithAutoPushRegistration());
        IterableApi.getInstance().setEmail(EMAIL_A);
        drainMainThread();
        while (server.takeRequest(50, TimeUnit.MILLISECONDS) != null) { /* drain setup traffic */ }

        // BaseTest replaces the AsyncTask executor with an inline one, so a disable would otherwise
        // run synchronously inside step 3 and the ordering would be inherent rather than tested.
        // Production runs it on the AsyncTask thread pool, so put it on a real thread here.
        IterablePushRegistration.instance = new IterablePushRegistration.IterablePushRegistrationImpl() {
            @Override
            void executePushRegistrationTask(IterablePushRegistrationData data) {
                new Thread(() -> new IterablePushRegistrationTask().doInBackground(data), "push-registration").start();
            }
        };

        AtomicReference<String> apiKeyWhenTokenResolved = new AtomicReference<>();
        IterablePushRegistrationTask.Util.instance = new IterablePushRegistrationTask.Util.UtilImpl() {
            @Override
            String getFirebaseToken() {
                // Stands in for the FCM round trip. Steps 4 to 7 are all local work that finishes in
                // single-digit milliseconds, so without the wait they complete inside this window.
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                apiKeyWhenTokenResolved.set(IterableApi.getInstance()._apiKey);
                return "device-token";
            }

            @Override
            String getSenderId(Context applicationContext) {
                return "12345";
            }
        };

        CountDownLatch latch = new CountDownLatch(1);
        switchTo(context, API_KEY_B, configWithAutoPushRegistration(), ignored -> latch.countDown());
        assertTrue("Callback should fire", awaitSwitch(latch));

        assertEquals("The key must not be swapped until the disable has been handed off",
                API_KEY_A, apiKeyWhenTokenResolved.get());

        RecordedRequest disableRequest = takeRequestFor(IterableConstants.ENDPOINT_DISABLE_DEVICE);
        assertNotNull("A disableDevice request should reach the server", disableRequest);
        assertEquals("The disable must reach the project being left",
                API_KEY_A, disableRequest.getHeader(IterableConstants.HEADER_API_KEY));
        assertEquals("The switch must still complete", API_KEY_B, IterableApi.getInstance()._apiKey);
    }

    /**
     * Matching iOS, which reports false when there was no token to disable. An app without push, or
     * one that has no device token yet, always lands here, so false has to be documented as normal.
     */
    @Test
    public void testSwitchReportsFalseWhenNoDeviceDisableCouldBeConfirmed() throws Exception {
        initializeProjectA();

        assertFalse("A switch with no device disable to send reports a noisy teardown",
                switchAndAwaitVerdict(API_KEY_B, configWithoutAuth()));
        assertEquals("The switch still completes", API_KEY_B, IterableApi.getInstance()._apiKey);
    }

    @Test
    public void testSwitchReportsTrueWhenTheDeviceDisableWasSent() throws Exception {
        IterableApi.initialize(context, API_KEY_A, configWithAutoPushRegistration());
        IterableApi.getInstance().setEmail(EMAIL_A);
        drainMainThread();
        stubFirebaseToken("device-token");

        assertTrue("A switch that disabled the previous project's token reports a clean teardown",
                switchAndAwaitVerdict(API_KEY_B, configWithAutoPushRegistration()));
    }

    @Test
    public void testSwitchReportsFalseWhenTheDeviceTokenIsUnavailable() throws Exception {
        IterableApi.initialize(context, API_KEY_A, configWithAutoPushRegistration());
        IterableApi.getInstance().setEmail(EMAIL_A);
        drainMainThread();
        stubFirebaseToken(null);

        assertFalse("A disable that could not be built reports a noisy teardown",
                switchAndAwaitVerdict(API_KEY_B, configWithAutoPushRegistration()));
        assertEquals("The switch still completes", API_KEY_B, IterableApi.getInstance()._apiKey);
    }

    private void stubFirebaseToken(@Nullable String token) {
        IterablePushRegistrationTask.Util.instance = new IterablePushRegistrationTask.Util.UtilImpl() {
            @Override
            String getFirebaseToken() {
                return token;
            }

            @Override
            String getSenderId(Context applicationContext) {
                return "12345";
            }
        };
    }

    /** @return the cleanTeardown the switch reported */
    private boolean switchAndAwaitVerdict(String apiKey, IterableConfig config) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> verdict = new AtomicReference<>();
        switchTo(context, apiKey, config, cleanTeardown -> {
            verdict.set(cleanTeardown);
            latch.countDown();
        });
        assertTrue("Callback should fire", awaitSwitch(latch));
        assertNotNull(verdict.get());
        return verdict.get();
    }

    // ========================================
    // Re-entrancy and initialization overlap (B3, B4)
    // ========================================

    /**
     * The drain task shuts its own executor down as its last act, so a switch started from inside a
     * switch callback lands in the window where the executor is being shut down. That must not jam
     * the operation queue or lose the callback.
     */
    @Test
    public void testSwitchFromInsideItsOwnCallbackStillCompletes() throws Exception {
        initializeProjectA();

        CountDownLatch secondSwitchDone = new CountDownLatch(1);
        CountDownLatch firstSwitchDone = new CountDownLatch(1);
        switchTo(context, API_KEY_B, configWithoutAuth(), ignored -> {
            firstSwitchDone.countDown();
            switchTo(context, "project-c-key", configWithoutAuth(), reentrantIgnored -> secondSwitchDone.countDown());
        });

        assertTrue("The first switch's callback should fire", awaitSwitch(firstSwitchDone));
        assertTrue("The re-entrant switch's callback should fire too", awaitSwitch(secondSwitchDone));
        assertEquals("project-c-key", IterableApi.getInstance()._apiKey);
        assertFalse("The gate must be down", IterableBackgroundInitializer.isSwitchingProject());

        // The queue must still work afterwards, which it does not if isProcessing was left stuck.
        IterableApi.getInstance().setEmail(EMAIL_B);
        drainMainThread();
        assertEquals("The SDK must still execute calls after a re-entrant switch",
                EMAIL_B, IterableApi.getInstance().getEmail());
        assertEquals("Nothing may be left stranded in the operation queue",
                0, IterableBackgroundInitializer.getQueuedOperationCount());
    }

    /**
     * The drain task shuts its own executor down as its last act, so a rejected drain is real. Left
     * unhandled it sets isProcessing true and never clears it, so the operation queue never drains
     * again for the life of the process and the callbacks polled out of it are lost.
     */
    @Test
    public void testRejectedQueueDrainRecoversInsteadOfJammingTheQueue() throws Exception {
        initializeProjectA();

        ExecutorService deadExecutor = Executors.newSingleThreadExecutor();
        deadExecutor.shutdownNow();

        IterableBackgroundInitializer.simulateInitializingState();
        IterableApi.getInstance().track("queuedBeforeTheRejection");
        assertEquals(1, IterableBackgroundInitializer.getQueuedOperationCount());

        assertEquals("A rejected drain must be retried on a fresh executor, not left to propagate",
                IterableBackgroundInitializer.DrainResult.STARTED,
                IterableBackgroundInitializer.processQueuedOperationsOn(deadExecutor));
        waitForQueueToDrain();
        assertEquals("The queue must actually drain", 0, IterableBackgroundInitializer.getQueuedOperationCount());

        // The real damage of the original bug: isProcessing left true forever, so nothing drains again.
        IterableApi.getInstance().track("queuedAfterTheRejection");
        assertEquals("A later drain must not be refused because isProcessing was left stuck",
                IterableBackgroundInitializer.DrainResult.STARTED,
                IterableBackgroundInitializer.processQueuedOperationsOn(Executors.newSingleThreadExecutor()));
        waitForQueueToDrain();
        assertEquals(0, IterableBackgroundInitializer.getQueuedOperationCount());
    }

    /**
     * Same window on the other side: a rejected teardown submission would leave the gate raised with
     * no callback ever firing, so every later setEmail, setUserId and track is queued forever.
     */
    @Test
    public void testRejectedTeardownSubmissionStillRunsTheSwitch() throws Exception {
        ExecutorService deadExecutor = Executors.newSingleThreadExecutor();
        deadExecutor.shutdownNow();

        CountDownLatch taskRan = new CountDownLatch(1);
        IterableBackgroundInitializer.executeOn(deadExecutor, taskRan::countDown);

        assertTrue("The teardown must be retried on a fresh executor rather than throwing out of "
                        + "switchProject or stranding the gate",
                taskRan.await(5, TimeUnit.SECONDS));
    }

    /**
     * initializeInBackground publishes _apiKey synchronously but finishes its init task later, and
     * that task marks initialization complete, which would lower a switch's gate mid-teardown.
     */
    @Test
    public void testSwitchIsDeferredWhileAnInitializationIsStillInFlight() throws Exception {
        IterableApi.initialize(context, API_KEY_A, configWithoutAuth());
        IterableApi.getInstance().setEmail(EMAIL_A);
        drainMainThread();

        IterableBackgroundInitializer.simulateInitializingState();

        CountDownLatch latch = new CountDownLatch(1);
        switchTo(context, API_KEY_B, configWithoutAuth(), ignored -> latch.countDown());

        assertFalse("A switch must not start a teardown while initialization is in flight",
                IterableBackgroundInitializer.isSwitchingProject());
        assertEquals("Nothing may be swapped yet", API_KEY_A, IterableApi.getInstance()._apiKey);
        assertEquals("Identity may not be torn down yet", EMAIL_A, IterableApi.getInstance().getEmail());

        IterableBackgroundInitializer.simulateInitializationComplete();

        assertTrue("The deferred switch's callback should fire", awaitSwitch(latch));
        assertEquals("The deferred switch must actually run", API_KEY_B, IterableApi.getInstance()._apiKey);
        assertNull("The deferred switch must tear down the previous identity",
                IterableApi.getInstance().getEmail());
    }

    /**
     * A switch makes initialization look in flight, so initializeInBackground parks its callback in
     * pendingCallbacks. Only completeProjectSwitch can drain those.
     */
    @Test
    public void testInitializationCallbackParkedDuringASwitchStillFires() throws Exception {
        initializeProjectA();

        assertTrue(IterableBackgroundInitializer.beginProjectSwitch(null));

        CountDownLatch parkedCallback = new CountDownLatch(1);
        IterableApi.initializeInBackground(context, API_KEY_B, configWithoutAuth(), parkedCallback::countDown);

        IterableApi.initialize(context, API_KEY_B, configWithoutAuth());
        IterableBackgroundInitializer.completeProjectSwitch(true);

        assertTrue("A callback parked during the switch window must not be dropped",
                awaitSwitch(parkedCallback));
    }

    // ========================================
    // Unsynchronised teardown state (B5)
    // ========================================

    /**
     * initialize() rebuilds the in-app, embedded and unknown-user managers but never the auth
     * manager. It has to be replaced explicitly, after config is swapped, or the next lazy build can
     * bind the new project's requests to the previous project's IterableAuthHandler.
     */
    @Test
    public void testAuthManagerIsRebuiltEagerlyByTheSwitch() throws Exception {
        initializeProjectA();
        assertNotNull(IterableApi.getInstance().getAuthManager());

        CountDownLatch latch = new CountDownLatch(1);
        switchTo(context, API_KEY_B, configWithoutAuth(), ignored -> latch.countDown());
        assertTrue("Callback should fire", awaitSwitch(latch));

        assertNotNull("The switch must leave a live auth manager built from the new config, not a "
                        + "null field for the next caller to fill in from whatever config it finds",
                IterableApi.getInstance().authManager);
    }

    /**
     * The SDK's own threads are not covered by the switch gate, so the lock the swap holds has to be
     * the same one getAuthManager() takes. Otherwise NetworkThread can build an auth manager from a
     * config that is halfway through being replaced.
     */
    @Test
    public void testGetAuthManagerWaitsForTheProjectStateLock() throws Exception {
        initializeProjectA();
        IterableApi api = IterableApi.getInstance();
        api.authManager = null;

        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        CountDownLatch managerObtained = new CountDownLatch(1);

        Thread holder = new Thread(() -> {
            synchronized (api.projectStateLock) {
                lockHeld.countDown();
                try {
                    releaseLock.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "lock-holder");
        holder.start();
        assertTrue(lockHeld.await(5, TimeUnit.SECONDS));

        Thread reader = new Thread(() -> {
            api.getAuthManager();
            managerObtained.countDown();
        }, "auth-manager-reader");
        reader.start();

        assertFalse("getAuthManager must not build an auth manager while the swap holds the lock",
                managerObtained.await(300, TimeUnit.MILLISECONDS));

        releaseLock.countDown();
        assertTrue("getAuthManager must proceed once the lock is released",
                managerObtained.await(5, TimeUnit.SECONDS));
        holder.join(5000);
        reader.join(5000);
    }

    /**
     * The teardown runs on the background executor while the SDK's own threads keep reading these
     * fields without taking any lock. Non-volatile writes give those threads no happens-before edge,
     * so they can keep seeing the previous project's state indefinitely.
     */
    @Test
    public void testProjectScopedFieldsAreVolatile() throws Exception {
        for (String fieldName : new String[]{"config", "_apiKey", "_email", "_userId", "_userIdUnknown",
                "_authToken", "inAppManager", "embeddedManager", "unknownUserManager", "authManager",
                "keychain", "_firstForegroundHandled"}) {
            Field field = IterableApi.class.getDeclaredField(fieldName);
            assertTrue(fieldName + " is replaced by switchProject from the background executor and read "
                    + "by ungated SDK threads, so it must be volatile",
                    Modifier.isVolatile(field.getModifiers()));
        }
    }

    // ========================================
    // Project-scoped storage (B6)
    // ========================================

    /**
     * setEmail on the new project runs the unknown-user event replay, which would post events
     * collected under the previous project into the new one.
     */
    @Test
    public void testEventsCollectedUnderThePreviousProjectAreNotReplayedIntoTheNewOne() throws Exception {
        IterableConfig unknownUserConfig = new IterableConfig.Builder()
                .setAutoPushRegistration(false)
                .setKeychainEncryption(false)
                .setEnableUnknownUserActivation(true)
                .build();
        IterableApi.initialize(context, API_KEY_A, unknownUserConfig);
        IterableApi.getInstance().setVisitorUsageTracked(true);
        drainMainThread();

        // Seeded directly: setVisitorUsageTracked clears the list, and this is exactly the shape
        // trackUnknownEvent writes.
        JSONObject event = new JSONObject();
        event.put(IterableConstants.KEY_EVENT_NAME, PROJECT_A_EVENT);
        event.put(IterableConstants.KEY_CREATED_AT, System.currentTimeMillis());
        event.put(IterableConstants.SHARED_PREFS_EVENT_TYPE, IterableConstants.TRACK_EVENT);
        JSONArray eventList = new JSONArray();
        eventList.put(event);
        context.getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                .edit()
                .putString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, eventList.toString())
                .apply();

        CountDownLatch latch = new CountDownLatch(1);
        switchTo(context, API_KEY_B, unknownUserConfig, ignored -> latch.countDown());
        assertTrue("Callback should fire", awaitSwitch(latch));

        while (server.takeRequest(50, TimeUnit.MILLISECONDS) != null) { /* drain switch traffic */ }

        IterableApi.getInstance().setEmail(EMAIL_B);
        drainMainThread();

        for (int i = 0; i < 30; i++) {
            drainMainThread();
            RecordedRequest recorded = server.takeRequest(100, TimeUnit.MILLISECONDS);
            if (recorded == null) {
                continue;
            }
            String body = recorded.getBody().readUtf8();
            assertFalse("The previous project's event must not be replayed into the new project, "
                            + "path was " + recorded.getPath(),
                    body.contains(PROJECT_A_EVENT));
        }

        assertEquals("The previous project's event list must not survive the switch", "",
                context.getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                        .getString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, ""));
    }

    @Test
    public void testPreviousProjectsAttributionAndCriteriaAreCleared() throws Exception {
        initializeProjectA();

        IterableApi.getInstance().setAttributionInfo(new IterableAttributionInfo(1234, 5678, "message-a"));
        context.getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                .edit()
                .putString(IterableConstants.SHARED_PREFS_CRITERIA, "[{\"criteriaId\":1}]")
                .apply();
        assertNotNull(IterableApi.getInstance().getAttributionInfo());

        CountDownLatch latch = new CountDownLatch(1);
        switchTo(context, API_KEY_B, configWithoutAuth(), ignored -> latch.countDown());
        assertTrue("Callback should fire", awaitSwitch(latch));

        assertNull("campaignId and templateId are namespaced per project, so the previous project's "
                        + "attribution must not be attached to the first track after the switch",
                IterableApi.getInstance().getAttributionInfo());
        assertEquals("The previous project's activation criteria must not survive", "",
                context.getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                        .getString(IterableConstants.SHARED_PREFS_CRITERIA, ""));
    }

    @Test
    public void testKeychainIsRebuiltSoTheNewConfigsEncryptionSettingApplies() throws Exception {
        initializeProjectA();
        IterableKeychain keychainBefore = IterableApi.getInstance().getKeychain();
        assertNotNull(keychainBefore);

        CountDownLatch latch = new CountDownLatch(1);
        switchTo(context, API_KEY_B, new IterableConfig.Builder()
                .setAutoPushRegistration(false)
                .setKeychainEncryption(true)
                .build(), ignored -> latch.countDown());
        assertTrue("Callback should fire", awaitSwitch(latch));

        assertNotSame("The new config's keychainEncryption and decryptionFailureHandler are ignored "
                        + "for the rest of the process unless the keychain is rebuilt",
                keychainBefore, IterableApi.getInstance().getKeychain());
    }

    // ========================================
    // Gate coverage and switch bookkeeping
    // ========================================

    /**
     * The long setEmail and setUserId overloads are public, so an app can call them directly. If they
     * are not gated, that app bypasses the switch window entirely.
     */
    @Test
    public void testTheLongSetEmailAndSetUserIdOverloadsAreGated() {
        initializeProjectA();

        assertTrue(IterableBackgroundInitializer.beginProjectSwitch(null));

        IterableApi.getInstance().setEmail(EMAIL_B, null, null, null, null);
        IterableApi.getInstance().setUserId("user-b", null, null, null, null, false);

        assertEquals("Both long overloads must be queued behind the gate, not executed",
                2, IterableBackgroundInitializer.getQueuedOperationCount());
        assertEquals("The previous project's identity must be untouched while the gate is up",
                EMAIL_A, IterableApi.getInstance().getEmail());

        IterableBackgroundInitializer.completeProjectSwitch(true);
    }

    /**
     * beginProjectSwitch has to let initialize() notify again, but clearing the subscriber list as
     * well drops a subscriber registered while the first initialization was in flight.
     */
    @Test
    public void testRaisingTheSwitchGateKeepsInitializationSubscribers() throws Exception {
        // Registered before any initialize has completed, so it is genuinely still pending. That is
        // the ordinary case of an app subscribing during Application#onCreate.
        CountDownLatch subscriberCalled = new CountDownLatch(1);
        IterableApi.onSDKInitialized(subscriberCalled::countDown);

        assertTrue(IterableBackgroundInitializer.beginProjectSwitch(null));

        IterableApi.initialize(context, API_KEY_B, configWithoutAuth());
        IterableBackgroundInitializer.completeProjectSwitch(true);

        assertTrue("Raising the switch gate must not discard subscribers that are still waiting to be "
                        + "notified", awaitSwitch(subscriberCalled));
    }

    @Test
    public void testAuthTokenReadyListenersAreNotRegisteredTwice() {
        initializeProjectA();
        IterableAuthManager authManager = IterableApi.getInstance().getAuthManager();

        AtomicInteger notifications = new AtomicInteger(0);
        IterableAuthManager.AuthTokenReadyListener listener = notifications::incrementAndGet;
        authManager.addAuthTokenReadyListener(listener);
        authManager.addAuthTokenReadyListener(listener);

        // Drive the INVALID -> ready transition that notifies listeners.
        authManager.setAuthTokenInvalid();
        authManager.setIsLastAuthTokenValid(true);

        assertEquals("A listener registered twice must only be notified once, otherwise the task "
                        + "runner double-processes every auth recovery after a project switch",
                1, notifications.get());
    }

    @Test
    public void testLambdaCallbackReceivesTheTeardownVerdict() throws Exception {
        initializeProjectA();

        // IterableProjectSwitchCallback has to stay a single-method interface. If the verdict ever
        // moves onto a type whose only abstract method takes no arguments, a lambda binds to that
        // one instead and silently discards the boolean, which this stops compiling.
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> verdict = new AtomicReference<>();
        IterableProjectSwitchCallback lambda = clean -> {
            verdict.set(clean);
            latch.countDown();
        };
        switchTo(context, API_KEY_B, configWithoutAuth(), lambda);

        assertTrue("A lambda callback must be notified", awaitSwitch(latch));
        assertNotNull("A lambda callback must receive the verdict, not a discarded default",
                verdict.get());
    }

    // ========================================
    // Per-project state held on the shared instance
    // ========================================

    /**
     * iOS drops all of this when it replaces its SDK instance. Android reuses sharedInstance, so
     * anything held in a field survives unless the teardown clears it. inboxSessionId is the one that
     * produces cross-project data: it is attached to the new project's in-app tracking calls.
     */
    @Test
    public void testPerProjectInstanceStateDoesNotSurviveTheSwitch() throws Exception {
        initializeProjectA();

        IterableApi.getInstance().setInboxSessionId("session-from-project-a");
        IterableApi.getInstance().setDeviceAttribute("tenant", "project-a");
        assertNotNull("precondition: the session ID is set", readInboxSessionId());
        assertFalse("precondition: a device attribute is set",
                IterableApi.getInstance().getDeviceAttributes().isEmpty());

        CountDownLatch latch = new CountDownLatch(1);
        switchTo(context, API_KEY_B, configWithoutAuth(), ignored -> latch.countDown());
        assertTrue("Callback should fire", awaitSwitch(latch));

        assertNull("An inbox session from the previous project must not be sent to the new one",
                readInboxSessionId());
        assertTrue("Device attributes must not carry over, iOS discards them with the instance",
                IterableApi.getInstance().getDeviceAttributes().isEmpty());
        assertNull("The previous project's push payload must not survive",
                IterableApi.getInstance().getPayloadData());
    }

    /**
     * Every shorter overload of these two is queued, so the longest one has to be too. It was public
     * and ran inline, which meant mid-switch behaviour depended on which overload the app happened to
     * call. Same defect the branch already fixed for setEmail and setUserId.
     */
    @Test
    public void testLongestTrackAndUpdateEmailOverloadsAreQueuedLikeTheirShorterSiblings() throws Exception {
        initializeProjectA();

        assertTrue(IterableBackgroundInitializer.beginProjectSwitch(null));
        try {
            IterableApi.getInstance().track("event", 11, 22, new JSONObject());
            assertEquals("the longest track overload must not bypass the gate",
                    1, IterableBackgroundInitializer.getQueuedOperationCount());

            IterableApi.getInstance().updateEmail(EMAIL_B, null, null, null);
            assertEquals("the longest updateEmail overload must not bypass the gate",
                    2, IterableBackgroundInitializer.getQueuedOperationCount());
        } finally {
            IterableBackgroundInitializer.resetBackgroundInitializationState();
        }
    }

    /** No accessor for it, and adding one purely for a test would put test-only surface on the API. */
    private String readInboxSessionId() throws Exception {
        Field field = IterableApi.class.getDeclaredField("inboxSessionId");
        field.setAccessible(true);
        return (String) field.get(IterableApi.getInstance());
    }
}
