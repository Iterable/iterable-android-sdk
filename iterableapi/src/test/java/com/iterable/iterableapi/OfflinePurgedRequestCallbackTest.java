package com.iterable.iterableapi;

import android.content.Context;

import androidx.annotation.NonNull;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static android.os.Looper.getMainLooper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Covers what happens to a queued request's completion handlers when the request is deleted from the
 * offline queue before it can be sent.
 *
 * The handlers are parked in {@link TaskScheduler}'s static maps at schedule time and are only ever
 * settled from {@link IterableTaskRunner}, which never runs a deleted task. Now that
 * {@code users/registerDeviceToken} is queued, the handlers an app passes to {@code setEmail} and
 * {@code setUserId} travel into that queue, so a logout purge used to strand them.
 */
@RunWith(TestRunner.class)
public class OfflinePurgedRequestCallbackTest extends BaseTest {

    private static final String TEST_TOKEN = "testToken";

    private MockWebServer server;
    private IterableTaskStorage taskStorage;
    private IterableTaskRunner taskRunner;
    private TaskScheduler scheduler;
    private OfflineRequestProcessor processor;
    private IterableNetworkConnectivityManager mockNetworkConnectivityManager;
    private IterablePushRegistrationTask.Util.UtilImpl originalPushRegistrationUtil;

    @Before
    public void setUp() {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @NonNull
            @Override
            public MockResponse dispatch(@NonNull RecordedRequest request) {
                return new MockResponse().setResponseCode(200).setBody("{}");
            }
        });
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        originalPushRegistrationUtil = IterablePushRegistrationTask.Util.instance;
        IterablePushRegistrationTask.Util.UtilImpl pushRegistrationUtilMock =
                mock(IterablePushRegistrationTask.Util.UtilImpl.class);
        when(pushRegistrationUtilMock.getSenderId(any(Context.class))).thenReturn("12345");
        when(pushRegistrationUtilMock.getFirebaseToken()).thenReturn(TEST_TOKEN);
        IterablePushRegistrationTask.Util.instance = pushRegistrationUtilMock;

        taskStorage = IterableTaskStorage.sharedInstance(getContext());
        taskStorage.deleteAllTasks();
        // Static and shared with every other test in the JVM.
        TaskScheduler.successCallbackMap.clear();
        TaskScheduler.failureCallbackMap.clear();

        // A runner we can drive deterministically over the same storage the SDK writes to. Its
        // network thread is never idled except through drainTaskRunner(), so it cannot flush behind
        // an assertion's back.
        mockNetworkConnectivityManager = mock(IterableNetworkConnectivityManager.class);
        IterableActivityMonitor mockActivityMonitor = mock(IterableActivityMonitor.class);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        HealthMonitor mockHealthMonitor = mock(HealthMonitor.class);
        when(mockHealthMonitor.canProcess()).thenReturn(true);
        when(mockHealthMonitor.canSchedule()).thenReturn(true);
        taskRunner = new IterableTaskRunner(taskStorage, mockActivityMonitor,
                mockNetworkConnectivityManager, mockHealthMonitor, new ApiEndpointClassification());
        scheduler = new TaskScheduler(taskStorage, taskRunner);
        processor = new OfflineRequestProcessor(scheduler, taskRunner, taskStorage, mockHealthMonitor);
    }

    @After
    public void tearDown() throws Exception {
        // IterableTaskStorage is a singleton that outlives the test, so a runner left registered
        // here would keep flushing tasks created by the next test.
        taskStorage.removeDatabaseStatusListener(taskRunner);
        taskStorage.deleteAllTasks();
        TaskScheduler.successCallbackMap.clear();
        TaskScheduler.failureCallbackMap.clear();
        IterableApi.getInstance().apiClient.setOfflineProcessingEnabled(false);
        IterablePushRegistrationTask.Util.instance = originalPushRegistrationUtil;
        server.shutdown();
        server = null;
    }

    @Test
    public void testPurgedRegisterFailsTheAppsHandlerAndLeavesNothingParked() {
        AtomicInteger successCalls = new AtomicInteger();
        AtomicReference<String> failureReason = new AtomicReference<>();
        AtomicReference<JSONObject> failureData = new AtomicReference<>(new JSONObject());

        String registerTaskId = schedule(IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN,
                data -> successCalls.incrementAndGet(),
                (reason, data) -> {
                    failureReason.set(reason);
                    failureData.set(data);
                });
        assertEquals(1, taskStorage.getNumberOfTasks());

        processor.onLogout(getContext());
        shadowOf(getMainLooper()).idle();

        assertEquals(TaskScheduler.PURGED_ON_LOGOUT_REASON, failureReason.get());
        assertNull("a request that was never sent has no response body", failureData.get());
        assertEquals("a request that was never sent must not report success", 0, successCalls.get());

        assertEquals(0, taskStorage.getNumberOfTasks());
        assertFalse(TaskScheduler.successCallbackMap.containsKey(registerTaskId));
        assertFalse(TaskScheduler.failureCallbackMap.containsKey(registerTaskId));
        assertTrue("nothing may be left parked for a purged task", TaskScheduler.successCallbackMap.isEmpty());
        assertTrue("nothing may be left parked for a purged task", TaskScheduler.failureCallbackMap.isEmpty());
    }

    @Test
    public void testPreservedDisableDeviceKeepsItsHandlersParked() {
        AtomicInteger disableSuccessCalls = new AtomicInteger();
        AtomicInteger disableFailureCalls = new AtomicInteger();
        AtomicInteger registerFailureCalls = new AtomicInteger();

        String disableTaskId = schedule(IterableConstants.ENDPOINT_DISABLE_DEVICE,
                data -> disableSuccessCalls.incrementAndGet(),
                (reason, data) -> disableFailureCalls.incrementAndGet());
        schedule(IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN, null,
                (reason, data) -> registerFailureCalls.incrementAndGet());
        assertEquals(2, taskStorage.getNumberOfTasks());

        processor.onLogout(getContext());
        shadowOf(getMainLooper()).idle();

        assertEquals("the register was discarded, so its handler has to fire", 1, registerFailureCalls.get());
        assertEquals("the disable survived the purge and is still going to be sent",
                0, disableFailureCalls.get());
        assertEquals(0, disableSuccessCalls.get());

        assertEquals(1, taskStorage.getNumberOfTasks());
        assertTrue(TaskScheduler.successCallbackMap.containsKey(disableTaskId));
        assertTrue(TaskScheduler.failureCallbackMap.containsKey(disableTaskId));
        assertEquals(1, TaskScheduler.failureCallbackMap.size());
    }

    /**
     * Orphaning is not specific to the register endpoint, so the unconditional purge settles handlers
     * too. Nothing in production reaches it today, which is exactly why it needs a test.
     */
    @Test
    public void testDeleteAllTasksAlsoSettlesParkedHandlers() {
        AtomicInteger trackFailureCalls = new AtomicInteger();
        AtomicInteger disableFailureCalls = new AtomicInteger();

        schedule(IterableConstants.ENDPOINT_TRACK, null, (reason, data) -> trackFailureCalls.incrementAndGet());
        schedule(IterableConstants.ENDPOINT_DISABLE_DEVICE, null, (reason, data) -> disableFailureCalls.incrementAndGet());

        scheduler.onTasksPurged(taskStorage.deleteAllTasks());
        shadowOf(getMainLooper()).idle();

        assertEquals(1, trackFailureCalls.get());
        assertEquals("deleteAllTasks() spares nothing, including the disable", 1, disableFailureCalls.get());
        assertEquals(0, taskStorage.getNumberOfTasks());
        assertTrue(TaskScheduler.successCallbackMap.isEmpty());
        assertTrue(TaskScheduler.failureCallbackMap.isEmpty());
    }

    /**
     * The failure handler is app code running during a logout, so it can legitimately queue more work
     * or trigger another purge. Neither may corrupt the purge that is notifying it or recurse.
     */
    @Test
    public void testHandlerThatQueuesAndPurgesAgainDoesNotCorruptThePurge() {
        AtomicInteger firstFailureCalls = new AtomicInteger();
        AtomicInteger nestedFailureCalls = new AtomicInteger();
        AtomicReference<Throwable> reentrantError = new AtomicReference<>();

        schedule(IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN, null, (reason, data) -> {
            firstFailureCalls.incrementAndGet();
            try {
                schedule(IterableConstants.ENDPOINT_TRACK, null, (r, d) -> nestedFailureCalls.incrementAndGet());
                processor.onLogout(getContext());
            } catch (Throwable t) {
                reentrantError.set(t);
            }
        });
        schedule(IterableConstants.ENDPOINT_TRACK, null, (reason, data) -> { });

        processor.onLogout(getContext());
        shadowOf(getMainLooper()).idle();

        assertNull("re-entering the SDK from a purge handler must not throw", reentrantError.get());
        assertEquals("the handler fires exactly once, not once per nested purge", 1, firstFailureCalls.get());
        assertEquals("the task queued from the handler is purged and settled too", 1, nestedFailureCalls.get());
        assertEquals(0, taskStorage.getNumberOfTasks());
        assertTrue(TaskScheduler.successCallbackMap.isEmpty());
        assertTrue(TaskScheduler.failureCallbackMap.isEmpty());
    }

    /**
     * The whole chain as an app sees it: {@code setEmail} parks its completion handlers,
     * autoPushRegistration queues the register that carries them, and the next {@code setEmail}
     * discards that register. An app dismissing a login spinner in the first {@code setEmail}'s
     * callback used to wait forever.
     */
    @Test
    public void testNextLoginSettlesThePreviousLoginsQueuedRegister() throws Exception {
        initializeWithOfflineQueue(true);

        AtomicInteger successA = new AtomicInteger();
        AtomicReference<String> failureA = new AtomicReference<>();

        IterableApi.getInstance().setEmail("userA@example.com",
                data -> successA.incrementAndGet(),
                (reason, data) -> failureA.set(reason));
        awaitTaskCount(1);
        assertEquals(IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN, onlyTaskName());

        // Logout queues a disableDevice (which is preserved), purges userA's register, then queues
        // userB's own register.
        IterableApi.getInstance().setEmail("userB@example.com");
        awaitTaskCount(2);

        assertEquals("userA's queued registration was discarded, so its handler has to fire",
                TaskScheduler.PURGED_ON_LOGOUT_REASON, failureA.get());
        assertEquals("userA never registered, so its success handler must not fire", 0, successA.get());
    }

    /**
     * Settling a discarded register runs the wrapper {@code registerDeviceToken} built around the
     * app's handlers, and that wrapper clears the handler pair it was created with. The next login's
     * pair has to survive that: {@code registerDeviceToken} reads the handler fields on a thread
     * {@code IterableApi} starts, so in production it can read them either side of the purge
     * notification. This forces the order that used to lose them.
     */
    @Test
    public void testSettlingADiscardedRegisterDoesNotClearTheNextLoginsHandlers() throws Exception {
        initializeWithOfflineQueue(true);

        AtomicInteger successA = new AtomicInteger();
        AtomicReference<String> failureA = new AtomicReference<>();
        AtomicInteger successB = new AtomicInteger();
        AtomicInteger failureB = new AtomicInteger();

        IterableApi.getInstance().setEmail("userA@example.com",
                data -> successA.incrementAndGet(),
                (reason, data) -> failureA.set(reason));
        awaitParkedFailureHandler();

        // No token from here on, so nothing dispatches a request by itself and the only registration
        // left in the test is the explicit one below.
        when(IterablePushRegistrationTask.Util.instance.getFirebaseToken()).thenReturn(null);

        IterableApi.getInstance().setEmail("userB@example.com",
                data -> successB.incrementAndGet(),
                (reason, data) -> failureB.incrementAndGet());
        awaitTaskCount(0);

        assertEquals(TaskScheduler.PURGED_ON_LOGOUT_REASON, failureA.get());
        assertEquals(0, successA.get());
        assertEquals("userA's discarded register must not report as userB's failure", 0, failureB.get());

        // What IterableApi's registration thread does once it gets a token, now that the purge has
        // already settled userA's handlers.
        IterableApi.getInstance().registerDeviceToken("userB@example.com", null, null,
                "pushIntegration", TEST_TOKEN, null, IterableApi.getInstance().getDeviceAttributes());
        awaitTaskCount(1);
        drainTaskRunner();

        assertEquals("userB's registration must complete against userB's own handler",
                1, successB.get());
        assertEquals(0, failureB.get());
    }

    /**
     * The register is queued from a thread IterableApi starts, and its handler is parked a moment
     * after the row is written, so wait for the handler rather than for the row.
     */
    private void awaitParkedFailureHandler() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            shadowOf(getMainLooper()).idle();
            for (String taskId : taskStorage.getAllTaskIds()) {
                if (TaskScheduler.failureCallbackMap.get(taskId) != null) {
                    return;
                }
            }
            Thread.sleep(10);
        }
        throw new AssertionError("no queued task ever parked a wrapped failure handler");
    }

    private void initializeWithOfflineQueue(boolean autoPushRegistration) {
        IterableApi.initialize(getContext(), "apiKeyA", new IterableConfig.Builder()
                .setAutoPushRegistration(autoPushRegistration)
                .setPushIntegrationName("pushIntegration")
                .setKeychainEncryption(false)
                .build());
        IterableApi.getInstance().apiClient.setOfflineProcessingEnabled(true);
    }

    private String onlyTaskName() {
        ArrayList<String> taskIds = taskStorage.getAllTaskIds();
        assertEquals(1, taskIds.size());
        IterableTask task = taskStorage.getTask(taskIds.get(0));
        assertNotNull(task);
        return task.name;
    }

    private String schedule(String resourcePath,
                            IterableHelper.SuccessHandler onSuccess,
                            IterableHelper.FailureHandler onFailure) {
        Set<String> before = new HashSet<>(taskStorage.getAllTaskIds());
        IterableApiRequest request = new IterableApiRequest("apiKeyA", resourcePath, new JSONObject(),
                IterableApiRequest.POST, null, onSuccess, onFailure);
        scheduler.scheduleTask(request, onSuccess, onFailure);
        for (String taskId : taskStorage.getAllTaskIds()) {
            if (!before.contains(taskId)) {
                return taskId;
            }
        }
        throw new AssertionError("no task was created for " + resourcePath);
    }

    /**
     * registerDeviceToken is dispatched onto its own thread by IterableApi, so wait for the row
     * rather than idling loopers.
     */
    private void awaitTaskCount(long expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            shadowOf(getMainLooper()).idle();
            if (taskStorage.getNumberOfTasks() == expected) {
                return;
            }
            Thread.sleep(10);
        }
        assertEquals(expected, taskStorage.getNumberOfTasks());
    }

    private void drainTaskRunner() {
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        taskRunner.onTaskCreated(null);
        shadowOf(taskRunner.handler.getLooper()).idle();
        shadowOf(getMainLooper()).idle();
    }
}
