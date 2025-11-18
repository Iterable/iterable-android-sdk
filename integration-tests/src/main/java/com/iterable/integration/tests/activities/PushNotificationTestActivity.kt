package com.iterable.integration.tests.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.iterable.integration.tests.R
import com.iterable.integration.tests.TestConstants
import com.iterable.integration.tests.utils.IntegrationTestUtils
import com.iterable.iterableapi.IterableApi

class PushNotificationTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "PushNotificationTest"
        private const val TEST_PUSH_CAMPAIGN_ID = TestConstants.TEST_PUSH_CAMPAIGN_ID
    }
    
    private lateinit var statusTextView: TextView
    private lateinit var permissionStatusTextView: TextView
    private lateinit var triggerCampaignButton: Button
    private lateinit var requestPermissionButton: Button
    private lateinit var testUtils: IntegrationTestUtils
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        updatePermissionStatus()
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission denied. Push notifications may not work.", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_push_notification_test)
        
        Log.d(TAG, "Push Notification Test Activity started")
        
        testUtils = IntegrationTestUtils(this)
        
        initializeViews()
        setupButtonListeners()
        ensureUserSignedIn()
        updatePermissionStatus()
        updateStatus("Ready to test push notifications")
    }
    
    override fun onResume() {
        super.onResume()
        // Update permission status when activity resumes (in case user granted permission)
        updatePermissionStatus()
    }
    
    private fun initializeViews() {
        statusTextView = findViewById(R.id.status_text)
        permissionStatusTextView = findViewById(R.id.permission_status_text)
        triggerCampaignButton = findViewById(R.id.btnTriggerPushCampaign)
        requestPermissionButton = findViewById(R.id.btnRequestPermission)
    }
    
    private fun setupButtonListeners() {
        triggerCampaignButton.setOnClickListener {
            triggerPushCampaign()
        }
        
        requestPermissionButton.setOnClickListener {
            requestNotificationPermission()
        }
    }
    
    private fun ensureUserSignedIn() {
        val success = testUtils.ensureUserSignedIn(TestConstants.TEST_USER_EMAIL)
        if (success) {
            Log.d(TAG, "User signed in: ${IterableApi.getInstance().getEmail()}")
        } else {
            Log.e(TAG, "Failed to sign in user")
            updateStatus("Failed to sign in user")
        }
    }
    
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below, check if notifications are enabled
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        }
    }
    
    private fun updatePermissionStatus() {
        val hasPermission = hasNotificationPermission()
        val statusText = if (hasPermission) {
            "✅ Notification Permission: Granted"
        } else {
            "❌ Notification Permission: Not Granted"
        }
        permissionStatusTextView.text = statusText
        
        // Enable/disable trigger button based on permission
        triggerCampaignButton.isEnabled = hasPermission
        if (!hasPermission) {
            updateStatus("Please grant notification permission to test push notifications")
        }
        
        // Show/hide request permission button based on Android version and current permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionButton.visibility = if (hasPermission) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }
        } else {
            // For Android 12 and below, permission is managed in system settings
            requestPermissionButton.visibility = if (hasPermission) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }
            requestPermissionButton.text = "Open Notification Settings"
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // For Android 12 and below, open notification settings
            val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
            }
            startActivity(intent)
        }
    }
    
    private fun triggerPushCampaign() {
        // Double-check permission before triggering
        if (!hasNotificationPermission()) {
            updateStatus("❌ Notification permission not granted. Please grant permission first.")
            Toast.makeText(this, "Notification permission required", Toast.LENGTH_SHORT).show()
            updatePermissionStatus()
            return
        }
        
        updateStatus("Triggering push campaign...")
        triggerCampaignButton.isEnabled = false
        
        testUtils.triggerPushCampaignViaAPI(TEST_PUSH_CAMPAIGN_ID, TestConstants.TEST_USER_EMAIL) { success ->
            runOnUiThread {
                updatePermissionStatus() // Refresh permission status
                triggerCampaignButton.isEnabled = hasNotificationPermission()
                if (success) {
                    updateStatus("✅ Push campaign triggered successfully!\nCampaign ID: $TEST_PUSH_CAMPAIGN_ID\nCheck notification drawer in a few seconds.")
                    Toast.makeText(this@PushNotificationTestActivity, "Push campaign triggered successfully", Toast.LENGTH_LONG).show()
                    Log.d(TAG, "Push campaign triggered successfully: campaignId=$TEST_PUSH_CAMPAIGN_ID")
                } else {
                    val errorMessage = testUtils.getLastErrorMessage()
                    updateStatus("❌ Failed to trigger push campaign\n${errorMessage ?: "Unknown error"}")
                    Toast.makeText(this@PushNotificationTestActivity, "Failed to trigger push campaign", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Failed to trigger push campaign: $errorMessage")
                }
            }
        }
    }
    
    private fun updateStatus(status: String) {
        statusTextView.text = "Status: $status"
    }
} 