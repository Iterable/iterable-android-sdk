package com.iterable.iterableapi;

import android.content.Context;

import com.iterable.iterableapi.IterableUtilRule;
import com.iterable.iterableapi.unit.TestRunner;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(TestRunner.class)
public abstract class BaseTest {

    @Rule
    public IterableUtilRule utilsRule = new IterableUtilRule();

    protected IterableUtil.IterableUtilImpl getIterableUtilSpy() {
        return utilsRule.iterableUtilSpy;
    }

    protected Context getContext() {
        return RuntimeEnvironment.application;
    }


}
