package com.iterable.inbox_customization

import android.app.Application

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // SDK initialization is now handled by ConfigurationFragment after user enters API key
    }
}