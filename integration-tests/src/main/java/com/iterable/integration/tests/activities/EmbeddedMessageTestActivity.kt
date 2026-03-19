package com.iterable.integration.tests.activities

import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.iterable.iterableapi.IterableApi
import com.iterable.integration.tests.R
import com.iterable.iterableapi.ui.embedded.IterableEmbeddedView
import com.iterable.iterableapi.ui.embedded.IterableEmbeddedViewType
import org.json.JSONObject

class EmbeddedMessageTestActivity : AppCompatActivity() {
    
    private lateinit var statusTextView: TextView
    private lateinit var checkIsPremiumButton: Button
    private lateinit var isPremiumSwitch: Switch
    private lateinit var syncMessagesButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_embedded_message_test)
        
        initializeViews()
        setupClickListeners()
        updateStatus("Ready to test embedded messages")
    }
    
    private fun initializeViews() {
        statusTextView = findViewById(R.id.status_text)
        checkIsPremiumButton = findViewById(R.id.btnCheckIsPremium)
        isPremiumSwitch = findViewById(R.id.switchIsPremium)
        syncMessagesButton = findViewById(R.id.btnSyncMessages)
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
        AlertDialog.Builder(this)
            .setTitle("isPremium Status")
            .setMessage("User data fields are stored on the server, not in the SDK.\n\n" +
                    "To check isPremium status:\n" +
                    "1. Check server logs/dashboard\n" +
                    "2. Call server API to get user profile\n" +
                    "3. Check logcat for updateUser calls")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun updateUserIsPremium(isPremium: Boolean) {
        val statusText = if (isPremium) "true" else "false"
        updateStatus("Updating user (isPremium = $statusText)...")
        
        val dataFields = JSONObject().apply {
            put("isPremium", isPremium)
        }
        
        isPremiumSwitch.isEnabled = false
        IterableApi.getInstance().updateUser(dataFields)
        
        Toast.makeText(this, "updateUser called (isPremium = $statusText)\nWait 5 seconds then sync messages", Toast.LENGTH_LONG).show()
        
        isPremiumSwitch.postDelayed({
            isPremiumSwitch.isEnabled = true
            updateStatus("User updated (isPremium = $statusText) - Sync messages to verify")
        }, 1000)
    }
    
    private fun syncEmbeddedMessages() {
        updateStatus("Syncing embedded messages...")
        IterableApi.getInstance().embeddedManager.syncMessages()
        
        Thread {
            Thread.sleep(2000)
            
            runOnUiThread {
                val placementIds = IterableApi.getInstance().embeddedManager.getPlacementIds()
                val messageCount = placementIds.sumOf { placementId ->
                    IterableApi.getInstance().embeddedManager.getMessages(placementId)?.size ?: 0
                }
                
                val statusMessage = if (messageCount > 0) {
                    "✅ Found $messageCount message(s) in ${placementIds.size} placement(s)"
                } else {
                    "⚠️ No messages found. Check user eligibility and campaign configuration"
                }
                
                updateStatus(statusMessage)
                Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show()
                
                if (messageCount > 0) {
                    displayEmbeddedMessages(placementIds)
                } else {
                    clearEmbeddedMessages()
                }
            }
        }.start()
    }
    
    private fun displayEmbeddedMessages(placementIds: List<Long>) {
        val firstPlacementId = placementIds.firstOrNull() ?: return
        val messages = IterableApi.getInstance().embeddedManager.getMessages(firstPlacementId) ?: return
        
        if (messages.isNotEmpty()) {
            val firstMessage = messages.first()
            val fragment = IterableEmbeddedView.newInstance(IterableEmbeddedViewType.BANNER, firstMessage, null)
            supportFragmentManager.beginTransaction()
                .replace(R.id.embedded_message_container, fragment)
                .commitNowAllowingStateLoss()
            
            updateStatus("✅ Message displayed: ${firstMessage.metadata.messageId}")
        }
    }
    
    private fun clearEmbeddedMessages() {
        val fragment = supportFragmentManager.findFragmentById(R.id.embedded_message_container)
        fragment?.let {
            supportFragmentManager.beginTransaction()
                .remove(it)
                .commitNowAllowingStateLoss()
        }
    }
    
    private fun updateStatus(status: String) {
        statusTextView.text = "Status: $status"
    }
} 