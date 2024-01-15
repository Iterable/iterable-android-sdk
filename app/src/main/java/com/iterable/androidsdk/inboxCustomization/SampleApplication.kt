package com.iterable.androidsdk.inboxCustomization

import android.app.Application
import com.iterable.androidsdk.inboxCustomization.util.DataManager

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DataManager.initializeIterableApi(this)
    }
}