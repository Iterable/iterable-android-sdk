package com.iterable.iterableapi;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.robolectric.android.util.concurrent.InlineExecutorService;
import org.robolectric.shadows.ShadowPausedAsyncTask;

@RunWith(TestRunner.class)
public abstract class BaseTest {

    @Rule
    public IterableUtilRule utilsRule = new IterableUtilRule();

    @Rule
    public AsyncTaskRule asyncTaskRule = new AsyncTaskRule();

    protected IterableUtilImpl getIterableUtilSpy() {
        return utilsRule.iterableUtilSpy;
    }

    protected Context getContext() {
        return ApplicationProvider.getApplicationContext();
    }

    private static class AsyncTaskRule extends TestWatcher {
        @Override
        protected void starting(Description description) {
            ShadowPausedAsyncTask.overrideExecutor(new InlineExecutorService());
        }
    }

}
