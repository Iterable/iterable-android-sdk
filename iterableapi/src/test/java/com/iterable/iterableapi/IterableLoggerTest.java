package com.iterable.iterableapi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import com.iterable.iterableapi.util.LogLevel;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class IterableLoggerTest extends BaseTest {

    @Before
    public void setUp() throws IOException {
        IterableApi.sharedInstance = new IterableApi();
    }

    @Test
    public void testIsLoggableLevelLogic() {
        IterableTestUtils.createIterableApiNew(new IterableTestUtils.ConfigBuilderExtender() {
            @Override
            public IterableConfig.Builder run(IterableConfig.Builder builder) {
                return builder.setLogLevel(LogLevel.WARN);
            }
        });

        assertFalse(IterableLogger.isLoggableLevel(LogLevel.VERBOSE));
        assertFalse(IterableLogger.isLoggableLevel(LogLevel.DEBUG));
        assertFalse(IterableLogger.isLoggableLevel(LogLevel.INFO));
        assertTrue(IterableLogger.isLoggableLevel(LogLevel.WARN));
        assertTrue(IterableLogger.isLoggableLevel(LogLevel.ERROR));
    }

    @Test
    public void testShouldDisableLogging() {
        IterableTestUtils.createIterableApiNew(new IterableTestUtils.ConfigBuilderExtender() {
            @Override
            public IterableConfig.Builder run(IterableConfig.Builder builder) {
                return builder.setLogLevel(LogLevel.NONE);
            }
        });

        assertFalse(IterableLogger.isLoggableLevel(LogLevel.VERBOSE));
        assertFalse(IterableLogger.isLoggableLevel(LogLevel.DEBUG));
        assertFalse(IterableLogger.isLoggableLevel(LogLevel.INFO));
        assertFalse(IterableLogger.isLoggableLevel(LogLevel.WARN));
        assertFalse(IterableLogger.isLoggableLevel(LogLevel.ERROR));
    }

    @Test
    public void testShouldLogAllInDebugMode() {
        IterableTestUtils.createIterableApiNew(new IterableTestUtils.ConfigBuilderExtender() {
            @Override
            public IterableConfig.Builder run(IterableConfig.Builder builder) {
                return builder.setLogLevel(LogLevel.ERROR);
            }
        });

        IterableApi.getInstance().setDebugMode(true);

        assertTrue(IterableLogger.isLoggableLevel(LogLevel.VERBOSE));
        assertTrue(IterableLogger.isLoggableLevel(LogLevel.DEBUG));
        assertTrue(IterableLogger.isLoggableLevel(LogLevel.INFO));
        assertTrue(IterableLogger.isLoggableLevel(LogLevel.WARN));
        assertTrue(IterableLogger.isLoggableLevel(LogLevel.ERROR));
        assertTrue(IterableLogger.isLoggableLevel(LogLevel.NONE));
    }
}