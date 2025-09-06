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
                        return true
                    }
                })
                .build()
            
            IterableApi.initialize(this, BuildConfig.ITERABLE_API_KEY, config)
            
            // Set the user email for integration testing
            val userEmail = "akshay.ayyanchira@iterable.com"
            IterableApi.getInstance().setEmail(userEmail)
            
            Log.d(TAG, "Iterable SDK initialized successfully with email: $userEmail")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Iterable SDK", e)
        }
    }
    
    private fun setupUI() {
        // Set API key text
        findViewById<android.widget.TextView>(R.id.tvApiKey).text = "API Key: ${BuildConfig.ITERABLE_API_KEY}"
        
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
} 