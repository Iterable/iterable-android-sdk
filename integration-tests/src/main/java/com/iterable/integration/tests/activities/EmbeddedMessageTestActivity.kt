package com.iterable.integration.tests.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableEmbeddedMessage
import com.iterable.integration.tests.R
import com.iterable.iterableapi.ui.embedded.IterableEmbeddedView
import com.iterable.iterableapi.ui.embedded.IterableEmbeddedViewType
import org.json.JSONObject

class EmbeddedMessageTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "EmbeddedMessageTest"
    }
    
    private lateinit var statusTextView: TextView
    private lateinit var checkIsPremiumButton: Button
    private lateinit var isPremiumSwitch: Switch
    private lateinit var syncMessagesButton: Button
    
    // Track current isPremium state locally (since SDK doesn't store it)
    private var currentIsPremium = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_embedded_message_test)
        
        Log.d(TAG, "Embedded Message Test Activity started")
        
        initializeViews()
        setupClickListeners()
        updateStatus("Activity initialized - Ready to test embedded messages")
    }
    
    private fun initializeViews() {
        statusTextView = findViewById(R.id.status_text)
        checkIsPremiumButton = findViewById(R.id.btnCheckIsPremium)
        isPremiumSwitch = findViewById(R.id.switchIsPremium)
        syncMessagesButton = findViewById(R.id.btnSyncMessages)
        
        // Initialize switch to false
        isPremiumSwitch.isChecked = currentIsPremium
    }
    
    private fun setupClickListeners() {
        checkIsPremiumButton.setOnClickListener {
            checkIsPremiumStatus()
        }
        
        isPremiumSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateUserIsPremium(isChecked)
        }
        
        syncMessagesButton.setOnClickListener {
            syncEmbeddedMessages()
        }
    }
    
    private fun checkIsPremiumStatus() {
        updateStatus("Checking isPremium status...")
        Log.d(TAG, "Checking isPremium status")
        
        // Note: SDK doesn't store user data fields locally, so we show an info dialog
        AlertDialog.Builder(this)
            .setTitle("isPremium Status")
            .setMessage("User data fields are stored on the server, not in the SDK.\n\n" +
                    "To check isPremium status, you can:\n" +
                    "1. Check server logs/dashboard\n" +
                    "2. Call server API to get user profile\n" +
                    "3. Check logcat for updateUser calls")
            .setPositiveButton("OK", null)
            .show()
        
        Toast.makeText(this, "Check logcat or server dashboard for user data fields", Toast.LENGTH_LONG).show()
        updateStatus("Status check: See dialog for details")
    }
    
    private fun updateUserIsPremium(isPremium: Boolean) {
        currentIsPremium = isPremium
        val statusText = if (isPremium) "true" else "false"
        
        updateStatus("Updating user (isPremium = $statusText)...")
        Log.d(TAG, "Updating user with isPremium = $statusText")
        
        val dataFields = JSONObject().apply {
            put("isPremium", isPremium)
        }
        
        // Disable switch during update
        isPremiumSwitch.isEnabled = false
        
        // Note: updateUser doesn't have callbacks in the current SDK API
        // But we can track success/failure by monitoring logs or adding listeners
        IterableApi.getInstance().updateUser(dataFields)
        
        // Show toast
        Toast.makeText(this, "updateUser called (isPremium = $statusText)\nWait 5 seconds then sync messages to verify", Toast.LENGTH_LONG).show()
        Log.d(TAG, "✅ updateUser called with isPremium = $statusText")
        
        // Re-enable switch after delay
        isPremiumSwitch.postDelayed({
            isPremiumSwitch.isEnabled = true
            updateStatus("User update request sent (isPremium = $statusText) - Sync messages to verify")
        }, 1000)
    }
    
    private fun syncEmbeddedMessages() {
        updateStatus("Syncing embedded messages...")
        Log.d(TAG, "Syncing embedded messages")
        
        IterableApi.getInstance().embeddedManager.syncMessages()
        
        // Wait a bit for sync to complete, then show status and display messages
        Thread {
            Thread.sleep(2000)
            
            runOnUiThread {
                val placementIds = IterableApi.getInstance().embeddedManager.getPlacementIds()
                val messageCount = placementIds.sumOf { placementId ->
                    IterableApi.getInstance().embeddedManager.getMessages(placementId)?.size ?: 0
                }
                
                val statusMessage = if (messageCount > 0) {
                    "✅ Sync complete: Found $messageCount message(s) in ${placementIds.size} placement(s)"
                } else {
                    "⚠️ Sync complete: No messages found. Check user eligibility and campaign configuration"
                }
                
                updateStatus(statusMessage)
                Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show()
                Log.d(TAG, statusMessage)
                
                // Display messages if found
                if (messageCount > 0) {
                    displayEmbeddedMessages(placementIds)
                } else {
                    // Clear any existing fragments
                    clearEmbeddedMessages()
                }
            }
        }.start()
    }
    
    private fun displayEmbeddedMessages(placementIds: List<Long>) {
        Log.d(TAG, "Displaying embedded messages for ${placementIds.size} placement(s)")
        
        // For now, display the first message from the first placement
        // In a real scenario, you might want to display all messages or have a placement selector
        val firstPlacementId = placementIds.firstOrNull()
        if (firstPlacementId != null) {
            val messages = IterableApi.getInstance().embeddedManager.getMessages(firstPlacementId)
            if (messages != null && messages.isNotEmpty()) {
                val firstMessage = messages.first()
                Log.d(TAG, "Displaying message: ${firstMessage.metadata.messageId} from placement: $firstPlacementId")
                
                // Create and add fragment
                val fragment = IterableEmbeddedView(IterableEmbeddedViewType.BANNER, firstMessage, null)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.embedded_message_container, fragment)
                    .commitNowAllowingStateLoss()
                
                updateStatus("✅ Message displayed: ${firstMessage.metadata.messageId}")
                Log.d(TAG, "✅ Embedded message fragment added")
            }
        }
    }
    
    private fun clearEmbeddedMessages() {
        val fragment = supportFragmentManager.findFragmentById(R.id.embedded_message_container)
        if (fragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commitNowAllowingStateLoss()
            Log.d(TAG, "Cleared embedded message fragment")
        }
    }
    
    private fun updateStatus(status: String) {
        statusTextView.text = "Status: $status"
        Log.d(TAG, "Status: $status")
    }
} 