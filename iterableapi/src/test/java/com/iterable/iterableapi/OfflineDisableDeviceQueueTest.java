package com.iterable.iterableapi;

import android.content.Context;

import androidx.annotation.NonNull;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static android.os.Looper.getMainLooper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * End-to-end coverage for the offline device registration queue.
 *
 * These tests deliberately go through {@link IterableApi#disablePush()} /
 * {@link IterableApi#registerForPush()} and the real {@link OfflineRequestProcessor} and
 * {@link IterableTaskStorage} rather than inserting rows by hand. Hand-inserted rows prove
 * nothing about whether the endpoint is actually offline compatible, which is why the 3.7.0
 * regression went unnoticed.
 */
@RunWith(TestRunner.class)
public class OfflineDisableDeviceQueueTest extends BaseTest {

    private static final String TEST_TOKEN = "testToken";
    private static final String DISABLE_PATH = "/" + IterableConstants.ENDPOINT_DISABLE_DEVICE;
    private static final String REGISTER_PATH = "/" + IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN;

    private MockWebServer server;
    private IterableTaskStorage taskStorage;
    private IterableTaskRunner taskRunner;
    private IterableNetworkConnectivityManager mockNetworkConnectivityManager;
    private IterablePushRegistrationTask.Util.UtilImpl originalPushRegistrationUtil;

    @Before
    public void setUp() throws Exception {
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

        IterableTestUtils.createIterableApi();

        taskStorage = IterableTaskStorage.sharedInstance(getContext());
        taskStorage.deleteAllTasks();
        IterableApi.getInstance().apiClient.setOfflineProcessingEnabled(true);

        // A runner we can drive deterministically over the same storage the SDK writes to.
        // The processor builds its own runner, but its network thread is never idled here, so
        // it cannot flush behind our back.
        mockNetworkConnectivityManager = mock(IterableNetworkConnectivityManager.class);
        IterableActivityMonitor mockActivityMonitor = mock(IterableActivityMonitor.class);
        when(mockActivityMonitor.isInForeground()).thenReturn(true);
        HealthMonitor mockHealthMonitor = mock(HealthMonitor.class);
        when(mockHealthMonitor.canProcess()).thenReturn(true);
        taskRunner = new IterableTaskRunner(taskStorage, mockActivityMonitor,
                mockNetworkConnectivityManager, mockHealthMonitor, new ApiEndpointClassification());
    }

    @After
    public void tearDown() throws Exception {
        // IterableTaskStorage is a singleton that outlives the test, so a runner left
        // registered here would keep flushing tasks created by the next test.
        taskStorage.removeDatabaseStatusListener(taskRunner);
        taskStorage.deleteAllTasks();
        IterableApi.getInstance().apiClient.setOfflineProcessingEnabled(false);
        IterablePushRegistrationTask.Util.instance = originalPushRegistrationUtil;
        server.shutdown();
        server = null;
    }

    @Test
    public void testDisablePushWhileOfflineIsQueuedSurvivesLogoutAndIsSent() throws Exception {
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(false);

        IterableApi.getInstance().disablePush();
        shadowOf(getMainLooper()).idle();

        assertEquals("disablePush() while offline should persist a task", 1, taskStorage.getNumberOfTasks());
        assertEquals(IterableConstants.ENDPOINT_DISABLE_DEVICE, onlyTask().name);

        drainTaskRunner();
        assertNull("nothing should be sent while the network is down", nextDeviceRequestPath(200));
        assertEquals(1, taskStorage.getNumberOfTasks());

        // Something unrelated in the queue, so the purge has something to actually delete.
        IterableApi.getInstance().track("offlineEvent");
        shadowOf(getMainLooper()).idle();
        assertEquals(2, taskStorage.getNumberOfTasks());

        IterableApi.getInstance().apiClient.onLogout();

        assertEquals("logout should keep the queued disable and drop everything else",
                1, taskStorage.getNumberOfTasks());
        assertEquals(IterableConstants.ENDPOINT_DISABLE_DEVICE, onlyTask().name);

        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        drainTaskRunner();

        assertEquals("the preserved disable should be sent once back online",
                DISABLE_PATH, nextDeviceRequestPath(1000));
        assertEquals(0, taskStorage.getNumberOfTasks());
    }

    @Test
    public void testQueuedDisableIsReplayedBeforeALaterRegister() throws Exception {
        when(mockNetworkConnectivityManager.isConnected()).thenReturn(false);

        IterableApi.getInstance().disablePush();
        shadowOf(getMainLooper()).idle();
        IterableApi.getInstance().apiClient.onLogout();

        // scheduledAt has millisecond resolution and `order by scheduled` has no tie-break,
        // so put a real gap between the two tasks rather than relying on wall-clock luck.
        Thread.sleep(5);

        // registerDeviceToken is dispatched onto its own thread by IterableApi, so wait for
        // the row rather than idling loopers.
        IterableApi.getInstance().registerForPush();
        awaitTaskCount(2);

        when(mockNetworkConnectivityManager.isConnected()).thenReturn(true);
        drainTaskRunner();

        // FIFO by scheduledAt is what stops the stale disable from undoing the new
        // registration: the disable was queued first, so it has to go out first.
        assertEquals(DISABLE_PATH, nextDeviceRequestPath(1000));
        assertEquals(REGISTER_PATH, nextDeviceRequestPath(1000));
        assertEquals(0, taskStorage.getNumberOfTasks());
    }

    /**
     * NAME is nullable in the schema, and SQL three-valued logic makes `name != ?` evaluate to NULL
     * rather than true for a null name, so a bare inequality would leave unnamed rows behind forever.
     * A null name is not reachable through the SDK today, which is why it needs asserting.
     */
    @Test
    public void testLogoutPurgeAlsoRemovesRowsWithNoName() throws Exception {
        String unnamedTaskId = taskStorage.createTask(null, IterableTaskType.API, "{}");
        String disableTaskId = taskStorage.createTask(IterableConstants.ENDPOINT_DISABLE_DEVICE, IterableTaskType.API, "{}");
        assertEquals(2, taskStorage.getNumberOfTasks());

        taskStorage.deleteAllTasksExcept(IterableConstants.ENDPOINT_DISABLE_DEVICE);

        ArrayList<String> remaining = taskStorage.getAllTaskIds();
        assertEquals("only the disable is spared; the null-named row must go", 1, remaining.size());
        assertEquals(disableTaskId, remaining.get(0));
        assertNull(taskStorage.getTask(unnamedTaskId));
    }

    /**
     * Returns the path of the next device registration request, skipping the unrelated traffic
     * (in-app sync, remote config) that SDK initialization produces.
     */
    private String nextDeviceRequestPath(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        do {
            RecordedRequest request = server.takeRequest(timeoutMs, TimeUnit.MILLISECONDS);
            if (request == null) {
                return null;
            }
            String path = request.getPath();
            if (DISABLE_PATH.equals(path) || REGISTER_PATH.equals(path)) {
                return path;
            }
        } while (System.currentTimeMillis() < deadline);
        return null;
    }

    private void awaitTaskCount(long expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            shadowOf(getMainLooper()).idle();
            if (taskStorage.getNumberOfTasks() == expected) {
                return;
            }
            Thread.sleep(10);
        }
        assertEquals(expected, taskStorage.getNumberOfTasks());
    }

    private IterableTask onlyTask() {
        ArrayList<String> ids = taskStorage.getAllTaskIds();
        assertEquals(1, ids.size());
        return taskStorage.getTask(ids.get(0));
    }

    private void drainTaskRunner() {
        taskRunner.onTaskCreated(null);
        shadowOf(taskRunner.handler.getLooper()).idle();
        shadowOf(getMainLooper()).idle();
    }
}
