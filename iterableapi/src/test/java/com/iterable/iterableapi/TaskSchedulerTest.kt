package com.iterable.iterableapi

import com.iterable.iterableapi.unit.TestRunner
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@RunWith(TestRunner::class)
class TaskSchedulerTest {
    private lateinit var taskStorage: IterableTaskStorage
    private lateinit var taskRunner: IterableTaskRunner
    private lateinit var requestDispatcher: IterableRequestDispatcher
    private lateinit var taskScheduler: TaskScheduler

    @Before
    fun setUp() {
        TaskScheduler.successCallbackMap.clear()
        TaskScheduler.failureCallbackMap.clear()
        taskStorage = mock(IterableTaskStorage::class.java)
        taskRunner = mock(IterableTaskRunner::class.java)
        requestDispatcher = mock(IterableRequestDispatcher::class.java)
        taskScheduler = TaskScheduler(
            taskStorage,
            taskRunner,
            requestDispatcher
        )
    }

    @Test
    fun `scheduling a request stores its serialized data`() {
        val request = request()

        taskScheduler.scheduleTask(request, null, null)

        verify(taskStorage).createTask(
            "api/test",
            IterableTaskType.API,
            request.toJSONObject().toString()
        )
    }

    @Test
    fun `storage failure dispatches the original request immediately`() {
        val success = mock(IterableHelper.SuccessHandler::class.java)
        val failure = mock(IterableHelper.FailureHandler::class.java)
        val request = request(success, failure)
        `when`(
            taskStorage.createTask(
                any(String::class.java),
                any(IterableTaskType::class.java),
                any(String::class.java)
            )
        ).thenReturn(null)

        taskScheduler.scheduleTask(request, success, failure)

        verify(requestDispatcher).execute(request)
    }

    @Test
    fun `successful stored request calls its client success callback`() {
        val success = mock(IterableHelper.SuccessHandler::class.java)
        val request = request(success, null)
        `when`(
            taskStorage.createTask(
                any(String::class.java),
                any(IterableTaskType::class.java),
                any(String::class.java)
            )
        ).thenReturn("task-id")
        taskScheduler.scheduleTask(request, success, null)

        val responseData = JSONObject()
        taskScheduler.onTaskCompleted(
            "task-id",
            IterableTaskRunner.TaskResult.SUCCESS,
            IterableApiResponse.success(200, "{}", responseData)
        )

        verify(success).onSuccess(responseData)
    }

    @Test
    fun `failed stored request calls its client failure callback`() {
        val failure = mock(IterableHelper.FailureHandler::class.java)
        val request = request(null, failure)
        `when`(
            taskStorage.createTask(
                any(String::class.java),
                any(IterableTaskType::class.java),
                any(String::class.java)
            )
        ).thenReturn("task-id")
        taskScheduler.scheduleTask(request, null, failure)

        val responseData = JSONObject()
        taskScheduler.onTaskCompleted(
            "task-id",
            IterableTaskRunner.TaskResult.FAILURE,
            IterableApiResponse.failure(
                400,
                """{"msg":"Bad request"}""",
                responseData,
                "Bad request"
            )
        )

        verify(failure).onFailure(eq("Bad request"), eq(responseData))
    }

    private fun request(
        success: IterableHelper.SuccessHandler? = null,
        failure: IterableHelper.FailureHandler? = null
    ): IterableApiRequest {
        return IterableApiRequest(
            "api-key",
            "api/test",
            JSONObject(),
            IterableApiRequest.POST,
            null,
            success,
            failure
        )
    }
}
