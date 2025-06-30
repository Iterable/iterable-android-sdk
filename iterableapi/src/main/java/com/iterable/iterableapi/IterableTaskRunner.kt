package com.iterable.iterableapi

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import androidx.annotation.MainThread
import androidx.annotation.NonNull
import androidx.annotation.WorkerThread
import org.json.JSONException
import org.json.JSONObject

internal class IterableTaskRunner(
    private val taskStorage: IterableTaskStorage,
    private val activityMonitor: IterableActivityMonitor,
    private val networkConnectivityManager: IterableNetworkConnectivityManager,
    private val healthMonitor: HealthMonitor
) : IterableTaskStorage.TaskCreatedListener, 
    Handler.Callback, 
    IterableNetworkConnectivityManager.IterableNetworkMonitorListener, 
    IterableActivityMonitor.AppStateCallback {

    companion object {
        private const val TAG = "IterableTaskRunner"
        private const val RETRY_INTERVAL_SECONDS = 60
        private const val OPERATION_PROCESS_TASKS = 100
    }

    private val networkThread = HandlerThread("NetworkThread")
    private lateinit var handler: Handler
    private val taskCompletedListeners: MutableList<TaskCompletedListener> = ArrayList()

    enum class TaskResult {
        SUCCESS, FAILURE, RETRY
    }

    interface TaskCompletedListener {
        @MainThread
        fun onTaskCompleted(taskId: String, result: TaskResult, response: IterableApiResponse?)
    }

    init {
        networkThread.start()
        handler = Handler(networkThread.looper, this)
        taskStorage.addTaskCreatedListener(this)
        networkConnectivityManager.addNetworkListener(this)
        activityMonitor.addCallback(this)
    }

    fun addTaskCompletedListener(listener: TaskCompletedListener) {
        taskCompletedListeners.add(listener)
    }

    fun removeTaskCompletedListener(listener: TaskCompletedListener) {
        taskCompletedListeners.remove(listener)
    }

    override fun onTaskCreated(iterableTask: IterableTask) {
        runNow()
    }

    override fun onNetworkConnected() {
        runNow()
    }

    override fun onNetworkDisconnected() {
        // Empty implementation
    }

    override fun onSwitchToForeground() {
        runNow()
    }

    override fun onSwitchToBackground() {
        // Empty implementation
    }

    private fun runNow() {
        handler.removeMessages(OPERATION_PROCESS_TASKS)
        handler.sendEmptyMessage(OPERATION_PROCESS_TASKS)
    }

    private fun scheduleRetry() {
        handler.removeCallbacksAndMessages(OPERATION_PROCESS_TASKS)
        handler.sendEmptyMessageDelayed(OPERATION_PROCESS_TASKS, (RETRY_INTERVAL_SECONDS * 1000).toLong())
    }

    @WorkerThread
    override fun handleMessage(@NonNull msg: Message): Boolean {
        if (msg.what == OPERATION_PROCESS_TASKS) {
            processTasks()
            return true
        }
        return false
    }

    @WorkerThread
    private fun processTasks() {
        if (!activityMonitor.isInForeground()) {
            IterableLogger.d(TAG, "App not in foreground, skipping processing tasks")
            return
        }

        if (!healthMonitor.canProcess()) {
            return
        }

        while (networkConnectivityManager.isConnected()) {
            val task = taskStorage.getNextScheduledTask() ?: return

            val proceed = processTask(task)
            if (!proceed) {
                scheduleRetry()
                return
            }
        }
    }

    @WorkerThread
    private fun processTask(@NonNull task: IterableTask): Boolean {
        if (task.taskType == IterableTaskType.API) {
            var response: IterableApiResponse? = null
            var result = TaskResult.FAILURE
            try {
                val taskData = getTaskDataWithDate(task)
                if (taskData != null) {
                    val request = IterableApiRequest.fromJSON(taskData, null, null)
                    request?.setProcessorType(IterableApiRequest.ProcessorType.OFFLINE)
                    if (request != null) {
                        response = IterableRequestTask.executeApiRequest(request)
                    }
                }
            } catch (e: Exception) {
                IterableLogger.e(TAG, "Error while processing request task", e)
                healthMonitor.onDBError()
            }

            if (response != null) {
                if (response.success) {
                    result = TaskResult.SUCCESS
                } else {
                    val errorMsg = response.errorMessage ?: ""
                    if (isRetriableError(errorMsg)) {
                        result = TaskResult.RETRY
                    } else {
                        result = TaskResult.FAILURE
                    }
                }
            }
            val taskId = task.id ?: ""
            callTaskCompletedListeners(taskId, result, response)
            if (result == TaskResult.RETRY) {
                // Keep the task, stop further processing
                return false
            } else {
                taskStorage.deleteTask(taskId)
                return true
            }
        }
        return false
    }

    fun getTaskDataWithDate(task: IterableTask): JSONObject? {
        return try {
            val jsonData = JSONObject(task.data)
            jsonData.getJSONObject("data").put(IterableConstants.KEY_CREATED_AT, task.createdAt / 1000)
            jsonData
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        }
    }

    private fun isRetriableError(errorMessage: String): Boolean {
        return errorMessage.contains("failed to connect")
    }

    @WorkerThread
    private fun callTaskCompletedListeners(taskId: String, result: TaskResult, response: IterableApiResponse?) {
        for (listener in taskCompletedListeners) {
            Handler(Looper.getMainLooper()).post {
                listener.onTaskCompleted(taskId, result, response)
            }
        }
    }
}