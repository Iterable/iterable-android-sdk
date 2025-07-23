package com.iterable.integration.tests.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.iterable.integration.tests.R

class InAppMessageTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "InAppMessageTest"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_app_message_test)
        
        Log.d(TAG, "In-App Message Test Activity started")
        
        // TODO: Implement in-app message test UI and logic
    }
} 