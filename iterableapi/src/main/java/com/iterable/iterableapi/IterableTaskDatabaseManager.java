package com.iterable.iterableapi;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class IterableTaskDatabaseManager extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "iterable_sdk.db";
    private static final int DATABASE_VERSION = 1;
    IterableTaskDatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create event table.
        db.execSQL("CREATE TABLE IF NOT EXISTS " + IterableTaskManager.ITERABLE_TASK_TABLE_NAME + IterableTaskManager.OFFLINE_TASK_COLUMN_DATA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No used for now.
    }

}
