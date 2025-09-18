package com.iterable.integration.tests.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.iterable.integration.tests.R
import com.iterable.integration.tests.utils.IntegrationTestUtils
import com.iterable.integration.tests.TestConstants

class CampaignTriggerTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "CampaignTriggerTestActivity"
        
        // Test campaign IDs - these should be configured in your Iterable project
        private const val TEST_INAPP_CAMPAIGN_ID = TestConstants.TEST_INAPP_CAMPAIGN_ID
        private const val TEST_PUSH_CAMPAIGN_ID = TestConstants.TEST_PUSH_CAMPAIGN_ID
        private val TEST_USER_EMAIL = TestConstants.TEST_USER_EMAIL
    }
    
    private lateinit var testUtils: IntegrationTestUtils
    private lateinit var logTextView: TextView
    private lateinit var campaignIdEditText: EditText
    private lateinit var userEmailEditText: EditText
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campaign_trigger_test)
        
        testUtils = IntegrationTestUtils(this)
        
        // Initialize UI components first
        logTextView = findViewById(R.id.logTextView)
        campaignIdEditText = findViewById(R.id.campaignIdEditText)
        userEmailEditText = findViewById(R.id.userEmailEditText)
        
        // Set default values
        campaignIdEditText.setText(TEST_INAPP_CAMPAIGN_ID.toString())
        userEmailEditText.setText(TEST_USER_EMAIL)
        
        // Setup button click listeners
        setupButtonListeners()
        
        // Now ensure user is signed in (after UI is initialized)
        ensureUserSignedIn()
        
        logMessage("Campaign Trigger Test Activity initialized")
        logMessage("Default campaign ID: $TEST_INAPP_CAMPAIGN_ID")
        logMessage("Default user email: $TEST_USER_EMAIL")
        logMessage("User signed in: ${com.iterable.iterableapi.IterableApi.getInstance().getEmail()}")
    }
    
    private fun ensureUserSignedIn() {
        val success = testUtils.ensureUserSignedIn(TEST_USER_EMAIL)
        if (success) {
            logMessage("✅ User signed in successfully")
        } else {
            logMessage("❌ Failed to sign in user")
        }
    }
    
    private fun setupButtonListeners() {
        // Test in-app campaign trigger
        findViewById<Button>(R.id.testInAppCampaignButton).setOnClickListener {
            testInAppCampaignTrigger()
        }
        
        // Test push campaign trigger
        findViewById<Button>(R.id.testPushCampaignButton).setOnClickListener {
            testPushCampaignTrigger()
        }
        
        // Test campaign with data fields
        findViewById<Button>(R.id.testCampaignWithDataFieldsButton).setOnClickListener {
            testCampaignWithDataFields()
        }
        
        // Test custom campaign trigger
        findViewById<Button>(R.id.testCustomCampaignButton).setOnClickListener {
            testCustomCampaignTrigger()
        }
        
        // Clear log
        findViewById<Button>(R.id.clearLogButton).setOnClickListener {
            clearLog()
        }
    }
    
    private fun testInAppCampaignTrigger() {
        logMessage("Testing in-app campaign trigger...")
        
        val campaignId = campaignIdEditText.text.toString().toIntOrNull() ?: TEST_INAPP_CAMPAIGN_ID
        val userEmail = userEmailEditText.text.toString().ifEmpty { TEST_USER_EMAIL }
        
        testUtils.triggerCampaignViaAPI(campaignId, userEmail) { success ->
            runOnUiThread {
                if (success) {
                    logMessage("✅ In-app campaign triggered successfully")
                    logMessage("Campaign ID: $campaignId")
                    logMessage("User Email: $userEmail")
                    Toast.makeText(this@CampaignTriggerTestActivity, "In-app campaign triggered successfully", Toast.LENGTH_SHORT).show()
                } else {
                    logMessage("❌ Failed to trigger in-app campaign")
                    val errorMessage = testUtils.getLastErrorMessage()
                    if (errorMessage != null) {
                        logMessage("Error details: $errorMessage")
                    }
                    Toast.makeText(this@CampaignTriggerTestActivity, "Failed to trigger in-app campaign", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun testPushCampaignTrigger() {
        logMessage("Testing push campaign trigger...")
        
        val campaignId = campaignIdEditText.text.toString().toIntOrNull() ?: TEST_PUSH_CAMPAIGN_ID
        val userEmail = userEmailEditText.text.toString().ifEmpty { TEST_USER_EMAIL }
        
        testUtils.triggerPushCampaignViaAPI(campaignId, userEmail) { success ->
            runOnUiThread {
                if (success) {
                    logMessage("✅ Push campaign triggered successfully")
                    logMessage("Campaign ID: $campaignId")
                    logMessage("User Email: $userEmail")
                    Toast.makeText(this@CampaignTriggerTestActivity, "Push campaign triggered successfully", Toast.LENGTH_SHORT).show()
                } else {
                    logMessage("❌ Failed to trigger push campaign")
                    val errorMessage = testUtils.getLastErrorMessage()
                    if (errorMessage != null) {
                        logMessage("Error details: $errorMessage")
                    }
                    Toast.makeText(this@CampaignTriggerTestActivity, "Failed to trigger push campaign", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun testCampaignWithDataFields() {
        logMessage("Testing campaign trigger with data fields...")
        
        val campaignId = campaignIdEditText.text.toString().toIntOrNull() ?: TEST_INAPP_CAMPAIGN_ID
        val userEmail = userEmailEditText.text.toString().ifEmpty { TEST_USER_EMAIL }
        
        // Create test data fields
        val dataFields = mapOf(
            "firstName" to "Jane",
            "lastName" to "Smith",
            "purchaseAmount" to 42.42,
            "testType" to "integration_test",
            "timestamp" to System.currentTimeMillis()
        )
        
        testUtils.triggerCampaignViaAPI(campaignId, userEmail, dataFields) { success ->
            runOnUiThread {
                if (success) {
                    logMessage("✅ Campaign with data fields triggered successfully")
                    logMessage("Campaign ID: $campaignId")
                    logMessage("User Email: $userEmail")
                    logMessage("Data Fields: $dataFields")
                    Toast.makeText(this@CampaignTriggerTestActivity, "Campaign with data fields triggered successfully", Toast.LENGTH_SHORT).show()
                } else {
                    logMessage("❌ Failed to trigger campaign with data fields")
                    val errorMessage = testUtils.getLastErrorMessage()
                    if (errorMessage != null) {
                        logMessage("Error details: $errorMessage")
                    }
                    Toast.makeText(this@CampaignTriggerTestActivity, "Failed to trigger campaign with data fields", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun testCustomCampaignTrigger() {
        logMessage("Testing custom campaign trigger...")
        
        val campaignIdText = campaignIdEditText.text.toString()
        val userEmail = userEmailEditText.text.toString()
        
        if (campaignIdText.isEmpty()) {
            logMessage("❌ Please enter a campaign ID")
            Toast.makeText(this, "Please enter a campaign ID", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (userEmail.isEmpty()) {
            logMessage("❌ Please enter a user email")
            Toast.makeText(this, "Please enter a user email", Toast.LENGTH_SHORT).show()
            return
        }
        
        val campaignId = campaignIdText.toIntOrNull()
        if (campaignId == null) {
            logMessage("❌ Invalid campaign ID: $campaignIdText")
            Toast.makeText(this, "Invalid campaign ID", Toast.LENGTH_SHORT).show()
            return
        }
        
        testUtils.triggerCampaignViaAPI(campaignId, userEmail) { success ->
            runOnUiThread {
                if (success) {
                    logMessage("✅ Custom campaign triggered successfully")
                    logMessage("Campaign ID: $campaignId")
                    logMessage("User Email: $userEmail")
                    Toast.makeText(this@CampaignTriggerTestActivity, "Custom campaign triggered successfully", Toast.LENGTH_SHORT).show()
                } else {
                    logMessage("❌ Failed to trigger custom campaign")
                    val errorMessage = testUtils.getLastErrorMessage()
                    if (errorMessage != null) {
                        logMessage("Error details: $errorMessage")
                    }
                    Toast.makeText(this@CampaignTriggerTestActivity, "Failed to trigger custom campaign", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun logMessage(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val logEntry = "[$timestamp] $message\n"
        
        runOnUiThread {
            logTextView.append(logEntry)
            
            // Auto-scroll to bottom (with null check)
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