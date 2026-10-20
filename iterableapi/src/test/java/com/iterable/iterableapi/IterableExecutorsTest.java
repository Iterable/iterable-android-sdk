package com.iterable.iterableapi;

import android.os.Process;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(TestRunner.class)
public class IterableExecutorsTest {
    @Test
    public void requestExecutorSpillsExcessWorkToBackgroundOverflowWithoutDroppingIt()
            throws Exception {
        ThreadPoolExecutor overflowExecutor =
                IterableExecutors.newRequestOverflowExecutor(1);
        ThreadPoolExecutor executor =
                IterableExecutors.newRequestExecutor(2, 1, overflowExecutor);
        CountDownLatch workersStarted = new CountDownLatch(2);
        CountDownLatch releaseWorkers = new CountDownLatch(1);
        CountDownLatch queuedWorkCompleted = new CountDownLatch(1);
        CountDownLatch overflowWorkCompleted = new CountDownLatch(1);
        AtomicLong overflowThreadId = new AtomicLong();
        AtomicInteger overflowPriority = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger overflowRuns = new AtomicInteger();
        long callerThreadId = Thread.currentThread().getId();

        try {
            Runnable blockingWork = () -> {
                workersStarted.countDown();
                try {
                    releaseWorkers.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };
            executor.execute(blockingWork);
            executor.execute(blockingWork);
            assertTrue(workersStarted.await(5, TimeUnit.SECONDS));

            executor.execute(queuedWorkCompleted::countDown);
            executor.execute(() -> {
                overflowRuns.incrementAndGet();
                overflowThreadId.set(Thread.currentThread().getId());
                overflowPriority.set(
                        Process.getThreadPriority(Process.myTid())
                );
                overflowWorkCompleted.countDown();
            });

            assertTrue(overflowWorkCompleted.await(5, TimeUnit.SECONDS));
            assertEquals(1, overflowRuns.get());
            assertNotEquals(callerThreadId, overflowThreadId.get());
            assertEquals(
                    Process.THREAD_PRIORITY_BACKGROUND,
                    overflowPriority.get()
            );
        } finally {
            releaseWorkers.countDown();
            assertTrue(queuedWorkCompleted.await(5, TimeUnit.SECONDS));
            executor.shutdownNow();
            overflowExecutor.shutdownNow();
        }
    }

    @Test
    public void requestWorkersUseAndroidBackgroundPriority() throws Exception {
        ThreadPoolExecutor executor = IterableExecutors.newRequestExecutor(1, 1);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicInteger priority = new AtomicInteger(Integer.MIN_VALUE);

        try {
            executor.execute(() -> {
                priority.set(Process.getThreadPriority(Process.myTid()));
                completed.countDown();
            });

            assertTrue(completed.await(5, TimeUnit.SECONDS));
            assertEquals(Process.THREAD_PRIORITY_BACKGROUND, priority.get());
            assertTrue(executor.getThreadFactory().newThread(() -> { }).isDaemon());
        } finally {
            executor.shutdownNow();
        }
    }
}
