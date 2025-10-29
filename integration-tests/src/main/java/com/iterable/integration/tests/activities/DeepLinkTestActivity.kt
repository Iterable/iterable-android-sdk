package com.iterable.integration.tests.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.iterable.integration.tests.R

class DeepLinkTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "DeepLinkTest"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deep_link_test)
        
        Log.d(TAG, "Deep Link Test Activity started")
        
        // Handle deep link URL if passed as extra
        intent.getStringExtra("deep_link_url")?.let { url ->
            Log.d(TAG, "Received deep link URL: $url")
            // TODO: Implement deep link handling logic
        }
    }
} 