package com.iterable.integration.tests.activities

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.IterableInAppLocation
import com.iterable.integration.tests.R

/**
 * Test activity specifically designed for in-app message testing
 * This activity provides a controlled environment for testing in-app message display and interaction
 */
class InAppTestActivity : Activity() {
    
    companion object {
        private const val TAG = "InAppTestActivity"
    }
    
    private lateinit var statusTextView: TextView
    private lateinit var triggerButton: Button
    private lateinit var clearButton: Button
    private lateinit var webViewContainer: LinearLayout
    private lateinit var testWebView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inapp_test)
        
        initializeViews()
        setupClickListeners()
        setupWebView()
        
        Log.d(TAG, "InAppTestActivity created")
    }
    
    private fun initializeViews() {
        statusTextView = findViewById(R.id.status_text)
        triggerButton = findViewById(R.id.trigger_button)
        clearButton = findViewById(R.id.clear_button)
        webViewContainer = findViewById(R.id.webview_container)
        testWebView = findViewById(R.id.test_webview)
        
        updateStatus("Activity initialized")
    }
    
    private fun setupClickListeners() {
        triggerButton.setOnClickListener {
            triggerInAppMessage()
        }
        
        clearButton.setOnClickListener {
            clearInAppMessages()
        }
    }
    
    private fun setupWebView() {
        testWebView.settings.javaScriptEnabled = true
        testWebView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "WebView page finished loading: $url")
                updateStatus("WebView loaded: $url")
            }
            
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.d(TAG, "WebView URL clicked: $url")
                updateStatus("URL clicked: $url")
                return true
            }
        }
    }
    
    private fun triggerInAppMessage() {
        updateStatus("Triggering in-app message...")
        
        try {
            // Track an event that should trigger an in-app message
            IterableApi.getInstance().track("test_inapp_event")
            
            // Also try to get existing messages and display them
            val messages = IterableApi.getInstance().getInAppManager().getMessages()
            if (messages.isNotEmpty()) {
                Log.d(TAG, "Found ${messages.size} in-app messages")
                displayInAppMessage(messages.first())
            } else {
                updateStatus("No in-app messages available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering in-app message", e)
            updateStatus("Error: ${e.message}")
        }
    }
    
    private fun displayInAppMessage(message: IterableInAppMessage) {
        try {
            Log.d(TAG, "Displaying in-app message: ${message.messageId}")
            updateStatus("Displaying message: ${message.messageId}")
            
            // Load the HTML content into our test WebView
            val htmlContent = message.content.html
            testWebView.loadDataWithBaseURL("", htmlContent, "text/html", "UTF-8", "")
            
            // Also try to show the message using the SDK
            IterableApi.getInstance().getInAppManager().showMessage(message, IterableInAppLocation.IN_APP)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying in-app message", e)
            updateStatus("Error displaying message: ${e.message}")
        }
    }
    
    private fun clearInAppMessages() {
        try {
            // Get all current messages and remove them individually
            val messages = IterableApi.getInstance().getInAppManager().getMessages()
            for (message in messages) {
                IterableApi.getInstance().getInAppManager().removeMessage(message)
            }
            
            // Also clear inbox messages
            val inboxMessages = IterableApi.getInstance().getInAppManager().getInboxMessages()
            for (message in inboxMessages) {
                IterableApi.getInstance().getInAppManager().removeMessage(message)
            }
            
            testWebView.loadData("", "text/html", "UTF-8")
            updateStatus("Cleared ${messages.size + inboxMessages.size} in-app messages")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing in-app messages", e)
            updateStatus("Error clearing messages: ${e.message}")
        }
    }
    
    private fun updateStatus(message: String) {
        runOnUiThread {
            statusTextView.text = message
            Log.d(TAG, "Status: $message")
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed")
        
        // Check for in-app messages when activity resumes
        checkForInAppMessages()
    }
    
    private fun checkForInAppMessages() {
        try {
            val messages = IterableApi.getInstance().getInAppManager().getMessages()
            if (messages.isNotEmpty()) {
                Log.d(TAG, "Found ${messages.size} in-app messages on resume")
                updateStatus("Found ${messages.size} in-app messages")
            } else {
                updateStatus("No in-app messages found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for in-app messages", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Activity destroyed")
    }
}
