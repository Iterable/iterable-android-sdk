package com.iterable.iterableapi

import android.content.Context

import androidx.annotation.MainThread
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting

import org.json.JSONException
import org.json.JSONObject

import java.util.HashMap

internal class OfflineRequestProcessor : RequestProcessor {
    private lateinit var taskScheduler: TaskScheduler
    private lateinit var taskRunner: IterableTaskRunner
    private lateinit var taskStorage: IterableTaskStorage
    private lateinit var healthMonitor: HealthMonitor

    companion object {
        private val offlineApiSet = setOf(
                IterableConstants.ENDPOINT_TRACK,
                IterableConstants.ENDPOINT_TRACK_PUSH_OPEN,
                IterableConstants.ENDPOINT_TRACK_PURCHASE,
                IterableConstants.ENDPOINT_TRACK_INAPP_OPEN,
                IterableConstants.ENDPOINT_TRACK_INAPP_CLICK,
                IterableConstants.ENDPOINT_TRACK_INAPP_CLOSE,
                IterableConstants.ENDPOINT_TRACK_INBOX_SESSION,
                IterableConstants.ENDPOINT_TRACK_INAPP_DELIVERY,
                IterableConstants.ENDPOINT_GET_EMBEDDED_MESSAGES,
                IterableConstants.ENDPOINT_INAPP_CONSUME)
    }

    constructor(context: Context) {
        val networkConnectivityManager = IterableNetworkConnectivityManager.sharedInstance(context)
        taskStorage = IterableTaskStorage.sharedInstance(context)
        healthMonitor = HealthMonitor(taskStorage)
        taskRunner = IterableTaskRunner(taskStorage,
                IterableActivityMonitor.getInstance(),
                networkConnectivityManager,
                healthMonitor)
        taskScheduler = TaskScheduler(taskStorage, taskRunner)
    }

    @VisibleForTesting
    constructor(scheduler: TaskScheduler, iterableTaskRunner: IterableTaskRunner, storage: IterableTaskStorage, mockHealthMonitor: HealthMonitor) {
        taskRunner = iterableTaskRunner
        taskScheduler = scheduler
        taskStorage = storage
        healthMonitor = mockHealthMonitor
    }

    override fun processGetRequest(apiKey: String?, @NonNull resourcePath: String, @NonNull json: JSONObject, authToken: String?, onCallback: IterableHelper.IterableActionHandler?) {
        val request = IterableApiRequest(apiKey ?: "", resourcePath, json, IterableApiRequest.GET, authToken, onCallback)
        IterableRequestTask().execute(request)
    }

    override fun processGetRequest(apiKey: String?, @NonNull resourcePath: String, @NonNull json: JSONObject, authToken: String?, onSuccess: IterableHelper.SuccessHandler?, onFailure: IterableHelper.FailureHandler?) {
        val request = IterableApiRequest(apiKey ?: "", resourcePath, json, IterableApiRequest.GET, authToken, onSuccess, onFailure)
        IterableRequestTask().execute(request)
    }

    override fun processPostRequest(apiKey: String?, @NonNull resourcePath: String, @NonNull json: JSONObject, authToken: String?, onSuccess: IterableHelper.SuccessHandler?, onFailure: IterableHelper.FailureHandler?) {
        val request = IterableApiRequest(apiKey ?: "", resourcePath, json, IterableApiRequest.POST, authToken, onSuccess, onFailure)
        if (isRequestOfflineCompatible(request.resourcePath) && healthMonitor.canSchedule()) {
            request.setProcessorType(IterableApiRequest.ProcessorType.OFFLINE)
            taskScheduler.scheduleTask(request, onSuccess, onFailure)
        } else {
            IterableRequestTask().execute(request)
        }
    }

    override fun onLogout(context: Context) {
        taskStorage.deleteAllTasks()
    }

    fun isRequestOfflineCompatible(baseUrl: String?): Boolean {
        return offlineApiSet.contains(baseUrl)
    }
}

internal class TaskScheduler(
    private val taskStorage: IterableTaskStorage,
    private val taskRunner: IterableTaskRunner
) : IterableTaskRunner.TaskCompletedListener {
    
    companion object {
        @JvmStatic
        val successCallbackMap = HashMap<String, IterableHelper.SuccessHandler?>()
        @JvmStatic
        val failureCallbackMap = HashMap<String, IterableHelper.FailureHandler?>()
    }

    init {
        taskRunner.addTaskCompletedListener(this)
    }

    fun scheduleTask(request: IterableApiRequest, onSuccess: IterableHelper.SuccessHandler?, onFailure: IterableHelper.FailureHandler?) {
        val serializedRequest: JSONObject
        try {
            serializedRequest = request.toJSONObject()
        } catch (e: JSONException) {
            IterableLogger.e("RequestProcessor", "Failed serializing the request for offline execution. Attempting to request the request now...")
            IterableRequestTask().execute(request)
            return
        }

        val taskId = taskStorage.createTask(request.resourcePath, IterableTaskType.API, serializedRequest.toString())
        if (taskId == null) {
            IterableRequestTask().execute(request)
            return
        }
        successCallbackMap[taskId] = onSuccess
        failureCallbackMap[taskId] = onFailure
    }

    @MainThread
    override fun onTaskCompleted(taskId: String, result: IterableTaskRunner.TaskResult, response: IterableApiResponse?) {
        val onSuccess = successCallbackMap[taskId]
        val onFailure = failureCallbackMap[taskId]
        successCallbackMap.remove(taskId)
        failureCallbackMap.remove(taskId)
        if (response?.success == true) {
            onSuccess?.onSuccess(response.responseJson ?: JSONObject())
        } else {
            onFailure?.onFailure(response?.errorMessage ?: "", response?.responseJson)
        }
    }
}
