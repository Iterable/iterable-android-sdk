package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(TestRunner.class)
public class HealthMonitorTests {
    private IterableTaskStorage mockTaskStorage;
    private IterableTaskRunner mockTaskRunner;
    private TaskScheduler mockTaskScheduler;

    @Before
    public void setUp() {
        mockTaskStorage = mock(IterableTaskStorage.class);
        mockTaskRunner = mock(IterableTaskRunner.class);
        mockTaskScheduler = mock(TaskScheduler.class);
    }

    @Test
    public void testUseOfflineProcessorByDefault() throws Exception {
        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        
        verifyNoInteractions(mockTaskScheduler);
    }
}
