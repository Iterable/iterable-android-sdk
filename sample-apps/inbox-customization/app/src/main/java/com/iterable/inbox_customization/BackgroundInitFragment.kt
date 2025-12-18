package com.iterable.inbox_customization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.iterable.inbox_customization.util.DataManager
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableInitializationCallback

class BackgroundInitFragment : Fragment() {
    private lateinit var statusText: TextView
    private lateinit var initButton: Button
    private lateinit var testApiButton: Button
    private var isInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_background_init, container, false)

        statusText = view.findViewById(R.id.statusText)
        initButton = view.findViewById(R.id.initButton)
        testApiButton = view.findViewById(R.id.testApiButton)

        setupUI()
        return view
    }

    private fun setupUI() {
        statusText.text = "SDK not initialized"
        
        initButton.setOnClickListener {
            startBackgroundInitialization()
        }

        testApiButton.setOnClickListener {
            testApiCall()
        }
        
        // Initially disable test API button
        testApiButton.isEnabled = false
    }

    private fun startBackgroundInitialization() {
        statusText.text = "Initializing SDK in background..."
        initButton.isEnabled = false

        val callback = object : IterableInitializationCallback {
            override fun onSDKInitialized() {
                activity?.runOnUiThread {
                    statusText.text = "SDK initialized successfully!"
                    testApiButton.isEnabled = true
                    isInitialized = true
                }
            }
        }

        // This returns immediately and doesn't block the UI
        val apiKey = DataManager.getStoredApiKey() ?: "demoApiKey"
        DataManager.initializeIterableApiInBackground(requireContext(), apiKey, callback)
        
        // Show that we can continue doing other work immediately
        statusText.append("\nInitialization started - UI remains responsive!")
    }

    private fun testApiCall() {
        if (isInitialized) {
            // Make a test API call to demonstrate the SDK is working
            val email = IterableApi.getInstance().email
            statusText.append("\nCurrent user email: ${email ?: "Not set"}")
            
            // Demonstrate that we can make API calls after initialization
            statusText.append("\nAPI calls are now working!")
        } else {
            statusText.append("\nSDK not yet initialized - call will be queued")
            // This call will be queued and executed after initialization
            IterableApi.getInstance().setEmail("queued-user@example.com")
        }
    }
}



