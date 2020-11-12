package com.iterable.iterableapi;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class IterableDataManager {

    private static IterableDataManager sharedInstance;

    private static final String TAG = "IterableDataManager";
    private static final String DATABASE_NAME = "iterable_sdk.db";
    private static final int DATABASE_VERSION = 1;
    private static final String ITERABLE_TASK_TABLE_NAME = "OfflineTask";

    private static final String OFFLINE_TASK_COLUMN_DATA = " (" + IterableTask.TASK_ID + " TEXT PRIMARY KEY," +
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
    private static final String KEY_ROWID = "rowid";

    private SQLiteDatabase database;
    private IterableDatabaseManager databaseManager;
    private ContentValues contentValues = new ContentValues();
    private boolean hasDatabaseError = false;

    IterableDataManager() {
        try {
            Context context = IterableApi.getInstance().getMainActivityContext();
            if (context == null) {
                return;
            }

            if (databaseManager == null) {
                databaseManager = new IterableDatabaseManager(IterableApi.getInstance().getMainActivityContext());
            }
            database = databaseManager.getWritableDatabase();
        } catch (Throwable t) {
            IterableLogger.e(TAG, "Failed to create database");
        }
    }

    public static IterableDataManager sharedInstance() {
        if(sharedInstance == null) {
            sharedInstance = new IterableDataManager();
        }
        return sharedInstance;
    }

    /*
    * Create and insert a new task in the database
    *
    * */
    void createTask(String name) {

        if(database == null) {
            IterableLogger.e(TAG, "Database not initialized");
            return;
        }
        IterableTask iterableTask = new IterableTask(name, IterableTaskType.API);
        contentValues.put(IterableTask.TASK_ID, iterableTask.id);
        contentValues.put(IterableTask.NAME, iterableTask.name);
        contentValues.put(IterableTask.VERSION, iterableTask.version);
        contentValues.put(IterableTask.CREATED_AT, iterableTask.createdAt.toString());
        if(iterableTask.modifiedAt != null) {
            contentValues.put(IterableTask.MODIFIED_AT, iterableTask.modifiedAt.toString());
        }
        if(iterableTask.lastAttemptedAt != null) {
            contentValues.put(IterableTask.LAST_ATTEMPTED_AT, iterableTask.lastAttemptedAt.toString());
        }
        if(iterableTask.scheduledAt != null) {
            contentValues.put(IterableTask.SCHEDULED_AT, iterableTask.scheduledAt.toString());
        }
        if(iterableTask.requestedAt != null) {
            contentValues.put(IterableTask.REQUESTED_AT, iterableTask.requestedAt.toString());
        }
        if(iterableTask.processing != null) {
            contentValues.put(IterableTask.PROCESSING, iterableTask.processing);
        }
        if(iterableTask.failed != null) {
            contentValues.put(IterableTask.FAILED, iterableTask.failed);
        }
        if(iterableTask.blocking != null) {
            contentValues.put(IterableTask.BLOCKING, iterableTask.blocking);
        }
        if(iterableTask.data != null) {
            contentValues.put(IterableTask.DATA, iterableTask.data.toString());
        }
        if(iterableTask.taskFailureData != null) {
            contentValues.put(IterableTask.ERROR, iterableTask.taskFailureData.toString());
        }

        contentValues.put(IterableTask.TYPE, iterableTask.taskType.toString());
        contentValues.put(IterableTask.ATTEMPTS, iterableTask.attempts);

        database.insert(ITERABLE_TASK_TABLE_NAME,null,contentValues);

        contentValues.clear();
    }

    Boolean removeTask(String id) {
        //To be implemented
        return false;
    }

    IterableTask getTask(String id) {
        //To be implemented
        return null;
    }

    /**
     * Gets number of rows in the event table.
     *
     * @return Number of rows in the event table.
     */
    long getEventsCount() {
        long count = 0;
        if (database == null) {
            return count;
        }
        try {
            count = DatabaseUtils.queryNumEntries(database, ITERABLE_TASK_TABLE_NAME);
            hasDatabaseError = false;
        } catch (Throwable t) {
            IterableLogger.e(TAG, "Unable to get a number of rows in the table.", t);
        }
        return count;
    }

    void deleteAllTasks () {
        if (getEventsCount() == 0) {
            IterableLogger.d(TAG, "No pending offline tasks found");
            return;
        }
        if (database == null) {
            IterableLogger.e(TAG, "Database not initialized");
            return;
        }

        int numberOfRowsDeleted = database.delete(ITERABLE_TASK_TABLE_NAME, null, null);
        IterableLogger.v(TAG, "Deleted " + numberOfRowsDeleted + " offline tasks");
        return;
    }

    private static class IterableDatabaseManager extends SQLiteOpenHelper {

        IterableDatabaseManager(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // Create event table.
            db.execSQL("CREATE TABLE IF NOT EXISTS " + ITERABLE_TASK_TABLE_NAME + OFFLINE_TASK_COLUMN_DATA);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // No used for now.
        }

    }
}
