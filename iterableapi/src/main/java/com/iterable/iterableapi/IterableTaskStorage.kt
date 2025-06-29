package com.iterable.iterableapi

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import java.util.*

internal class IterableTaskStorage private constructor(context: Context?) {

    private var database: SQLiteDatabase? = null
    private var databaseManager: IterableDatabaseManager? = null

    interface TaskCreatedListener {
        fun onTaskCreated(iterableTask: IterableTask)
    }

    private val taskCreatedListeners: MutableList<TaskCreatedListener> = ArrayList()

    interface IterableDatabaseStatusListeners {
        fun onDBError()
        fun isReady()
    }

    private val databaseStatusListeners: MutableList<IterableDatabaseStatusListeners> = ArrayList()

    init {
        try {
            if (context != null) {
                if (databaseManager == null) {
                    databaseManager = IterableDatabaseManager(context)
                }
                database = databaseManager!!.writableDatabase
            }
        } catch (e: SQLException) {
            IterableLogger.e(TAG, "Database cannot be opened for writing")
        }
    }

    fun addTaskCreatedListener(listener: TaskCreatedListener) {
        taskCreatedListeners.add(listener)
    }

    fun removeDatabaseStatusListener(listener: TaskCreatedListener) {
        taskCreatedListeners.remove(listener)
    }

    /**
     * Creates a new instance with default values of IterableTask and stores it in the database
     *
     * @param name Type of the offline task. See [IterableTaskType]
     * @return unique id of the task created
     */
    @Nullable
    fun createTask(name: String, type: IterableTaskType, data: String): String? {
        if (!isDatabaseReady()) {
            return null
        }
        val contentValues = ContentValues()
        val iterableTask = IterableTask(name, IterableTaskType.API, data)
        contentValues.put(TASK_ID, iterableTask.id)
        contentValues.put(NAME, iterableTask.name)
        contentValues.put(VERSION, iterableTask.version)
        contentValues.put(CREATED_AT, iterableTask.createdAt)
        if (iterableTask.modifiedAt != 0L) {
            contentValues.put(MODIFIED_AT, iterableTask.modifiedAt)
        }
        if (iterableTask.lastAttemptedAt != 0L) {
            contentValues.put(LAST_ATTEMPTED_AT, iterableTask.lastAttemptedAt)
        }
        if (iterableTask.scheduledAt != 0L) {
            contentValues.put(SCHEDULED_AT, iterableTask.scheduledAt)
        }
        if (iterableTask.requestedAt != 0L) {
            contentValues.put(REQUESTED_AT, iterableTask.requestedAt)
        }
        contentValues.put(PROCESSING, iterableTask.processing)
        contentValues.put(FAILED, iterableTask.failed)
        contentValues.put(BLOCKING, iterableTask.blocking)
        if (iterableTask.data != null) {
            contentValues.put(DATA, iterableTask.data)
        }
        if (iterableTask.taskFailureData != null) {
            contentValues.put(ERROR, iterableTask.taskFailureData)
        }

        contentValues.put(TYPE, iterableTask.taskType.toString())
        contentValues.put(ATTEMPTS, iterableTask.attempts)

        val rowId = database!!.insert(ITERABLE_TASK_TABLE_NAME, null, contentValues)
        if (rowId == -1L) {
            notifyDBError()
            return null
        }
        contentValues.clear()

        // Call through Handler to make sure we don't call the listeners immediately, as the caller may need additional processing
        Handler(Looper.getMainLooper()).post {
            for (listener in taskCreatedListeners) {
                listener.onTaskCreated(iterableTask)
            }
        }

        return iterableTask.id
    }

    /**
     * Gets a Task for the task id provided. Returns null if the database is null.
     *
     * @param id Unique id for the task
     * @return [IterableTask] corresponding to id provided
     */
    @Nullable
    fun getTask(id: String): IterableTask? {
        if (!isDatabaseReady()) {
            return null
        }
        val cursor = database!!.rawQuery(QUERY_GET_TASK_BY_ID, arrayOf(id))

        if (!cursor.moveToFirst()) {
            IterableLogger.d(TAG, "No record found")
            return null
        }

        val task = createTaskFromCursor(cursor)

        IterableLogger.v(TAG, "Found " + cursor.columnCount + "columns")
        cursor.close()
        return task
    }

    @SuppressLint("Range")
    private fun createTaskFromCursor(cursor: Cursor): IterableTask {
        val id: String
        val name: String
        var type: IterableTaskType? = null
        var version = 1
        var attempts = 0
        var dateCreated: Long = 0
        var dateModified: Long = 0
        var dateLastAttempted: Long = 0
        var dateScheduled: Long = 0
        var dateRequested: Long = 0
        var processing = false
        var failed = false
        var blocking = false
        var data: String? = null
        var error: String? = null

        id = cursor.getString(cursor.getColumnIndex(TASK_ID))
        name = cursor.getString(cursor.getColumnIndex(NAME))
        version = cursor.getInt(cursor.getColumnIndex(VERSION))
        dateCreated = cursor.getLong(cursor.getColumnIndex(CREATED_AT))
        if (!cursor.isNull(cursor.getColumnIndex(MODIFIED_AT))) {
            dateModified = cursor.getLong(cursor.getColumnIndex(MODIFIED_AT))
        }
        if (!cursor.isNull(cursor.getColumnIndex(LAST_ATTEMPTED_AT))) {
            dateLastAttempted = cursor.getLong(cursor.getColumnIndex(LAST_ATTEMPTED_AT))
        }
        if (!cursor.isNull(cursor.getColumnIndex(SCHEDULED_AT))) {
            dateScheduled = cursor.getLong(cursor.getColumnIndex(SCHEDULED_AT))
        }
        if (!cursor.isNull(cursor.getColumnIndex(REQUESTED_AT))) {
            dateRequested = cursor.getLong(cursor.getColumnIndex(REQUESTED_AT))
        }
        if (!cursor.isNull(cursor.getColumnIndex(PROCESSING))) {
            processing = cursor.getInt(cursor.getColumnIndex(PROCESSING)) > 0
        }
        if (!cursor.isNull(cursor.getColumnIndex(FAILED))) {
            failed = cursor.getInt(cursor.getColumnIndex(FAILED)) > 0
        }
        if (!cursor.isNull(cursor.getColumnIndex(BLOCKING))) {
            blocking = cursor.getInt(cursor.getColumnIndex(BLOCKING)) > 0
        }
        if (!cursor.isNull(cursor.getColumnIndex(DATA))) {
            data = cursor.getString(cursor.getColumnIndex(DATA))
        }
        if (!cursor.isNull(cursor.getColumnIndex(ERROR))) {
            error = cursor.getString(cursor.getColumnIndex(ERROR))
        }
        if (!cursor.isNull(cursor.getColumnIndex(TYPE))) {
            type = IterableTaskType.valueOf(cursor.getString(cursor.getColumnIndex(TYPE)))
        }
        if (!cursor.isNull(cursor.getColumnIndex(ATTEMPTS))) {
            attempts = cursor.getInt(cursor.getColumnIndex(ATTEMPTS))
        }

        return IterableTask(id, name, version, dateCreated, dateModified, dateLastAttempted, dateScheduled, dateRequested, processing, failed, blocking, data, error, type, attempts)
    }

    /**
     * Gets ids of all the tasks in OfflineTask table
     * @return [ArrayList] of [String] ids for all the tasks in OfflineTask table
     */
    @NonNull
    fun getAllTaskIds(): ArrayList<String> {
        val taskIds = ArrayList<String>()
        if (!isDatabaseReady()) {
            return taskIds
        }

        val cursor = database!!.rawQuery(
            "SELECT $TASK_ID FROM $ITERABLE_TASK_TABLE_NAME",
            null
        )

        if (cursor.moveToFirst()) {
            do {
                taskIds.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        IterableLogger.v(TAG, "Found " + cursor.columnCount + " columns")
        cursor.close()
        return taskIds
    }

    @Throws(IllegalStateException::class)
    fun getNumberOfTasks(): Long {
        if (!isDatabaseReady()) {
            throw IllegalStateException("Database is not ready")
        }
        return DatabaseUtils.queryNumEntries(database, ITERABLE_TASK_TABLE_NAME)
    }

    /**
     * Returns the next scheduled task for processing
     *
     * @return next scheduled [IterableTask]
     */
    @Nullable
    fun getNextScheduledTask(): IterableTask? {
        if (!isDatabaseReady()) {
            return null
        }
        val cursor = database!!.rawQuery("select * from OfflineTask order by scheduled limit 1", null)
        var task: IterableTask? = null
        if (cursor.moveToFirst()) {
            task = createTaskFromCursor(cursor)
        }
        cursor.close()
        return task
    }

    /**
     * Deletes all the entries from the OfflineTask table.
     */
    fun deleteAllTasks() {
        if (!isDatabaseReady()) {
            return
        }
        val numberOfRowsDeleted = database!!.delete(ITERABLE_TASK_TABLE_NAME, null, null)
        IterableLogger.v(TAG, "Deleted $numberOfRowsDeleted offline tasks")
    }

    /**
     * Deletes a task from OfflineTask table
     *
     * @param id for the task
     * @return Whether or not the task was deleted
     */
    fun deleteTask(id: String): Boolean {
        if (!isDatabaseReady()) return false
        val numberOfEntriesDeleted = database!!.delete(ITERABLE_TASK_TABLE_NAME, "$TASK_ID =?", arrayOf(id))
        IterableLogger.v(TAG, "Deleted entry - $numberOfEntriesDeleted")
        return true
    }

    /**
     * Updates Modified at date for a task in OfflineTask table
     *
     * @param id   Unique id for the task
     * @param date Date when the task was modified
     * @return Whether or not the task was updated
     */
    fun updateModifiedAt(id: String, date: Date): Boolean {
        if (!isDatabaseReady()) return false
        val contentValues = ContentValues()
        contentValues.put(MODIFIED_AT, date.toString())
        return updateTaskWithContentValues(id, contentValues)
    }

    /**
     * Updates Last attempted date for a task in OfflineTask table
     *
     * @param id   Unique id for the task
     * @param date Date when the task was last attempted
     * @return Whether or not the task was updated
     */
    fun updateLastAttemptedAt(id: String, date: Date): Boolean {
        if (!isDatabaseReady()) return false
        val contentValues = ContentValues()
        contentValues.put(LAST_ATTEMPTED_AT, date.toString())
        return updateTaskWithContentValues(id, contentValues)
    }

    /**
     * Updates Requested at date for a task in OfflineTask table
     *
     * @param id   Unique id for the task
     * @param date Date when the task was last requested
     * @return Whether or not the task was updated
     */
    fun updateRequestedAt(id: String, date: Date): Boolean {
        if (!isDatabaseReady()) return false
        val contentValues = ContentValues()
        contentValues.put(REQUESTED_AT, date.toString())
        return updateTaskWithContentValues(id, contentValues)
    }

    /**
     * Updates Scheduled at date for a task in OfflineTask table
     *
     * @param id   Unique id for the task
     * @param date Date when the task is Scheduled
     * @return Whether or not the task was updated
     */
    fun updateScheduledAt(id: String, date: Date): Boolean {
        if (isDatabaseReady()) return false
        val contentValues = ContentValues()
        contentValues.put(SCHEDULED_AT, date.toString())
        return updateTaskWithContentValues(id, contentValues)
    }

    /**
     * Updates the processing state of task in OfflineTask table
     *
     * @param id    Unique id for the task
     * @param state whether the task is processing or completed
     * @return Whether or not the task was updated
     */
    fun updateIsProcessing(id: String, state: Boolean): Boolean {
        if (!isDatabaseReady()) return false
        val contentValues = ContentValues()

        contentValues.put(PROCESSING, state)
        return updateTaskWithContentValues(id, contentValues)
    }

    /**
     * Updates the failed state of task in OfflineTask table
     *
     * @param id    Unique id for the task
     * @param state whether the task failed
     * @return Whether or not the task was updated
     */
    fun updateHasFailed(id: String, state: Boolean): Boolean {
        if (!isDatabaseReady()) return false
        val contentValues = ContentValues()
        contentValues.put(FAILED, state)
        return updateTaskWithContentValues(id, contentValues)
    }

    /**
     * Updates Number of attempts for a task in OfflineTask table
     *
     * @param id      Unique id for the task
     * @param attempt number of times the task has been executed
     * @return Whether or not the task was updated
     */
    fun incrementAttempts(id: String, attempt: Int): Boolean {
        if (!isDatabaseReady()) return false
        val contentValues = ContentValues()
        contentValues.put(ATTEMPTS, attempt)
        return updateTaskWithContentValues(id, contentValues)
    }

    /**
     * Increments number of attempts made by a task in OfflineTask table
     *
     * @param id Unique id for the task
     * @return Whether or not the task was updated
     */
    fun incrementAttempts(id: String): Boolean {
        if (!isDatabaseReady()) return false
        val task = getTask(id)
        if (task == null) {
            IterableLogger.e(TAG, "No task found for id $id")
            return false
        }
        val contentValues = ContentValues()
        contentValues.put(ATTEMPTS, task.attempts + 1)
        return updateTaskWithContentValues(id, contentValues)
    }

    /**
     * Updates task with error data OfflineTask table
     *
     * @param id        Unique id for the task
     * @param errorData error received after processing the task
     * @return Whether or not the task was updated
     */
    fun updateError(id: String, errorData: String): Boolean {
        if (!isDatabaseReady()) return false
        val contentValues = ContentValues()
        contentValues.put(ERROR, errorData)
        return updateTaskWithContentValues(id, contentValues)
    }

    /**
     * Updates data for a task in OfflineTask table
     *
     * @param id   Unique id for the task
     * @param data required for the task. JSONObject converted to string
     * @return Whether or not the task was updated
     */
    fun updateData(id: String, data: String): Boolean {
        if (!isDatabaseReady()) return false
        val contentValues = ContentValues()
        contentValues.put(DATA, data)
        return updateTaskWithContentValues(id, contentValues)
    }

    private fun updateTaskWithContentValues(id: String, contentValues: ContentValues): Boolean {
        return 0 > database!!.update(ITERABLE_TASK_TABLE_NAME, contentValues, "$TASK_ID=?", arrayOf(id))
    }

    private fun isDatabaseReady(): Boolean {
        if (database == null || !database!!.isOpen) {
            notifyDBError()
            IterableLogger.e(TAG, "Database not initialized or is closed")
            return false
        }
        return true
    }

    fun addDatabaseStatusListener(listener: IterableDatabaseStatusListeners) {
        if (isDatabaseReady()) {
            listener.isReady()
        } else {
            listener.onDBError()
        }
        databaseStatusListeners.add(listener)
    }

    fun removeDatabaseStatusListener(listener: IterableDatabaseStatusListeners) {
        databaseStatusListeners.remove(listener)
    }

    private fun notifyDBError() {
        Handler(Looper.getMainLooper()).post {
            for (listener in databaseStatusListeners) {
                listener.onDBError()
            }
        }
    }

    companion object {
        private var sharedInstance: IterableTaskStorage? = null

        private const val TAG = "IterableTaskStorage"

        const val ITERABLE_TASK_TABLE_NAME = "OfflineTask"

        //String columns as stored in DB
        const val TASK_ID = "task_id"
        const val NAME = "name"
        const val ATTEMPTS = "attempts"
        const val TYPE = "type"
        const val ERROR = "error"
        const val DATA = "data"
        const val BLOCKING = "blocking"
        const val FAILED = "failed"
        const val PROCESSING = "processing"
        const val REQUESTED_AT = "requested"
        const val SCHEDULED_AT = "scheduled"
        const val LAST_ATTEMPTED_AT = "last_attempt"
        const val MODIFIED_AT = "modified"
        const val CREATED_AT = "created"
        const val VERSION = "version"

        const val OFFLINE_TASK_COLUMN_DATA = " (" + TASK_ID + " TEXT PRIMARY KEY," +
                NAME + " TEXT," +
                VERSION + " INTEGER," +
                CREATED_AT + " BIGINT," +
                MODIFIED_AT + " BIGINT," +
                LAST_ATTEMPTED_AT + " BIGINT," +
                SCHEDULED_AT + " BIGINT," +
                REQUESTED_AT + " BIGINT," +
                PROCESSING + " BOOLEAN," +
                FAILED + " BOOLEAN," +
                BLOCKING + " BOOLEAN," +
                DATA + " TEXT," +
                ERROR + " TEXT," +
                TYPE + " TEXT," +
                ATTEMPTS + " INTEGER" + ")"

        private const val QUERY_GET_TASK_BY_ID = "select * from OfflineTask where task_id = ?"

        @JvmStatic
        fun sharedInstance(context: Context): IterableTaskStorage {
            if (sharedInstance == null) {
                sharedInstance = IterableTaskStorage(context)
            }
            return sharedInstance!!
        }
    }
}