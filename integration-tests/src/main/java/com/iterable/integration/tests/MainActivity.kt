package com.iterable.integration.tests

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableConfig
import com.iterable.iterableapi.IterableUrlHandler
import com.iterable.integration.tests.activities.*
import com.iterable.integration.tests.utils.IntegrationTestUtils
import com.iterable.integration.tests.utils.TestUserEmailOverride
import com.iterable.integration.tests.utils.maskEmail
import com.iterable.integration.tests.TestConstants

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "IntegrationMainActivity"
        const val EXTRA_DEEP_LINK_URL = "deep_link_url"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeIterableSDK()
        setupUI()
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun initializeIterableSDK() {
        try {
            // Check if we're in test mode - if so, skip initialization
            // The test will handle SDK initialization with custom handlers
            val isTestMode = System.getProperty("iterable.test.mode") == "true"
            if (isTestMode) {
                Log.d(TAG, "🔧 Test mode detected - skipping MainActivity SDK initialization")
                Log.d(TAG, "🔧 Test will handle SDK initialization with custom handlers")
                return
            }
            
            Log.d(TAG, "Normal mode - initializing SDK with default handlers")
            
            val config = IterableConfig.Builder()
                .setAutoPushRegistration(true)
                .setEnableEmbeddedMessaging(true)
                .setInAppDisplayInterval(2.0)
                .setUrlHandler(object : IterableUrlHandler {
                    override fun handleIterableURL(url: android.net.Uri, context: com.iterable.iterableapi.IterableActionContext): Boolean {
                        Log.d(TAG, "Deep link handled: $url")
                        // Navigate to deep link test activity
                        val intent = Intent(this@MainActivity, DeepLinkTestActivity::class.java)
                        intent.putExtra(EXTRA_DEEP_LINK_URL, url.toString())
                        startActivity(intent)
                        return false
                    }
                })
                .setCustomActionHandler(object : com.iterable.iterableapi.IterableCustomActionHandler {
                    override fun handleIterableCustomAction(
                        action: com.iterable.iterableapi.IterableAction,
                        actionContext: com.iterable.iterableapi.IterableActionContext
                    ): Boolean {
                        val actionType = action.getType()
                        // Log action.data()
                        val actionData = action.getData()
                        Log.d(TAG, "Custom action received: type=$actionType, data=$actionData")
                        // You can add custom logic here to handle different action types
                        // For now, just log and return true to indicate the action was handled
                        return false
                    }
                })
                .build()
            
            IterableApi.initialize(this, BuildConfig.ITERABLE_API_KEY, config)

            // Override (set via the field on this screen) takes precedence over the
            // BuildConfig email so a developer can validate against any user without
            // editing local.properties or rebuilding.
            val userEmail = TestUserEmailOverride.get(this) ?: TestConstants.TEST_USER_EMAIL
            IterableApi.getInstance().setEmail(userEmail)

            Log.d(TAG, "Iterable SDK initialized successfully with email: ${maskEmail(userEmail)}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Iterable SDK", e)
        }
    }
    
    private fun setupUI() {
        val apiKey = BuildConfig.ITERABLE_API_KEY
        val keyDisplay = when {
            apiKey.isEmpty() -> "API Key: (empty)"
            apiKey.length < 8 -> "API Key: (length=${apiKey.length})"
            else -> "API Key: ****${apiKey.takeLast(4)} (length=${apiKey.length})"
        }
        findViewById<android.widget.TextView>(R.id.tvApiKey).text = keyDisplay

        setupTestEmailOverride()

        findViewById<android.widget.Button>(R.id.btnPushNotifications).setOnClickListener {
            startActivity(Intent(this@MainActivity, PushNotificationTestActivity::class.java))
        }
        
        findViewById<android.widget.Button>(R.id.btnInAppMessages).setOnClickListener {
            startActivity(Intent(this@MainActivity, InAppMessageTestActivity::class.java))
        }
        
        findViewById<android.widget.Button>(R.id.btnEmbeddedMessages).setOnClickListener {
            startActivity(Intent(this@MainActivity, EmbeddedMessageTestActivity::class.java))
        }
        
        findViewById<android.widget.Button>(R.id.btnDeepLinking).setOnClickListener {
            startActivity(Intent(this@MainActivity, DeepLinkTestActivity::class.java))
        }
        
        findViewById<android.widget.Button>(R.id.btnCampaignTrigger).setOnClickListener {
            startActivity(Intent(this@MainActivity, CampaignTriggerTestActivity::class.java))
        }
        
        findViewById<android.widget.Button>(R.id.btnRunAllTests).setOnClickListener {
            IntegrationTestUtils(this@MainActivity).runAllIntegrationTests(this@MainActivity)
        }
    }
    
    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            Log.d(TAG, "Received deep link: $uri")
            // Handle deep link
            val intent = Intent(this, DeepLinkTestActivity::class.java)
            intent.putExtra(EXTRA_DEEP_LINK_URL, uri.toString())
            startActivity(intent)
        }
    }

    private fun setupTestEmailOverride() {
        val currentLabel = findViewById<android.widget.TextView>(R.id.tvCurrentTestEmail)
        val editField = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTestEmailOverride)
        val saveButton = findViewById<android.widget.Button>(R.id.btnSaveTestEmail)
        val clearButton = findViewById<android.widget.Button>(R.id.btnClearTestEmail)

        fun refresh() {
            val override = TestUserEmailOverride.get(this)
            val effective = override ?: TestConstants.TEST_USER_EMAIL
            val source = if (override != null) "override" else "BuildConfig"
            currentLabel.text = "Current: ${maskEmail(effective)} ($source)"
            editField.setText(override ?: "")
        }
        refresh()

        saveButton.setOnClickListener {
            val email = editField.text?.toString()?.trim().orEmpty()
            if (email.isEmpty()) {
                android.widget.Toast.makeText(this, "Enter an email first", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            TestUserEmailOverride.set(this, email)
            IterableApi.getInstance().setEmail(email)
            refresh()
            android.widget.Toast.makeText(this, "Test email override saved", android.widget.Toast.LENGTH_SHORT).show()
        }

        clearButton.setOnClickListener {
            TestUserEmailOverride.clear(this)
            IterableApi.getInstance().setEmail(TestConstants.TEST_USER_EMAIL)
            refresh()
            android.widget.Toast.makeText(this, "Test email override cleared", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}