package com.iterable.iterableapi;

import android.os.Process;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(TestRunner.class)
public class IterableExecutorsTest {
    @Test
    public void requestExecutorIsBoundedAndNeverRunsRejectedWorkOnCaller()
            throws Exception {
        ThreadPoolExecutor executor = IterableExecutors.newRequestExecutor(2, 1);
        CountDownLatch workersStarted = new CountDownLatch(2);
        CountDownLatch releaseWorkers = new CountDownLatch(1);
        AtomicInteger callerRuns = new AtomicInteger();

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

            executor.execute(() -> { });
            try {
                executor.execute(callerRuns::incrementAndGet);
                fail("Expected the bounded executor to reject excess work");
            } catch (RejectedExecutionException expected) {
                assertEquals(0, callerRuns.get());
            }
        } finally {
            releaseWorkers.countDown();
            executor.shutdownNow();
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
