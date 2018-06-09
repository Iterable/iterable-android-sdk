package com.iterable.iterableapi.unit;

import android.content.Context;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RuntimeEnvironment;

@RunWith(TestRunner.class)
@PowerMockIgnore({
        "org.mockito.*",
        "org.robolectric.*",
        "org.json.*",
        "org.powermock.*",
        "android.*",
        "okhttp3.*",
        "javax.net.ssl.*",
        "com.squareup.*",
        "okio.*"
})
public abstract class BaseTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    protected Context getContext() {
        return RuntimeEnvironment.application;
    }

}
