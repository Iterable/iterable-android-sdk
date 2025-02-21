package com.iterable.iterableapi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;

class IterableTaskStorage {

    private static IterableTaskStorage sharedInstance;

    private static final String TAG = "IterableTaskStorage";

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
            ATTEMPTS + " INTEGER" + ")";

    private static final String QUERY_GET_TASK_BY_ID = "select * from OfflineTask where task_id = ?";

    private SQLiteDatabase database;
    private IterableDatabaseManager databaseManager;

    interface TaskCreatedListener {
        void onTaskCreated(IterableTask iterableTask);
    }

    private ArrayList<TaskCreatedListener> taskCreatedListeners = new ArrayList<>();

    private IterableTaskStorage(Context context) {
        try {
            if (context == null) {
                return;
            }

            if (databaseManager == null) {
                databaseManager = new IterableDatabaseManager(context);
            }
            database = databaseManager.getWritableDatabase();
        } catch (SQLException e) {
            IterableLogger.e(TAG, "Database cannot be opened for writing");
        }
    }

    static IterableTaskStorage sharedInstance(Context context) {
        if (sharedInstance == null) {
            sharedInstance = new IterableTaskStorage(context);
        }
        return sharedInstance;
    }

    void addTaskCreatedListener(TaskCreatedListener listener) {
        taskCreatedListeners.add(listener);
    }

    void removeDatabaseStatusListener(TaskCreatedListener listener) {
        taskCreatedListeners.remove(listener);
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
        final IterableTask iterableTask = new IterableTask(name, IterableTaskType.API, data);
        contentValues.put(TASK_ID, iterableTask.id);
        contentValues.put(NAME, iterableTask.name);
        contentValues.put(VERSION, iterableTask.version);
        contentValues.put(CREATED_AT, iterableTask.createdAt);
        if (iterableTask.modifiedAt != 0) {
            contentValues.put(MODIFIED_AT, iterableTask.modifiedAt);
        }
        if (iterableTask.lastAttemptedAt != 0) {
            contentValues.put(LAST_ATTEMPTED_AT, iterableTask.lastAttemptedAt);
        }
        if (iterableTask.scheduledAt != 0) {
            contentValues.put(SCHEDULED_AT, iterableTask.scheduledAt);
        }
        if (iterableTask.requestedAt != 0) {
            contentValues.put(REQUESTED_AT, iterableTask.requestedAt);
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

        long rowId = database.insert(ITERABLE_TASK_TABLE_NAME, null, contentValues);
        if (rowId == -1) {
            notifyDBError();
            return null;
        }
        contentValues.clear();

        // Call through Handler to make sure we don't call the listeners immediately, as the caller may need additional processing
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (TaskCreatedListener listener : taskCreatedListeners) {
                    listener.onTaskCreated(iterableTask);
                }
            }
        });

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
        Cursor cursor = database.rawQuery(QUERY_GET_TASK_BY_ID, new String[]{id});

        if (!cursor.moveToFirst()) {
            IterableLogger.d(TAG, "No record found");
            return null;
        }

        IterableTask task = createTaskFromCursor(cursor);

        IterableLogger.v(TAG, "Found " + cursor.getColumnCount() + "columns");
        cursor.close();
        return task;
    }

    private IterableTask createTaskFromCursor(Cursor cursor) {
        String id, name;
        IterableTaskType type = null;
        int version = 1;
        int attempts = 0;
        long dateCreated = 0, dateModified = 0, dateLastAttempted = 0, dateScheduled = 0, dateRequested = 0;
        boolean processing = false, failed = false, blocking = false;
        String data = null, error = null;

        id = cursor.getString(cursor.getColumnIndex(TASK_ID));
        name = cursor.getString(cursor.getColumnIndex(NAME));
        version = cursor.getInt(cursor.getColumnIndex(VERSION));
        dateCreated = cursor.getLong(cursor.getColumnIndex(CREATED_AT));
        if (!cursor.isNull(cursor.getColumnIndex(MODIFIED_AT))) {
            dateModified = cursor.getLong(cursor.getColumnIndex(MODIFIED_AT));
        }
        if (!cursor.isNull(cursor.getColumnIndex(LAST_ATTEMPTED_AT))) {
            dateLastAttempted = cursor.getLong(cursor.getColumnIndex(LAST_ATTEMPTED_AT));
        }
        if (!cursor.isNull(cursor.getColumnIndex(SCHEDULED_AT))) {
            dateScheduled = cursor.getLong(cursor.getColumnIndex(SCHEDULED_AT));
        }
        if (!cursor.isNull(cursor.getColumnIndex(REQUESTED_AT))) {
            dateRequested = cursor.getLong(cursor.getColumnIndex(REQUESTED_AT));
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

        return new IterableTask(id, name, version, dateCreated, dateModified, dateLastAttempted, dateScheduled, dateRequested, processing, failed, blocking, data, error, type, attempts);
    }

    /**
     * Gets ids of all the tasks in OfflineTask table
     * @return {@link ArrayList} of {@link String} ids for all the tasks in OfflineTask table
     */
    @NonNull
    ArrayList<String> getAllTaskIds() {
        ArrayList<String> taskIds = new ArrayList<>();
        if (!isDatabaseReady()) {
            return taskIds;
        }

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

    long getNumberOfTasks() throws IllegalStateException {
        if (!isDatabaseReady()) {
            throw new IllegalStateException("Database is not ready");
        }
        return DatabaseUtils.queryNumEntries(database, ITERABLE_TASK_TABLE_NAME);
    }

    /**
     * Returns the next scheduled task for processing
     *
     * @return next scheduled {@link IterableTask}
     */
    @Nullable
    IterableTask getNextScheduledTask() {
        if (!isDatabaseReady()) {
            return null;
        }
        Cursor cursor = database.rawQuery("select * from OfflineTask order by scheduled limit 1", null);
        IterableTask task = null;
        if (cursor.moveToFirst()) {
            task = createTaskFromCursor(cursor);
        }
        cursor.close();
        return task;
    }

    /**
     * Deletes all the entries from the OfflineTask table.
     */
    void deleteAllTasks() {
        if (!isDatabaseReady()) {
            return;
        }
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
        if (database == null || !database.isOpen()) {
            notifyDBError();
            IterableLogger.e(TAG, "Database not initialized or is closed");
            return false;
        }
        return true;
    }

    public interface IterableDatabaseStatusListeners {
        void onDBError();
        void isReady();
    }

    private ArrayList<IterableDatabaseStatusListeners> databaseStatusListeners = new ArrayList<>();

    void addDatabaseStatusListener(IterableDatabaseStatusListeners listener) {
        if (isDatabaseReady()) {
            listener.isReady();
        } else {
            listener.onDBError();
        }
        databaseStatusListeners.add(listener);
    }

    void removeDatabaseStatusListener(IterableDatabaseStatusListeners listener) {
        databaseStatusListeners.remove(listener);
    }

    private void notifyDBError() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (IterableDatabaseStatusListeners listener : databaseStatusListeners) {
                    listener.onDBError();
                }
            }
        });
    }
}