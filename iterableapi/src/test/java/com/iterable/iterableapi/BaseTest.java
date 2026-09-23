package com.iterable.iterableapi;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(TestRunner.class)
public abstract class BaseTest {

    @Rule
    public IterableUtilRule utilsRule = new IterableUtilRule();

    @Before
    public void baseTestSetUp() {
        IterableApi.sharedInstance = IterableTestUtils.newApiWithInlineRequests();
    }

    @After
    public void baseTestTearDown() {
        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();
        IterablePushNotificationUtil.clearPendingAction();
        IterableApi.sharedInstance = IterableTestUtils.newApiWithInlineRequests();
    }

    protected IterableUtilImpl getIterableUtilSpy() {
        return utilsRule.iterableUtilSpy;
    }

    protected Context getContext() {
        return ApplicationProvider.getApplicationContext();
    }
}
