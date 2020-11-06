package com.iterable.iterableapi;

import android.content.Context;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.util.concurrent.InlineExecutorService;
import org.robolectric.shadows.ShadowPausedAsyncTask;

@RunWith(TestRunner.class)
public abstract class BaseTest {

    @Rule
    public IterableUtilRule utilsRule = new IterableUtilRule();

    @Rule
    public AsyncTaskRule asyncTaskRule = new AsyncTaskRule();

    protected IterableUtil.IterableUtilImpl getIterableUtilSpy() {
        return utilsRule.iterableUtilSpy;
    }

    protected Context getContext() {
        return RuntimeEnvironment.application;
    }

    private static class AsyncTaskRule extends TestWatcher {
        @Override
        protected void starting(Description description) {
            ShadowPausedAsyncTask.overrideExecutor(new InlineExecutorService());
        }
    }

}
