package com.iterable.iterableapi

internal class HealthMonitor(
    private val iterableTaskStorage: IterableTaskStorage
) : IterableTaskStorage.IterableDatabaseStatusListeners {
    
    companion object {
        private const val TAG = "HealthMonitor"
    }

    private var databaseErrored = false

    init {
        iterableTaskStorage.addDatabaseStatusListener(this)
    }

    fun canSchedule(): Boolean {
        IterableLogger.d(TAG, "canSchedule")
        return try {
            !(iterableTaskStorage.getNumberOfTasks() >= IterableConstants.OFFLINE_TASKS_LIMIT)
        } catch (e: IllegalStateException) {
            IterableLogger.e(TAG, e.localizedMessage)
            databaseErrored = true
            false
        }
    }

    fun canProcess(): Boolean {
        IterableLogger.d(TAG, "Health monitor can process: ${!databaseErrored}")
        return !databaseErrored
    }

    override fun onDBError() {
        IterableLogger.e(TAG, "DB Error notified to healthMonitor")
        databaseErrored = true
    }

    override fun isReady() {
        IterableLogger.v(TAG, "DB Ready notified to healthMonitor")
        databaseErrored = false
    }
}
