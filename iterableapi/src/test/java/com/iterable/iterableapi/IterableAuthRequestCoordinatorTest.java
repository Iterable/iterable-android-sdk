package com.iterable.iterableapi;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class IterableAuthRequestCoordinatorTest {
    private IterableAuthRequestCoordinator<Object> coordinator;
    private Object identityA;
    private Object identityB;

    @Before
    public void setUp() {
        coordinator = new IterableAuthRequestCoordinator<>();
        identityA = new Object();
        identityB = new Object();
    }

    @Test
    public void firstRequestStartsImmediately() {
        IterableHelper.SuccessHandler callback = mock(IterableHelper.SuccessHandler.class);

        IterableAuthRequestCoordinator.EnqueueResult<Object> result =
                coordinator.enqueue(identityA, callback, false, true);

        assertEquals(
                IterableAuthRequestCoordinator.EnqueueStatus.STARTED,
                result.getStatus()
        );
        assertSame(callback, result.getRequestToStart().getSuccessCallback());
        assertFalse(result.getRequestToStart().hasFailedPriorAuth());
        assertTrue(result.getRequestToStart().shouldIgnoreRetryPolicy());
    }

    @Test
    public void sameIdentityKeepsTheActiveRequest() {
        IterableHelper.SuccessHandler firstCallback =
                mock(IterableHelper.SuccessHandler.class);
        IterableHelper.SuccessHandler secondCallback =
                mock(IterableHelper.SuccessHandler.class);
        IterableAuthRequestCoordinator.Request<Object> activeRequest =
                coordinator.enqueue(
                        identityA,
                        firstCallback,
                        false,
                        true
                ).getRequestToStart();

        IterableAuthRequestCoordinator.EnqueueResult<Object> result =
                coordinator.enqueue(identityA, secondCallback, false, false);

        assertEquals(
                IterableAuthRequestCoordinator.EnqueueStatus.ALREADY_ACTIVE_FOR_IDENTITY,
                result.getStatus()
        );
        assertNull(result.getRequestToStart());
        assertTrue(coordinator.complete(activeRequest, identityA).isResultAccepted());
        assertSame(firstCallback, activeRequest.getSuccessCallback());
    }

    @Test
    public void failedRetryIsIgnoredWhileARequestIsActive() {
        IterableAuthRequestCoordinator.Request<Object> requestA =
                coordinator.enqueue(identityA, null, false, true).getRequestToStart();

        IterableAuthRequestCoordinator.EnqueueResult<Object> result =
                coordinator.enqueue(identityB, null, true, true);
        IterableAuthRequestCoordinator.Completion<Object> completion =
                coordinator.complete(requestA, identityB);

        assertEquals(
                IterableAuthRequestCoordinator.EnqueueStatus.IGNORED_FAILED_RETRY,
                result.getStatus()
        );
        assertNull(completion.getNextRequest());
    }

    @Test
    public void newIdentityRunsAfterTheStaleRequestCompletes() {
        IterableHelper.SuccessHandler callbackB =
                mock(IterableHelper.SuccessHandler.class);
        IterableAuthRequestCoordinator.Request<Object> requestA =
                coordinator.enqueue(identityA, null, false, true).getRequestToStart();

        IterableAuthRequestCoordinator.EnqueueResult<Object> enqueueB =
                coordinator.enqueue(identityB, callbackB, false, true);
        IterableAuthRequestCoordinator.Completion<Object> completion =
                coordinator.complete(requestA, identityB);

        assertEquals(
                IterableAuthRequestCoordinator.EnqueueStatus.QUEUED_FOR_NEW_IDENTITY,
                enqueueB.getStatus()
        );
        assertFalse(completion.isResultAccepted());
        assertSame(identityB, completion.getNextRequest().getIdentity());
        assertSame(callbackB, completion.getNextRequest().getSuccessCallback());
        assertFalse(completion.getNextRequest().hasFailedPriorAuth());
    }

    @Test
    public void latestQueuedIdentityWins() {
        Object identityC = new Object();
        IterableAuthRequestCoordinator.Request<Object> requestA =
                coordinator.enqueue(identityA, null, false, true).getRequestToStart();
        coordinator.enqueue(identityB, null, false, true);
        coordinator.enqueue(identityC, null, false, true);

        IterableAuthRequestCoordinator.Completion<Object> completion =
                coordinator.complete(requestA, identityC);

        assertFalse(completion.isResultAccepted());
        assertSame(identityC, completion.getNextRequest().getIdentity());
    }

    @Test
    public void clearingQueuedWorkLeavesNoSuccessor() {
        IterableAuthRequestCoordinator.Request<Object> requestA =
                coordinator.enqueue(identityA, null, false, true).getRequestToStart();
        coordinator.enqueue(identityB, null, false, true);

        coordinator.clearQueued();
        IterableAuthRequestCoordinator.Completion<Object> completion =
                coordinator.complete(requestA, identityB);

        assertFalse(completion.isResultAccepted());
        assertNull(completion.getNextRequest());
    }

    @Test
    public void orphanedCompletionCannotClearTheReplacement() {
        IterableAuthRequestCoordinator.Request<Object> requestA =
                coordinator.enqueue(identityA, null, false, true).getRequestToStart();
        coordinator.enqueue(identityB, null, false, true);
        IterableAuthRequestCoordinator.Request<Object> requestB =
                coordinator.complete(requestA, identityB).getNextRequest();

        IterableAuthRequestCoordinator.Completion<Object> orphanedCompletion =
                coordinator.complete(requestA, identityB);

        assertFalse(orphanedCompletion.isResultAccepted());
        assertNull(orphanedCompletion.getNextRequest());
        assertTrue(coordinator.isCurrent(requestB, identityB));
    }
}
