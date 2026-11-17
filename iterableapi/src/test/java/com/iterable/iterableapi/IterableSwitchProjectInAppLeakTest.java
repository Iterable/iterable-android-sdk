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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * In-app messages are stored in one file that every project reads, so the previous project's
 * messages must be off it before the next project's storage loads it, and must not be written
 * back afterwards.
 */
@RunWith(TestRunner.class)
public class IterableSwitchProjectInAppLeakTest extends BaseTest {

    private static final String API_KEY_A = "project-a-key";
    private static final String API_KEY_B = "project-b-key";
    private static final String EMAIL_A = "user-a@example.com";
    private static final String NO_MESSAGES = "{\"inAppMessages\":[]}";

    private Context context;
    private MockWebServer server;
    private volatile String inAppPayload = NO_MESSAGES;
    private volatile long inAppDelayMs = 0;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().startsWith("/" + IterableConstants.ENDPOINT_GET_INAPP_MESSAGES)) {
                    return new MockResponse().setResponseCode(200).setBody(inAppPayload)
                            .setBodyDelay(inAppDelayMs, TimeUnit.MILLISECONDS);
                }
                return new MockResponse().setResponseCode(200).setBody("{}");
            }
        });
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        IterableTestUtils.resetIterableApi();
        // A real in-app manager, so its file storage is the one under test.
        IterableApi.sharedInstance = new IterableApi();
    }

    @After
    public void tearDown() throws Exception {
        settle();
        IterableTestUtils.resetIterableApi();
        IterableRequestTask.overrideUrl = null;
        server.shutdown();
    }

    /**
     * A's messages are on disk when the switch resets A's manager. The reset only schedules its
     * save, and B's storage reads the file as soon as it is built.
     */
    @Test
    public void testTheNextProjectDoesNotLoadThePreviousProjectsMessagesBeforeTheirRemovalIsSaved() throws Exception {
        inAppPayload = IterableTestUtils.getResourceString("inapp_payload_inbox_multiple.json");
        initializeProjectA();
        for (int i = 0; i < 50 && IterableApi.getInstance().getInAppManager().getMessages().isEmpty(); i++) {
            drainMainThread();
            Thread.sleep(20);
        }
        letScheduledSavesRun();
        assertFalse("A's messages have to be on disk for this test to mean anything", messagesOnDisk() == 0);
        inAppPayload = NO_MESSAGES;

        switchToBAndSettle();

        assertEquals("B must not hold A's in-app messages", 0,
                IterableApi.getInstance().getInAppManager().getMessages().size());
    }

    /**
     * A's getMessages is still in flight when the switch resets A's manager. Its response must not
     * be applied to A's storage afterwards, or B loads A's messages from disk on its next launch.
     */
    @Test
    public void testASyncInFlightAtTheSwitchIsNotSavedForTheNextProject() throws Exception {
        initializeProjectA();
        letScheduledSavesRun();

        inAppPayload = IterableTestUtils.getResourceString("inapp_payload_inbox_multiple.json");
        inAppDelayMs = 400;
        IterableApi.getInstance().getInAppManager().syncInApp();

        switchToBAndSettle();
        for (int i = 0; i < 20; i++) {
            drainMainThread();
            Thread.sleep(50);
        }
        letScheduledSavesRun();

        assertEquals("A's late sync must not reach the file B reads", 0, messagesOnDisk());
    }

    /**
     * The teardown resets A's manager several steps before it releases it, and a foreground sync
     * can start in between while A's user is still identified. Retiring the manager at release has
     * to refuse that sync too.
     */
    @Test
    public void testASyncStartedAfterTheResetIsNotAppliedOnceTheManagerIsRetired() throws Exception {
        initializeProjectA();
        letScheduledSavesRun();
        IterableInAppManager inAppManagerA = IterableApi.getInstance().getInAppManager();
        inAppManagerA.reset();

        inAppPayload = IterableTestUtils.getResourceString("inapp_payload_inbox_multiple.json");
        inAppDelayMs = 400;
        inAppManagerA.syncInApp();
        inAppManagerA.retire();
        for (int i = 0; i < 20; i++) {
            drainMainThread();
            Thread.sleep(50);
        }
        letScheduledSavesRun();

        assertEquals("a retired manager must not apply a sync", 0, inAppManagerA.getMessages().size());
        assertEquals(0, messagesOnDisk());
    }

    // region Helpers

    private void initializeProjectA() {
        IterableApi.initialize(context, API_KEY_A, configWithoutAuth());
        IterableApi.getInstance().setEmail(EMAIL_A);
        drainMainThread();
    }

    private void switchToBAndSettle() throws InterruptedException {
        CountDownLatch landedOnB = new CountDownLatch(1);
        IterableApi.switchProject(context, new IterableProject(API_KEY_B, configWithoutAuth()),
                ignored -> landedOnB.countDown());
        assertTrue(awaitSwitch(landedOnB));
        settle();
    }

    /** What the next launch would load. */
    private int messagesOnDisk() {
        return new IterableInAppFileStorage(context).getMessages().size();
    }

    private void letScheduledSavesRun() throws InterruptedException {
        ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS);
        Thread.sleep(200);
    }

    private IterableConfig configWithoutAuth() {
        return new IterableConfig.Builder()
                .setAutoPushRegistration(false)
                .setKeychainEncryption(false)
                .build();
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
