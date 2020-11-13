package com.iterable.iterableapi;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PowerMockIgnore({
        "org.mockito.*",
        "org.robolectric.*",
        "org.json.*",
        "org.powermock.*",
        "android.*",
        "okhttp3.*",
        "javax.net.ssl.*",
        "javax.xml.parsers.*",
        "com.sun.org.apache.xerces.internal.jaxp.*",
        "com.squareup.*",
        "okio.*"
})
@RunWith(TestRunner.class)
public abstract class BasePowerMockTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    public Context getContext() {
        return ApplicationProvider.getApplicationContext();
    }

}
