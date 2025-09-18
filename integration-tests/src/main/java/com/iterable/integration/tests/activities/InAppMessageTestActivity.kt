package com.iterable.integration.tests.activities

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.IterableInAppLocation
import com.iterable.integration.tests.R
import com.iterable.integration.tests.BuildConfig

class InAppMessageTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "InAppMessageTest"
    }
    
    private lateinit var userInfoTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var triggerButton: Button
    private lateinit var clearButton: Button
    private lateinit var refreshButton: Button
    private lateinit var webViewContainer: LinearLayout
    private lateinit var testWebView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_app_message_test)
        
        Log.d(TAG, "In-App Message Test Activity started")
        
        initializeViews()
        setupClickListeners()
        setupWebView()
        updateUserInfo()
        
        updateStatus("Activity initialized - InApp messages will display automatically over this screen")
    }
    
    private fun initializeViews() {
        userInfoTextView = findViewById(R.id.user_info_text)
        statusTextView = findViewById(R.id.status_text)
        triggerButton = findViewById(R.id.trigger_button)
        clearButton = findViewById(R.id.clear_button)
        refreshButton = findViewById(R.id.refresh_button)
        webViewContainer = findViewById(R.id.webview_container)
        testWebView = findViewById(R.id.test_webview)
    }
    
    private fun setupClickListeners() {
        triggerButton.setOnClickListener {
            triggerInAppMessage()
        }
        
        clearButton.setOnClickListener {
            clearInAppMessages()
        }
        
        refreshButton.setOnClickListener {
            refreshInAppMessages()
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
                updateStatus("Found ${messages.size} in-app messages - displaying first one")
                displayInAppMessage(messages.first())
            } else {
                updateStatus("No in-app messages available - tracked event to potentially trigger new ones")
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
            
            // Load the HTML content into our test WebView for preview
            val htmlContent = message.content.html
            testWebView.loadDataWithBaseURL("", htmlContent, "text/html", "UTF-8", "")
            
            // Also show the message using the SDK (this will show the overlay)
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
            Log.d(TAG, "Cleared all in-app messages")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing in-app messages", e)
            updateStatus("Error clearing messages: ${e.message}")
        }
    }
    
    private fun refreshInAppMessages() {
        try {
            updateStatus("Refreshing in-app messages...")
            updateUserInfo() // Also refresh user info
            
            // Check current message count
            val messages = IterableApi.getInstance().getInAppManager().getMessages()
            val inboxMessages = IterableApi.getInstance().getInAppManager().getInboxMessages()
            
            updateStatus("Found ${messages.size} in-app messages, ${inboxMessages.size} inbox messages")
            Log.d(TAG, "Message refresh: ${messages.size} in-app, ${inboxMessages.size} inbox")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing in-app messages", e)
            updateStatus("Error refreshing: ${e.message}")
        }
    }
    
    private fun updateStatus(status: String) {
        statusTextView.text = status
        Log.d(TAG, "Status: $status")
    }
    
    private fun updateUserInfo() {
        try {
            val userEmail = IterableApi.getInstance().getEmail() ?: "Not signed in"
            val testUserEmail = BuildConfig.ITERABLE_TEST_USER_EMAIL
            val apiKey = BuildConfig.ITERABLE_API_KEY
            val serverApiKey = BuildConfig.ITERABLE_SERVER_API_KEY
            
            // Truncate API keys for display (show first 8 and last 4 characters)
            val displayApiKey = if (apiKey.length > 12) {
                "${apiKey.take(8)}...${apiKey.takeLast(4)}"
            } else {
                apiKey
            }
            
            val displayServerApiKey = if (serverApiKey.length > 12) {
                "${serverApiKey.take(8)}...${serverApiKey.takeLast(4)}"
            } else {
                serverApiKey
            }
            
            val userInfo = "User: $userEmail | Test User: $testUserEmail | API Key: $displayApiKey | Server Key: $displayServerApiKey"
            userInfoTextView.text = userInfo
            
            Log.d(TAG, "User Info: $userInfo")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user info", e)
            userInfoTextView.text = "Error loading user info: ${e.message}"
        }
    }
} 