package com.iterable.inbox_customization

import android.app.Application
import com.iterable.inbox_customization.util.DataManager

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DataManager.initializeIterableApi(this)

    }
}