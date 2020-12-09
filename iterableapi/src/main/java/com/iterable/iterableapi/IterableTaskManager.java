package com.iterable.iterableapi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;

class IterableTaskManager {

    private static IterableTaskManager sharedInstance;

    private static final String TAG = "IterableTaskManager";

    static final String ITERABLE_TASK_TABLE_NAME = "OfflineTask";
    private static final String QUERY_GET_TASK_BY_ID = "select * from OfflineTask where task_id = ?";

    static final String OFFLINE_TASK_COLUMN_DATA = " (" + IterableTask.TASK_ID + " TEXT PRIMARY KEY," +
            IterableTask.NAME + " TEXT," +
            IterableTask.VERSION + " INTEGER," +
            IterableTask.CREATED_AT + " TEXT," +
            IterableTask.MODIFIED_AT + " TEXT," +
            IterableTask.LAST_ATTEMPTED_AT + " TEXT," +
            IterableTask.SCHEDULED_AT + " TEXT," +
            IterableTask.REQUESTED_AT + " TEXT," +
            IterableTask.PROCESSING + " BOOLEAN," +
            IterableTask.FAILED + " BOOLEAN," +
            IterableTask.BLOCKING + " BOOLEAN," +
            IterableTask.DATA + " TEXT," +
            IterableTask.ERROR + " TEXT," +
            IterableTask.TYPE + " TEXT," +
            IterableTask.ATTEMPTS + " INTEGER" + ")";

    private SQLiteDatabase database;
    private IterableDatabaseManager databaseManager;

    private IterableTaskManager(Context context) {
        try {
            if (context == null) {
                return;
            }

            if (databaseManager == null) {
                databaseManager = new IterableDatabaseManager(IterableApi.getInstance().getMainActivityContext());
            }
            database = databaseManager.getWritableDatabase();
        } catch (SQLException e) {
            IterableLogger.e(TAG, "Database cannot be opened for writing");
        }
    }

    static IterableTaskManager sharedInstance(Context context) {
        if (sharedInstance == null) {
            sharedInstance = new IterableTaskManager(context);
        }
        return sharedInstance;
    }

    /**
     * Creates a new instance with default values of IterableTask and stores it in the database
     *
     * @param name Type of the offline task. See {@link IterableTaskType}
     * @return unique id of the task created
     */
    @Nullable
    String createTask(String name, IterableTaskType type, String data) {
        if (!isDatabaseReady()) return null;
        ContentValues contentValues = new ContentValues();
        IterableTask iterableTask = new IterableTask(name, IterableTaskType.API, data);
        contentValues.put(IterableTask.TASK_ID, iterableTask.id);
        contentValues.put(IterableTask.NAME, iterableTask.name);
        contentValues.put(IterableTask.VERSION, iterableTask.version);
        contentValues.put(IterableTask.CREATED_AT, iterableTask.createdAt.toString());
        if (iterableTask.modifiedAt != null) {
            contentValues.put(IterableTask.MODIFIED_AT, iterableTask.modifiedAt.toString());
        }
        if (iterableTask.lastAttemptedAt != null) {
            contentValues.put(IterableTask.LAST_ATTEMPTED_AT, iterableTask.lastAttemptedAt.toString());
        }
        if (iterableTask.scheduledAt != null) {
            contentValues.put(IterableTask.SCHEDULED_AT, iterableTask.scheduledAt.toString());
        }
        if (iterableTask.requestedAt != null) {
            contentValues.put(IterableTask.REQUESTED_AT, iterableTask.requestedAt.toString());
        }
        contentValues.put(IterableTask.PROCESSING, iterableTask.processing);
        contentValues.put(IterableTask.FAILED, iterableTask.failed);
        contentValues.put(IterableTask.BLOCKING, iterableTask.blocking);
        if (iterableTask.data != null) {
            contentValues.put(IterableTask.DATA, iterableTask.data);
        }
        if (iterableTask.taskFailureData != null) {
            contentValues.put(IterableTask.ERROR, iterableTask.taskFailureData);
        }

        contentValues.put(IterableTask.TYPE, iterableTask.taskType.toString());
        contentValues.put(IterableTask.ATTEMPTS, iterableTask.attempts);

        database.insert(ITERABLE_TASK_TABLE_NAME, null, contentValues);
        contentValues.clear();

        return iterableTask.id;
    }

    /**
     * Gets a Task for the task id provided. Returns null if the database is null.
     *
     * @param id Unique id for the task
     * @return {@link IterableTask} corresponding to id provided
     */
    @Nullable
    IterableTask getTask(String id) {
        if (!isDatabaseReady()) {
            return null;
        }

        String name = null;
        IterableTaskType type = null;
        int version = 1;
        int attempts = 0;
        Date dateCreated = null;
        Date dateModified = null, dateLastAttempted = null, dateScheduled = null, dateRequested = null;
        boolean processing = false, failed = false, blocking = false;
        String data = null, error = null;

        Cursor cursor = database.rawQuery(QUERY_GET_TASK_BY_ID, new String[]{id});

        if (!cursor.moveToFirst()) {
            IterableLogger.d(TAG, "No record found");
            return null;
        }
        name = cursor.getString(cursor.getColumnIndex(IterableTask.NAME));
        version = cursor.getInt(cursor.getColumnIndex(IterableTask.VERSION));
        dateCreated = new Date(cursor.getString(cursor.getColumnIndex(IterableTask.CREATED_AT)));
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.MODIFIED_AT))) {
            dateModified = new Date(cursor.getString(cursor.getColumnIndex(IterableTask.MODIFIED_AT)));
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.LAST_ATTEMPTED_AT))) {
            dateLastAttempted = new Date(cursor.getString(cursor.getColumnIndex(IterableTask.LAST_ATTEMPTED_AT)));
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.SCHEDULED_AT))) {
            dateScheduled = new Date(cursor.getString(cursor.getColumnIndex(IterableTask.SCHEDULED_AT)));
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.REQUESTED_AT))) {
            dateRequested = new Date(cursor.getString(cursor.getColumnIndex(IterableTask.REQUESTED_AT)));
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.PROCESSING))) {
            processing = cursor.getInt(cursor.getColumnIndex(IterableTask.PROCESSING)) > 0;
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.FAILED))) {
            failed = cursor.getInt(cursor.getColumnIndex(IterableTask.FAILED)) > 0;
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.BLOCKING))) {
            blocking = cursor.getInt(cursor.getColumnIndex(IterableTask.BLOCKING)) > 0;
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.DATA))) {
            data = cursor.getString(cursor.getColumnIndex(IterableTask.DATA));
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.ERROR))) {
            error = cursor.getString(cursor.getColumnIndex(IterableTask.ERROR));
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.TYPE))) {
            type = IterableTaskType.valueOf(cursor.getString(cursor.getColumnIndex(IterableTask.TYPE)));
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.ATTEMPTS))) {
            attempts = cursor.getInt(cursor.getColumnIndex(IterableTask.ATTEMPTS));
        }

        IterableTask task = new IterableTask(id, name, version, dateCreated, dateModified, dateLastAttempted, dateScheduled, dateRequested, processing, failed, blocking, data, error, type, attempts);
        IterableLogger.v(TAG, "Found " + cursor.getColumnCount() + "columns");
        cursor.close();
        return task;
    }

    /**
     * Gets ids of all the tasks in OfflineTask table
     *
     * @return {@link ArrayList} of {@link String} ids for all the tasks in OfflineTask table
     */
    @NonNull
    ArrayList<String> getAllTaskIds() {
        ArrayList<String> taskIds = new ArrayList<>();
        if (!isDatabaseReady()) return taskIds;

        Cursor cursor = database.rawQuery("SELECT " + IterableTask.TASK_ID +
                        " FROM " + ITERABLE_TASK_TABLE_NAME,
                null);

        if (cursor.moveToFirst()) {
            do {
                taskIds.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        IterableLogger.v(TAG, "Found " + cursor.getColumnCount() + " columns");
        cursor.close();
        return taskIds;
    }

    /**
     * Deletes all the entries from the OfflineTask table.
     */
    void deleteAllTasks() {
        if (!isDatabaseReady()) return;
        int numberOfRowsDeleted = database.delete(ITERABLE_TASK_TABLE_NAME, null, null);
        IterableLogger.v(TAG, "Deleted " + numberOfRowsDeleted + " offline tasks");
    }

    /**
     * Deletes a task from OfflineTask table
     *
     * @param id for the task
     * @return Whether or not the task was deleted
     */
    boolean deleteTask(String id) {
        if (!isDatabaseReady()) return false;
        int numberOfEntriesDeleted = database.delete(ITERABLE_TASK_TABLE_NAME, IterableTask.TASK_ID + " =?", new String[]{id});
        IterableLogger.v(TAG, "Deleted entry - " + numberOfEntriesDeleted);
        return true;
    }

    /**
     * Updates Modified at date for a task in OfflineTask table
     *
     * @param id   Unique id for the task
     * @param date Date when the task was modified
     * @return Whether or not the task was updated
     */
    boolean updateModifiedAt(String id, Date date) {
        if (!isDatabaseReady()) return false;
        ContentValues contentValues = new ContentValues();
        contentValues.put(IterableTask.MODIFIED_AT, date.toString());
        return updateTaskWithContentValues(id, contentValues);
    }

    /**
     * Updates Last attempted date for a task in OfflineTask table
     *
     * @param id   Unique id for the task
     * @param date Date when the task was last attempted
     * @return Whether or not the task was updated
     */
    boolean updateLastAttemptedAt(String id, Date date) {
        if (!isDatabaseReady()) return false;
        ContentValues contentValues = new ContentValues();
        contentValues.put(IterableTask.LAST_ATTEMPTED_AT, date.toString());
        return updateTaskWithContentValues(id, contentValues);
    }

    /**
     * Updates Requested at date for a task in OfflineTask table
     *
     * @param id   Unique id for the task
     * @param date Date when the task was last requested
     * @return Whether or not the task was updated
     */
    boolean updateRequestedAt(String id, Date date) {
        if (!isDatabaseReady()) return false;
        ContentValues contentValues = new ContentValues();
        contentValues.put(IterableTask.REQUESTED_AT, date.toString());
        return updateTaskWithContentValues(id, contentValues);
    }

    /**
     * Updates Scheduled at date for a task in OfflineTask table
     *
     * @param id   Unique id for the task
     * @param date Date when the task is Scheduled
     * @return Whether or not the task was updated
     */
    boolean updateScheduledAt(String id, Date date) {
        if (isDatabaseReady()) return false;
        ContentValues contentValues = new ContentValues();
        contentValues.put(IterableTask.SCHEDULED_AT, date.toString());
        return updateTaskWithContentValues(id, contentValues);
    }

    /**
     * Updates the processing state of task in OfflineTask table
     *
     * @param id    Unique id for the task
     * @param state whether the task is processing or completed
     * @return Whether or not the task was updated
     */
    boolean updateIsProcessing(String id, Boolean state) {
        if (!isDatabaseReady()) return false;
        ContentValues contentValues = new ContentValues();

        contentValues.put(IterableTask.PROCESSING, state);
        return updateTaskWithContentValues(id, contentValues);
    }

    /**
     * Updates the failed state of task in OfflineTask table
     *
     * @param id    Unique id for the task
     * @param state whether the task failed
     * @return Whether or not the task was updated
     */
    boolean updateHasFailed(String id, boolean state) {
        if (!isDatabaseReady()) return false;
        ContentValues contentValues = new ContentValues();
        contentValues.put(IterableTask.FAILED, state);
        return updateTaskWithContentValues(id, contentValues);
    }

    /**
     * Updates Number of attempts for a task in OfflineTask table
     *
     * @param id      Unique id for the task
     * @param attempt number of times the task has been executed
     * @return Whether or not the task was updated
     */
    boolean incrementAttempts(String id, int attempt) {
        if (!isDatabaseReady()) return false;
        ContentValues contentValues = new ContentValues();
        contentValues.put(IterableTask.ATTEMPTS, attempt);
        return updateTaskWithContentValues(id, contentValues);
    }

    /**
     * Increments number of attempts made by a task in OfflineTask table
     *
     * @param id Unique id for the task
     * @return Whether or not the task was updated
     */
    boolean incrementAttempts(String id) {
        if (!isDatabaseReady()) return false;
        IterableTask task = getTask(id);
        if (task == null) {
            IterableLogger.e(TAG, "No task found for id " + id);
            return false;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put(IterableTask.ATTEMPTS, task.attempts + 1);
        return updateTaskWithContentValues(id, contentValues);
    }

    /**
     * Updates task with error data OfflineTask table
     *
     * @param id        Unique id for the task
     * @param errorData error received after processing the task
     * @return Whether or not the task was updated
     */
    boolean updateError(String id, String errorData) {
        if (!isDatabaseReady()) return false;
        ContentValues contentValues = new ContentValues();
        contentValues.put(IterableTask.ERROR, errorData);
        return updateTaskWithContentValues(id, contentValues);
    }

    /**
     * Updates data for a task in OfflineTask table
     *
     * @param id   Unique id for the task
     * @param data required for the task. JSONObject converted to string
     * @return Whether or not the task was updated
     */
    boolean updateData(String id, String data) {
        if (!isDatabaseReady()) return false;
        ContentValues contentValues = new ContentValues();
        contentValues.put(IterableTask.DATA, data);
        return updateTaskWithContentValues(id, contentValues);
    }

    private boolean updateTaskWithContentValues(String id, ContentValues contentValues) {
        return (0 > database.update(ITERABLE_TASK_TABLE_NAME, contentValues, IterableTask.TASK_ID + "=?", new String[]{id}));
    }

    private boolean isDatabaseReady() {
        if (database == null) {
            IterableLogger.e(TAG, "Database not initialized");
            return false;
        }
        if (!database.isOpen()) {
            IterableLogger.e(TAG, "Database is closed");
            return false;
        }
        return true;
    }

}
