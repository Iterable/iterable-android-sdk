package com.iterable.integration.tests.activities

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.iterable.integration.tests.R

class PushNotificationTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "PushNotificationTest"
    }
    
    private lateinit var statusTextView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_push_notification_test)
        
        Log.d(TAG, "Push Notification Test Activity started")
        
        initializeViews()
        updateStatus("Ready to test push notifications")
    }
    
    private fun initializeViews() {
        statusTextView = findViewById(R.id.status_text)
    }
    
    private fun updateStatus(status: String) {
        statusTextView.text = "Status: $status"
    }
} 