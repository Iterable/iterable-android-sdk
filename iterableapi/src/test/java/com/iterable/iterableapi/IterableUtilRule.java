package com.iterable.iterableapi;

import static org.mockito.Mockito.spy;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class IterableUtilRule extends TestWatcher {
    private IterableUtilImpl originalIterableUtil;
    public IterableUtilImpl iterableUtilSpy;

    @Override
    protected void starting(Description description) {
        originalIterableUtil = IterableUtil.instance;
        iterableUtilSpy = spy(originalIterableUtil);
        IterableUtil.instance = iterableUtilSpy;
    }

    @Override
    protected void finished(Description description) {
        IterableUtil.instance = originalIterableUtil;
        iterableUtilSpy = null;
    }
}
