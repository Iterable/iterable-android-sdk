package com.iterable.iterableapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * The two paths around the switch that {@link IterableSwitchProjectTest} does not reach: a queue
 * drain that no executor will accept, and a switch started off the main thread.
 */
@RunWith(TestRunner.class)
public class IterableSwitchProjectQueueDrainTest extends BaseTest {

    private static final String API_KEY_A = "project-a-key";
    private static final String API_KEY_B = "project-b-key";

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
        for (int i = 0; i < 100 && IterableBackgroundInitializer.isSwitchingProject(); i++) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
            Thread.sleep(20);
        }
        IterableTestUtils.resetIterableApi();
        IterableRequestTask.overrideUrl = null;
        server.shutdown();
        server = null;
    }

    /**
     * Both the caller's executor and the shared one refuse the drain, which the retry on a fresh
     * executor normally rules out. The queued calls are stranded, and what must not happen is the
     * queue latching itself shut: {@code isProcessing} left set would refuse every later drain for
     * the life of the process.
     */
    @Test
    public void testRejectedDrainReleasesTheQueueInsteadOfLatchingItShut() throws Exception {
        ExecutorService rejectingShared = rejectingExecutor();
        ExecutorService originalShared = swapSharedExecutor(rejectingShared);
        try {
            IterableBackgroundInitializer.simulateInitializingState();
            CountDownLatch executed = new CountDownLatch(1);
            IterableBackgroundInitializer.queueOrExecute(executed::countDown, "queued behind the gate");
            assertEquals(1, IterableBackgroundInitializer.getQueuedOperationCount());

            assertEquals(IterableBackgroundInitializer.DrainResult.REJECTED,
                    IterableBackgroundInitializer.processQueuedOperationsOn(rejectingExecutor()));
            assertEquals("a refused drain must leave the queue intact, not silently drop it",
                    1, IterableBackgroundInitializer.getQueuedOperationCount());
            assertFalse(executed.await(100, TimeUnit.MILLISECONDS));

            swapSharedExecutor(originalShared);
            assertEquals("the queue must still be drainable once an executor accepts again",
                    IterableBackgroundInitializer.DrainResult.STARTED,
                    IterableBackgroundInitializer.processQueuedOperationsOn(originalShared));
            assertTrue(executed.await(5, TimeUnit.SECONDS));
        } finally {
            swapSharedExecutor(originalShared);
            IterableBackgroundInitializer.resetBackgroundInitializationState();
        }
    }

    /**
     * switchProject is documented as callable from anywhere. Its callback is always delivered on the
     * main thread, which is the part an app relies on when it re-identifies the user from there.
     */
    @Test
    public void testSwitchStartedOffTheMainThreadCompletesAndCallsBackOnTheMainThread() throws Exception {
        IterableApi.initialize(context, API_KEY_A, config());
        IterableApi.getInstance().setEmail("user-a@example.com");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        CountDownLatch switched = new CountDownLatch(1);
        AtomicReference<Looper> callbackLooper = new AtomicReference<>();
        Thread caller = new Thread(() -> IterableApi.switchProject(context,
                new IterableProject(API_KEY_B, config()),
                cleanTeardown -> {
                    callbackLooper.set(Looper.myLooper());
                    switched.countDown();
                }), "switch-caller");
        caller.start();
        caller.join(5000);

        assertTrue("Callback should fire", awaitSwitch(switched));
        assertEquals(API_KEY_B, IterableApi.getInstance()._apiKey);
        assertNotNull(callbackLooper.get());
        assertEquals("the callback contract is main thread regardless of the calling thread",
                Looper.getMainLooper(), callbackLooper.get());
    }

    /**
     * A push open carries the campaignId, templateId and messageId of the project that sent the
     * push. Queueing it behind the switch gate would replay it once the new project is live, so it
     * would be reported against a project where those IDs do not exist. It has to run inline.
     */
    @Test
    public void testPushOpenDuringASwitchRunsInlineInsteadOfBeingReplayedAgainstTheNewProject() throws Exception {
        IterableApi.initialize(context, API_KEY_A, config());
        IterableApi.getInstance().setEmail("user-a@example.com");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        while (server.takeRequest(50, TimeUnit.MILLISECONDS) != null) { /* drain the setup traffic */ }

        assertTrue(IterableBackgroundInitializer.beginProjectSwitch(null));
        try {
            IterableApi.getInstance().trackPushOpen(11, 22, "msg_from_project_a", false, null);

            assertEquals("a push open must not be parked for the incoming project",
                    0, IterableBackgroundInitializer.getQueuedOperationCount());

            RecordedRequest pushOpen = takeRequestMatching("trackPushOpen");
            assertNotNull("the push open must be sent rather than dropped", pushOpen);
            assertEquals("it must go to the project that sent the push, not the one being switched to",
                    API_KEY_A, pushOpen.getHeader(IterableConstants.HEADER_API_KEY));
        } finally {
            IterableBackgroundInitializer.resetBackgroundInitializationState();
        }
    }

    /**
     * The switch gate and the initialization gate are the same state on Android, so narrowing one
     * risks narrowing the other. A push open made while a background initialization is still in
     * flight still has to be queued: there is no previous project for it to be misattributed to.
     */
    @Test
    public void testPushOpenDuringABackgroundInitializationIsStillQueued() throws Exception {
        IterableApi.initialize(context, API_KEY_A, config());
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        IterableBackgroundInitializer.simulateInitializingState();
        try {
            IterableApi.getInstance().trackPushOpen(11, 22, "msg", false, null);

            assertEquals("initialization queueing must be left alone",
                    1, IterableBackgroundInitializer.getQueuedOperationCount());
        } finally {
            IterableBackgroundInitializer.resetBackgroundInitializationState();
        }
    }

    private RecordedRequest takeRequestMatching(String pathFragment) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
            RecordedRequest request = server.takeRequest(100, TimeUnit.MILLISECONDS);
            if (request == null) {
                continue;
            }
            if (request.getPath() != null && request.getPath().contains(pathFragment)) {
                return request;
            }
        }
        return null;
    }

    private IterableConfig config() {
        return new IterableConfig.Builder()
                .setAutoPushRegistration(false)
                .setKeychainEncryption(false)
                .build();
    }

    private ExecutorService rejectingExecutor() {
        ExecutorService executor = mock(ExecutorService.class);
        // Not shut down, so ensureBackgroundExecutor() hands it out rather than replacing it. That is
        // the only shape in which a drain can be refused twice: an executor shutting down between
        // being handed out and execute().
        when(executor.isShutdown()).thenReturn(false);
        doThrow(new RejectedExecutionException("test")).when(executor).execute(any(Runnable.class));
        return executor;
    }

    /**
     * The shared executor is a private static, and there is no seam for replacing it. Adding one for
     * a test that exercises a refusal would put test-only surface on the production class.
     */
    private ExecutorService swapSharedExecutor(ExecutorService executor) throws Exception {
        Field field = IterableBackgroundInitializer.class.getDeclaredField("backgroundExecutor");
        field.setAccessible(true);
        ExecutorService previous = (ExecutorService) field.get(null);
        field.set(null, executor);
        return previous;
    }

    private boolean awaitSwitch(CountDownLatch latch) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
            if (latch.await(50, TimeUnit.MILLISECONDS)) {
                ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
                return true;
            }
        }
        return false;
    }
}
