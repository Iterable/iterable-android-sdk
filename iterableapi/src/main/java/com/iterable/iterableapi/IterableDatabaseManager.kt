package com.iterable.iterableapi

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class IterableDatabaseManager(context: Context) : 
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    
    companion object {
        private const val DATABASE_NAME = "iterable_sdk.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create event table.
        db.execSQL("CREATE TABLE IF NOT EXISTS " + IterableTaskStorage.ITERABLE_TASK_TABLE_NAME + IterableTaskStorage.OFFLINE_TASK_COLUMN_DATA)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No used for now.
    }

}
