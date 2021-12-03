package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(TestRunner.class)
public class TaskSchedulerTest {
    private IterableTaskStorage mockTaskStorage;
    private IterableTaskRunner mockTaskRunner;
    private TaskScheduler taskScheduler;

    @Before
    public void setUp() throws Exception {
        mockTaskStorage = mock(IterableTaskStorage.class);
        mockTaskRunner = mock(IterableTaskRunner.class);
        taskScheduler = new TaskScheduler(mockTaskStorage, mockTaskRunner);
    }

    @Test
    public void testScheduleTaskCreatesTaskInStorage() throws Exception {
        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        taskScheduler.scheduleTask(request, null, null);
        verify(mockTaskStorage).createTask(eq("api/test"), eq(IterableTaskType.API), eq(request.toJSONObject().toString()));
    }

    @Test
    public void testSuccessCallbackIsCalledOnCompletion() throws Exception {
        IterableHelper.SuccessHandler successHandler = mock(IterableHelper.SuccessHandler.class);
        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        when(mockTaskStorage.createTask(any(String.class), any(IterableTaskType.class), any(String.class))).thenReturn("testTaskId");
        taskScheduler.scheduleTask(request, successHandler, null);
        taskScheduler.onTaskCompleted("testTaskId", IterableTaskRunner.TaskResult.SUCCESS, IterableApiResponse.success(200, "", new JSONObject()));
        verify(successHandler).onSuccess(any(JSONObject.class));
    }

    @Test
    public void testFailureCallbackIsCalledOnCompletion() throws Exception {
        IterableHelper.FailureHandler failureHandler = mock(IterableHelper.FailureHandler.class);
        IterableApiRequest request = new IterableApiRequest("apiKey", "api/test", new JSONObject(), "POST", null, null, null);
        when(mockTaskStorage.createTask(any(String.class), any(IterableTaskType.class), any(String.class))).thenReturn("testTaskId");
        taskScheduler.scheduleTask(request, null, failureHandler);
        taskScheduler.onTaskCompleted("testTaskId", IterableTaskRunner.TaskResult.FAILURE, IterableApiResponse.failure(400, "", new JSONObject(), "TestError"));
        verify(failureHandler).onFailure(eq("TestError"), any(JSONObject.class));
    }
}
