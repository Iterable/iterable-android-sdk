package com.iterable.iterableapi;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class IterableDatabaseManager extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "iterable_sdk.db";
    private static final int DATABASE_VERSION = 1;
    IterableDatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create event table.
        db.execSQL("CREATE TABLE IF NOT EXISTS " + IterableTaskStorage.ITERABLE_TASK_TABLE_NAME + IterableTaskStorage.OFFLINE_TASK_COLUMN_DATA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No used for now.
    }

}
