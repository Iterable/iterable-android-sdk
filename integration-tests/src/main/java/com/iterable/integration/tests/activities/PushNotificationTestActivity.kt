package com.iterable.integration.tests.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.iterable.integration.tests.R

class PushNotificationTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "PushNotificationTest"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_push_notification_test)
        
        Log.d(TAG, "Push Notification Test Activity started")
        
        // TODO: Implement push notification test UI and logic
    }
} 