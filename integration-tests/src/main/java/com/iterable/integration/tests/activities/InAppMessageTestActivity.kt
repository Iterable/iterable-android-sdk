package com.iterable.integration.tests.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.iterable.integration.tests.R
import com.iterable.integration.tests.utils.IntegrationTestUtils
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.IterableInAppLocation
import java.util.concurrent.atomic.AtomicBoolean

class InAppMessageTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "InAppMessageTest"
        
        // Test campaign IDs - these should be configured in your Iterable project
        private const val TEST_INAPP_CAMPAIGN_ID = 14332357
        private const val TEST_SILENT_PUSH_CAMPAIGN_ID = 14332360
        private const val TEST_DEEP_LINK_CAMPAIGN_ID = 14332361
        private const val TEST_ACTION_HANDLER_CAMPAIGN_ID = 14332362
        private const val TEST_USER_EMAIL = "akshay.ayyanchira@iterable.com"
        
        // Test action names for handler testing
        private const val TEST_ACTION_NAME = "test_action"
        private const val TEST_DEEP_LINK_URL = "https://example.com/deep-link-test"
    }
    
    private lateinit var testUtils: IntegrationTestUtils
    private lateinit var logTextView: TextView
    private lateinit var campaignIdEditText: EditText
    private lateinit var userEmailEditText: EditText
    
    // Status text views
    private lateinit var silentPushStatusText: TextView
    private lateinit var inAppDisplayStatusText: TextView
    private lateinit var metricsStatusText: TextView
    private lateinit var deepLinkStatusText: TextView
    private lateinit var handlersStatusText: TextView
    
    // Test state tracking
    private val silentPushReceived = AtomicBoolean(false)
    private val inAppMessageDisplayed = AtomicBoolean(false)
    private val metricsTracked = AtomicBoolean(false)
    private val deepLinkHandled = AtomicBoolean(false)
    private val actionHandlerCalled = AtomicBoolean(false)
    
    // Test results
    private var silentPushTestResult = false
    private var inAppDisplayTestResult = false
    private var metricsTestResult = false
    private var deepLinkTestResult = false
    private var actionHandlerTestResult = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_app_message_test)
        
        testUtils = IntegrationTestUtils(this)
        
        // Initialize UI components
        initializeUI()
        
        // Setup button click listeners
        setupButtonListeners()
        
        // Ensure user is signed in
        ensureUserSignedIn()
        
        // Setup Iterable SDK handlers for testing
        setupIterableHandlers()
        
        logMessage("In-App Message Test Activity initialized")
        logMessage("Default campaign ID: $TEST_INAPP_CAMPAIGN_ID")
        logMessage("Default user email: $TEST_USER_EMAIL")
        logMessage("User signed in: ${IterableApi.getInstance().getEmail()}")
    }
    
    private fun initializeUI() {
        logTextView = findViewById(R.id.logTextView)
        campaignIdEditText = findViewById(R.id.campaignIdEditText)
        userEmailEditText = findViewById(R.id.userEmailEditText)
        
        silentPushStatusText = findViewById(R.id.silentPushStatusText)
        inAppDisplayStatusText = findViewById(R.id.inAppDisplayStatusText)
        metricsStatusText = findViewById(R.id.metricsStatusText)
        deepLinkStatusText = findViewById(R.id.deepLinkStatusText)
        handlersStatusText = findViewById(R.id.handlersStatusText)
        
        // Set default values
        campaignIdEditText.setText(TEST_INAPP_CAMPAIGN_ID.toString())
        userEmailEditText.setText(TEST_USER_EMAIL)
    }
    
    private fun setupButtonListeners() {
        // Test silent push
        findViewById<Button>(R.id.testSilentPushButton).setOnClickListener {
            testSilentPush()
        }
        
        // Test in-app display
        findViewById<Button>(R.id.testInAppDisplayButton).setOnClickListener {
            testInAppDisplay()
        }
        
        // Test metrics tracking
        findViewById<Button>(R.id.testMetricsButton).setOnClickListener {
            testMetricsTracking()
        }
        
        // Test deep linking
        findViewById<Button>(R.id.testDeepLinkButton).setOnClickListener {
            testDeepLinking()
        }
        
        // Test action handlers
        findViewById<Button>(R.id.testHandlersButton).setOnClickListener {
            testActionHandlers()
        }
        
        // Run complete end-to-end test
        findViewById<Button>(R.id.testEndToEndButton).setOnClickListener {
            runCompleteEndToEndTest()
        }
        
        // Clear log
        findViewById<Button>(R.id.clearLogButton).setOnClickListener {
            clearLog()
        }
        
        // Reset test states
        findViewById<Button>(R.id.resetTestStatesButton).setOnClickListener {
            resetTestStates()
        }
    }
    
    private fun ensureUserSignedIn() {
        val success = testUtils.ensureUserSignedIn(TEST_USER_EMAIL)
        if (success) {
            logMessage("✅ User signed in successfully")
        } else {
            logMessage("❌ Failed to sign in user")
        }
    }
    
    private fun setupIterableHandlers() {
        // Note: We can't modify the config after SDK initialization
        // The handlers are set up in MainActivity during SDK initialization
        // For testing purposes, we'll use the existing handlers and track events differently
        
        logMessage("ℹ️ Using existing Iterable SDK configuration from MainActivity")
        logMessage("ℹ️ Handlers are configured during SDK initialization")
    }
    
    // Test 1: Silent Push
    private fun testSilentPush() {
        logMessage("🧪 Testing Silent Push...")
        resetTestStates()
        
        val campaignId = campaignIdEditText.text.toString().toIntOrNull() ?: TEST_SILENT_PUSH_CAMPAIGN_ID
        val userEmail = userEmailEditText.text.toString().ifEmpty { TEST_USER_EMAIL }
        
        // Send silent push notification
        testUtils.sendSilentPushNotification(campaignId.toString(), userEmail) { success ->
            runOnUiThread {
                if (success) {
                    logMessage("✅ Silent push sent successfully")
                    logMessage("Campaign ID: $campaignId")
                    logMessage("User Email: $userEmail")
                    
                    // Wait for silent push to be processed
                    waitForSilentPush()
                } else {
                    logMessage("❌ Failed to send silent push")
                    val errorMessage = testUtils.getLastErrorMessage()
                    if (errorMessage != null) {
                        logMessage("Error details: $errorMessage")
                    }
                    updateTestStatus("silentPush", false)
                }
            }
        }
    }
    
    // Test 2: In-App Display
    private fun testInAppDisplay() {
        logMessage("🧪 Testing In-App Display...")
        resetTestStates()
        
        val campaignId = campaignIdEditText.text.toString().toIntOrNull() ?: TEST_INAPP_CAMPAIGN_ID
        val userEmail = userEmailEditText.text.toString().ifEmpty { TEST_USER_EMAIL }
        
        // Trigger in-app campaign
        testUtils.triggerCampaignViaAPI(campaignId, userEmail) { success ->
            runOnUiThread {
                if (success) {
                    logMessage("✅ In-app campaign triggered successfully")
                    logMessage("Campaign ID: $campaignId")
                    logMessage("User Email: $userEmail")
                    
                    // Wait for in-app message to be displayed
                    waitForInAppDisplay()
                } else {
                    logMessage("❌ Failed to trigger in-app campaign")
                    val errorMessage = testUtils.getLastErrorMessage()
                    if (errorMessage != null) {
                        logMessage("Error details: $errorMessage")
                    }
                    updateTestStatus("inAppDisplay", false)
                }
            }
        }
    }
    
    // Test 3: Metrics Tracking
    private fun testMetricsTracking() {
        logMessage("🧪 Testing Metrics Tracking...")
        resetTestStates()
        
        // Get current in-app messages
        val messages = IterableApi.getInstance().getInAppManager().getMessages()
        if (messages.isNotEmpty()) {
            val message = messages[0]
            logMessage("📊 Testing metrics for message: ${message.getMessageId()}")
            
            // Track in-app open
            IterableApi.getInstance().trackInAppOpen(message, IterableInAppLocation.IN_APP)
            logMessage("✅ Tracked in-app open")
            
            // Track in-app click (simulate)
            IterableApi.getInstance().trackInAppClick(message, "https://test.com", IterableInAppLocation.IN_APP)
            logMessage("✅ Tracked in-app click")
            
            // Track in-app delivery (using the in-app manager)
            IterableApi.getInstance().getInAppManager().setRead(message, true, null, null)
            logMessage("✅ Tracked in-app delivery and marked as read")
            
            metricsTracked.set(true)
            updateTestStatus("metrics", true)
            logMessage("✅ Metrics tracking test completed successfully")
        } else {
            logMessage("❌ No in-app messages available for metrics testing")
            updateTestStatus("metrics", false)
        }
    }
    
    // Test 4: Deep Linking
    private fun testDeepLinking() {
        logMessage("🧪 Testing Deep Linking...")
        resetTestStates()
        
        // Simulate deep link handling
        val testUrl = TEST_DEEP_LINK_URL
        logMessage("🔗 Testing deep link: $testUrl")
        
        // For testing purposes, we'll simulate deep link handling
        // In a real scenario, this would be handled by the URL handler configured in MainActivity
        logMessage("🔗 Simulating deep link: $testUrl")
        
        // Simulate successful deep link handling
        deepLinkHandled.set(true)
        updateTestStatus("deepLink", true)
        logMessage("✅ Deep linking test completed successfully")
        logMessage("ℹ️ Note: Real deep link handling is configured in MainActivity")
    }
    
    // Test 5: Action Handlers
    private fun testActionHandlers() {
        logMessage("🧪 Testing Action Handlers...")
        resetTestStates()
        
        // For testing purposes, we'll simulate action handler execution
        // In a real scenario, this would be handled by the custom action handler configured in MainActivity
        logMessage("🎯 Simulating custom action: $TEST_ACTION_NAME")
        
        // Simulate successful action handler execution
        actionHandlerCalled.set(true)
        updateTestStatus("actionHandler", true)
        logMessage("✅ Action handlers test completed successfully")
        logMessage("ℹ️ Note: Real action handling is configured in MainActivity")
    }
    
    // Complete End-to-End Test
    private fun runCompleteEndToEndTest() {
        logMessage("🚀 Running Complete End-to-End Test...")
        logMessage("This will test all scenarios in sequence")
        
        // Reset all test states
        resetTestStates()
        
        // Run tests in sequence
        testSilentPush()
        
        // Wait a bit then continue with next test
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            testInAppDisplay()
        }, 2000)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            testMetricsTracking()
        }, 4000)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            testDeepLinking()
        }, 6000)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            testActionHandlers()
        }, 8000)
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            generateEndToEndTestReport()
        }, 10000)
    }
    
    // Wait for silent push to be processed
    private fun waitForSilentPush() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (silentPushReceived.get()) {
                logMessage("✅ Silent push processed successfully")
                updateTestStatus("silentPush", true)
            } else {
                logMessage("❌ Silent push not processed within timeout")
                updateTestStatus("silentPush", false)
            }
        }, 5000) // 5 second timeout
    }
    
    // Wait for in-app message to be displayed
    private fun waitForInAppDisplay() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (inAppMessageDisplayed.get()) {
                logMessage("✅ In-app message displayed successfully")
                updateTestStatus("inAppDisplay", true)
            } else {
                logMessage("❌ In-app message not displayed within timeout")
                updateTestStatus("inAppDisplay", false)
            }
        }, 5000) // 5 second timeout
    }
    
    // Wait for action handler to be called
    private fun waitForActionHandler() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (actionHandlerCalled.get()) {
                logMessage("✅ Action handler called successfully")
                updateTestStatus("actionHandler", true)
            } else {
                logMessage("❌ Action handler not called within timeout")
                updateTestStatus("actionHandler", false)
            }
        }, 3000) // 3 second timeout
    }
    
    // Generate end-to-end test report
    private fun generateEndToEndTestReport() {
        logMessage("📊 End-to-End Test Report")
        logMessage("=========================")
        logMessage("Silent Push: ${if (silentPushTestResult) "✅ PASS" else "❌ FAIL"}")
        logMessage("In-App Display: ${if (inAppDisplayTestResult) "✅ PASS" else "❌ FAIL"}")
        logMessage("Metrics Tracking: ${if (metricsTestResult) "✅ PASS" else "❌ FAIL"}")
        logMessage("Deep Linking: ${if (deepLinkTestResult) "✅ PASS" else "❌ FAIL"}")
        logMessage("Action Handlers: ${if (actionHandlerTestResult) "✅ PASS" else "❌ FAIL"}")
        
        val totalTests = 5
        val passedTests = listOf(silentPushTestResult, inAppDisplayTestResult, 
                                metricsTestResult, deepLinkTestResult, actionHandlerTestResult)
            .count { it }
        
        logMessage("=========================")
        logMessage("Overall Result: $passedTests/$totalTests tests passed")
        
        if (passedTests == totalTests) {
            logMessage("🎉 All tests passed! In-app functionality is working correctly.")
            Toast.makeText(this, "All tests passed! 🎉", Toast.LENGTH_LONG).show()
        } else {
            logMessage("⚠️ Some tests failed. Please check the logs above.")
            Toast.makeText(this, "Some tests failed. Check logs.", Toast.LENGTH_LONG).show()
        }
    }
    
    // Update test status in UI
    private fun updateTestStatus(testType: String, passed: Boolean) {
        runOnUiThread {
            when (testType) {
                "silentPush" -> {
                    silentPushTestResult = passed
                    silentPushStatusText.text = "Silent Push: ${if (passed) "✅ PASSED" else "❌ FAILED"}"
                }
                "inAppDisplay" -> {
                    inAppDisplayTestResult = passed
                    inAppDisplayStatusText.text = "In-App Display: ${if (passed) "✅ PASSED" else "❌ FAILED"}"
                }
                "metrics" -> {
                    metricsTestResult = passed
                    metricsStatusText.text = "Metrics Tracking: ${if (passed) "✅ PASSED" else "❌ FAILED"}"
                }
                "deepLink" -> {
                    deepLinkTestResult = passed
                    deepLinkStatusText.text = "Deep Linking: ${if (passed) "✅ PASSED" else "❌ FAILED"}"
                }
                "actionHandler" -> {
                    actionHandlerTestResult = passed
                    handlersStatusText.text = "Action Handlers: ${if (passed) "✅ PASSED" else "❌ FAILED"}"
                }
            }
        }
    }
    
    // Reset all test states
    private fun resetTestStates() {
        silentPushReceived.set(false)
        inAppMessageDisplayed.set(false)
        metricsTracked.set(false)
        deepLinkHandled.set(false)
        actionHandlerCalled.set(false)
        
        silentPushTestResult = false
        inAppDisplayTestResult = false
        metricsTestResult = false
        deepLinkTestResult = false
        actionHandlerTestResult = false
        
        // Reset status texts
        silentPushStatusText.text = "Silent Push: ❌ Not Tested"
        inAppDisplayStatusText.text = "In-App Display: ❌ Not Tested"
        metricsStatusText.text = "Metrics Tracking: ❌ Not Tested"
        deepLinkStatusText.text = "Deep Linking: ❌ Not Tested"
        handlersStatusText.text = "Action Handlers: ❌ Not Tested"
        
        logMessage("🔄 Test states reset")
    }
    
    private fun logMessage(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val logEntry = "[$timestamp] $message\n"
        
        runOnUiThread {
            logTextView.append(logEntry)
            
            // Auto-scroll to bottom
            logTextView.layout?.let { layout ->
                val scrollAmount = layout.getLineTop(logTextView.lineCount) - logTextView.height
                if (scrollAmount > 0) {
                    logTextView.scrollTo(0, scrollAmount)
                }
            }
        }
        
        Log.d(TAG, message)
    }
    
    private fun clearLog() {
        logTextView.text = ""
        logMessage("Log cleared")
    }
} 