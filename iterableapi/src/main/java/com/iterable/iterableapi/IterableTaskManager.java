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

    //String columns as stored in DB
    static final String TASK_ID = "task_id";
    static final String NAME = "name";
    static final String ATTEMPTS = "attempts";
    static final String TYPE = "type";
    static final String ERROR = "error";
    static final String DATA = "data";
    static final String BLOCKING = "blocking";
    static final String FAILED = "failed";
    static final String PROCESSING = "processing";
    static final String REQUESTED_AT = "requested";
    static final String SCHEDULED_AT = "scheduled";
    static final String LAST_ATTEMPTED_AT = "last_attempt";
    static final String MODIFIED_AT = "modified";
    static final String CREATED_AT = "created";
    static final String VERSION = "version";

    static final String OFFLINE_TASK_COLUMN_DATA = " (" + TASK_ID + " TEXT PRIMARY KEY," +
            NAME + " TEXT," +
            VERSION + " INTEGER," +
            CREATED_AT + " TEXT," +
            MODIFIED_AT + " TEXT," +
            LAST_ATTEMPTED_AT + " TEXT," +
            SCHEDULED_AT + " TEXT," +
            REQUESTED_AT + " TEXT," +
            PROCESSING + " BOOLEAN," +
            FAILED + " BOOLEAN," +
            BLOCKING + " BOOLEAN," +
            DATA + " TEXT," +
            ERROR + " TEXT," +
            TYPE + " TEXT," +
            ATTEMPTS + " INTEGER" + ")";

    private static final String QUERY_GET_TASK_BY_ID = "select * from OfflineTask where task_id = ?";

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
        if (!isDatabaseReady()) {
            return null;
        }
        ContentValues contentValues = new ContentValues();
        IterableTask iterableTask = new IterableTask(name, IterableTaskType.API, data);
        contentValues.put(TASK_ID, iterableTask.id);
        contentValues.put(NAME, iterableTask.name);
        contentValues.put(VERSION, iterableTask.version);
        contentValues.put(CREATED_AT, iterableTask.createdAt.toString());
        if (iterableTask.modifiedAt != null) {
            contentValues.put(MODIFIED_AT, iterableTask.modifiedAt.toString());
        }
        if (iterableTask.lastAttemptedAt != null) {
            contentValues.put(LAST_ATTEMPTED_AT, iterableTask.lastAttemptedAt.toString());
        }
        if (iterableTask.scheduledAt != null) {
            contentValues.put(SCHEDULED_AT, iterableTask.scheduledAt.toString());
        }
        if (iterableTask.requestedAt != null) {
            contentValues.put(REQUESTED_AT, iterableTask.requestedAt.toString());
        }
        contentValues.put(PROCESSING, iterableTask.processing);
        contentValues.put(FAILED, iterableTask.failed);
        contentValues.put(BLOCKING, iterableTask.blocking);
        if (iterableTask.data != null) {
            contentValues.put(DATA, iterableTask.data);
        }
        if (iterableTask.taskFailureData != null) {
            contentValues.put(ERROR, iterableTask.taskFailureData);
        }

        contentValues.put(TYPE, iterableTask.taskType.toString());
        contentValues.put(ATTEMPTS, iterableTask.attempts);

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
        name = cursor.getString(cursor.getColumnIndex(NAME));
        version = cursor.getInt(cursor.getColumnIndex(VERSION));
        dateCreated = new Date(cursor.getString(cursor.getColumnIndex(CREATED_AT)));
        if (!cursor.isNull(cursor.getColumnIndex(MODIFIED_AT))) {
            dateModified = new Date(cursor.getString(cursor.getColumnIndex(MODIFIED_AT)));
        }
        if (!cursor.isNull(cursor.getColumnIndex(LAST_ATTEMPTED_AT))) {
            dateLastAttempted = new Date(cursor.getString(cursor.getColumnIndex(LAST_ATTEMPTED_AT)));
        }
        if (!cursor.isNull(cursor.getColumnIndex(SCHEDULED_AT))) {
            dateScheduled = new Date(cursor.getString(cursor.getColumnIndex(SCHEDULED_AT)));
        }
        if (!cursor.isNull(cursor.getColumnIndex(REQUESTED_AT))) {
            dateRequested = new Date(cursor.getString(cursor.getColumnIndex(REQUESTED_AT)));
        }
        if (!cursor.isNull(cursor.getColumnIndex(PROCESSING))) {
            processing = cursor.getInt(cursor.getColumnIndex(PROCESSING)) > 0;
        }
        if (!cursor.isNull(cursor.getColumnIndex(FAILED))) {
            failed = cursor.getInt(cursor.getColumnIndex(FAILED)) > 0;
        }
        if (!cursor.isNull(cursor.getColumnIndex(BLOCKING))) {
            blocking = cursor.getInt(cursor.getColumnIndex(BLOCKING)) > 0;
        }
        if (!cursor.isNull(cursor.getColumnIndex(DATA))) {
            data = cursor.getString(cursor.getColumnIndex(DATA));
        }
        if (!cursor.isNull(cursor.getColumnIndex(ERROR))) {
            error = cursor.getString(cursor.getColumnIndex(ERROR));
        }
        if (!cursor.isNull(cursor.getColumnIndex(TYPE))) {
            type = IterableTaskType.valueOf(cursor.getString(cursor.getColumnIndex(TYPE)));
        }
        if (!cursor.isNull(cursor.getColumnIndex(ATTEMPTS))) {
            attempts = cursor.getInt(cursor.getColumnIndex(ATTEMPTS));
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

        Cursor cursor = database.rawQuery("SELECT " + TASK_ID +
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
        int numberOfEntriesDeleted = database.delete(ITERABLE_TASK_TABLE_NAME, TASK_ID + " =?", new String[]{id});
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
        contentValues.put(MODIFIED_AT, date.toString());
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
        contentValues.put(LAST_ATTEMPTED_AT, date.toString());
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
        contentValues.put(REQUESTED_AT, date.toString());
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
        contentValues.put(SCHEDULED_AT, date.toString());
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

        contentValues.put(PROCESSING, state);
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
        contentValues.put(FAILED, state);
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
        contentValues.put(ATTEMPTS, attempt);
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
        contentValues.put(ATTEMPTS, task.attempts + 1);
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
        contentValues.put(ERROR, errorData);
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
        contentValues.put(DATA, data);
        return updateTaskWithContentValues(id, contentValues);
    }

    private boolean updateTaskWithContentValues(String id, ContentValues contentValues) {
        return (0 > database.update(ITERABLE_TASK_TABLE_NAME, contentValues, TASK_ID + "=?", new String[]{id}));
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
