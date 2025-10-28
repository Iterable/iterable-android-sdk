package com.iterable.integration.tests.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.iterable.integration.tests.R

class EmbeddedMessageTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "EmbeddedMessageTest"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_embedded_message_test)
        
        Log.d(TAG, "Embedded Message Test Activity started")
        
        // TODO: Implement embedded message test UI and logic
    }
} 