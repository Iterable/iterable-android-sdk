package com.iterable.integration.tests.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.iterable.integration.tests.R
import com.iterable.iterableapi.IterableApi
import org.json.JSONObject

/**
 * Activity for testing deep link handling.
 * 
 * This activity can be launched in two ways:
 * 1. As a deep link destination when app handles a URL (via intent-filter)
 * 2. As a navigation target from MainActivity for manual testing
 * 
 * Deep link handling flow:
 * - External source (browser, email, etc.) → App Link/Deep Link → MainActivity → This Activity
 * - Test infrastructure uses adb to simulate external deep links
 */
class DeepLinkTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "DeepLinkTestActivity"
        const val EXTRA_DEEP_LINK_URL = "deep_link_url"
        const val EXTRA_DEEP_LINK_PATH = "deep_link_path"
        
        // Accessibility IDs for UI testing
        const val VIEW_ID_HEADER = "deep-link-view-header"
        const val VIEW_ID_URL = "deep-link-view-url"
        const val VIEW_ID_PATH = "deep-link-view-path"
        const val VIEW_ID_HOST = "deep-link-view-host"
        const val VIEW_ID_SCHEME = "deep-link-view-scheme"
        const val VIEW_ID_STATUS = "deep-link-view-status"
        const val VIEW_ID_CLOSE_BUTTON = "deep-link-view-close-button"
    }
    
    // Track last received deep link for test verification
    private var lastReceivedUrl: String? = null
    private var lastReceivedPath: String? = null
    private var lastReceivedHost: String? = null
    private var lastReceivedScheme: String? = null
    
    // UI elements
    private lateinit var tvHeader: TextView
    private lateinit var tvUrl: TextView
    private lateinit var tvPath: TextView
    private lateinit var tvHost: TextView
    private lateinit var tvScheme: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnClose: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deep_link_test)
        
        Log.d(TAG, "Deep Link Test Activity started")
        
        setupUI()
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called")
        intent?.let { handleIntent(it) }
    }
    
    private fun setupUI() {
        tvHeader = findViewById(R.id.tvDeepLinkHeader)
        tvUrl = findViewById(R.id.tvDeepLinkUrl)
        tvPath = findViewById(R.id.tvDeepLinkPath)
        tvHost = findViewById(R.id.tvDeepLinkHost)
        tvScheme = findViewById(R.id.tvDeepLinkScheme)
        tvStatus = findViewById(R.id.tvDeepLinkStatus)
        btnClose = findViewById(R.id.btnCloseDeepLink)
        
        // Set accessibility content descriptions for UI testing
        tvHeader.contentDescription = VIEW_ID_HEADER
        tvUrl.contentDescription = VIEW_ID_URL
        tvPath.contentDescription = VIEW_ID_PATH
        tvHost.contentDescription = VIEW_ID_HOST
        tvScheme.contentDescription = VIEW_ID_SCHEME
        tvStatus.contentDescription = VIEW_ID_STATUS
        btnClose.contentDescription = VIEW_ID_CLOSE_BUTTON
        
        btnClose.setOnClickListener {
            Log.d(TAG, "Close button clicked")
            finish()
        }
        
        // Initial state - waiting for deep link
        updateUI(null)
    }
    
    private fun handleIntent(intent: Intent) {
        Log.d(TAG, "handleIntent: action=${intent.action}, data=${intent.data}")
        
        // Check for direct deep link via intent data (from App Links or custom schemes)
        intent.data?.let { uri ->
            Log.d(TAG, "Received deep link via intent data: $uri")
            processDeepLink(uri)
            return
        }
        
        // Check for URL passed as extra (from MainActivity or SDK URL handler)
        intent.getStringExtra(EXTRA_DEEP_LINK_URL)?.let { urlString ->
            Log.d(TAG, "Received deep link via extra: $urlString")
            try {
                val uri = Uri.parse(urlString)
                processDeepLink(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse deep link URL: $urlString", e)
                updateUIWithError("Failed to parse URL: $urlString")
            }
            return
        }
        
        Log.d(TAG, "No deep link data in intent - showing waiting state")
        updateUI(null)
    }
    
    private fun processDeepLink(uri: Uri) {
        Log.d(TAG, "Processing deep link: $uri")
        
        lastReceivedUrl = uri.toString()
        lastReceivedPath = uri.path
        lastReceivedHost = uri.host
        lastReceivedScheme = uri.scheme
        
        Log.d(TAG, "Deep link details:")
        Log.d(TAG, "  URL: $lastReceivedUrl")
        Log.d(TAG, "  Path: $lastReceivedPath")
        Log.d(TAG, "  Host: $lastReceivedHost")
        Log.d(TAG, "  Scheme: $lastReceivedScheme")
        
        // Track custom event for deep link received (for analytics verification)
        try {
            IterableApi.getInstance().track("deepLinkReceived", 0, 23, JSONObject().apply {
                put("url", lastReceivedUrl ?: "")
                put("path", lastReceivedPath ?: "")
                put("host", lastReceivedHost ?: "")
                put("scheme", lastReceivedScheme ?: "")
            })
        } catch (e: Exception) {
            Log.w(TAG, "Failed to track deep link event", e)
        }
        
        updateUI(uri)
    }
    
    private fun updateUI(uri: Uri?) {
        if (uri != null) {
            tvHeader.text = getHeaderFromPath(uri.path)
            tvUrl.text = "URL: ${uri.toString()}"
            tvPath.text = "Path: ${uri.path ?: "(none)"}"
            tvHost.text = "Host: ${uri.host ?: "(none)"}"
            tvScheme.text = "Scheme: ${uri.scheme ?: "(none)"}"
            tvStatus.text = "Status: Deep link received successfully!"
        } else {
            tvHeader.text = "Deep Link Tests"
            tvUrl.text = "URL: (waiting for deep link...)"
            tvPath.text = "Path: -"
            tvHost.text = "Host: -"
            tvScheme.text = "Scheme: -"
            tvStatus.text = "Status: Waiting for deep link"
        }
    }
    
    private fun updateUIWithError(errorMessage: String) {
        tvHeader.text = "Deep Link Error"
        tvUrl.text = "URL: (error)"
        tvPath.text = "Path: -"
        tvHost.text = "Host: -"
        tvScheme.text = "Scheme: -"
        tvStatus.text = "Status: $errorMessage"
    }
    
    /**
     * Generate a friendly header based on the deep link path.
     * Similar to iOS's UpdateViewController which shows different content based on path.
     */
    private fun getHeaderFromPath(path: String?): String {
        return when {
            path == null -> "Deep Link Received"
            path.contains("/update/hi") -> "👋 Hi!"
            path.contains("/update") -> "📝 Update"
            path.contains("/product") -> "🛍️ Product"
            path.contains("/settings") -> "⚙️ Settings"
            path.contains("/profile") -> "👤 Profile"
            else -> "🔗 Deep Link: $path"
        }
    }
    
    // Getters for test verification
    fun getLastReceivedUrl(): String? = lastReceivedUrl
    fun getLastReceivedPath(): String? = lastReceivedPath
    fun getLastReceivedHost(): String? = lastReceivedHost
    fun getLastReceivedScheme(): String? = lastReceivedScheme
}