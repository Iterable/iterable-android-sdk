package com.iterable.iterableapi;

import static junit.framework.Assert.assertEquals;

import android.util.Log;

import com.iterable.iterableapi.unit.TestRunner;
import com.iterable.iterableapi.util.LogLevel;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestRunner.class)
public class LogLevelTest {

    @Test
    public void testLogLevelsHaveExpectedValues() {
        assertEquals(Log.VERBOSE, LogLevel.VERBOSE);
        assertEquals(Log.DEBUG, LogLevel.DEBUG);
        assertEquals(Log.INFO, LogLevel.INFO);
        assertEquals(Log.WARN, LogLevel.WARN);
        assertEquals(Log.ERROR, LogLevel.ERROR);
        assertEquals(Integer.MAX_VALUE, LogLevel.NONE);
    }
}
