package com.iterable.integration.tests

import android.app.Application
import com.iterable.integration.tests.capture.RequestCapture

class BcitApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.REQUEST_CAPTURE_ENABLED) {
            RequestCapture.install()
        }
    }
}
