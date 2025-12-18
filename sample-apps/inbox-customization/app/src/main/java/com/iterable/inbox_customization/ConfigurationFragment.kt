package com.iterable.inbox_customization

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.iterable.inbox_customization.util.DataManager
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableHelper

class ConfigurationFragment : Fragment() {
    private var isSignedIn = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        val apiKeyInput = view.findViewById<TextInputEditText>(R.id.apiKeyInput)
        val initializeSDKButton = view.findViewById<android.widget.Button>(R.id.initializeSDKButton)
        val continueButton = view.findViewById<android.widget.Button>(R.id.continueButton)
        val resetButton = view.findViewById<android.widget.Button>(R.id.resetButton)
        val emailInputLayout = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.emailInputLayout)
        val emailInput = view.findViewById<TextInputEditText>(R.id.emailInput)
        val signInButton = view.findViewById<android.widget.Button>(R.id.signInButton)

        // Check for stored API key
        val storedApiKey = DataManager.getStoredApiKey()
        
        // Always check if IterableApi is initialized and get email
        var storedEmail: String? = null
        try {
            val iterableApi = IterableApi.getInstance()
            storedEmail = iterableApi.getEmail()
            isSignedIn = !TextUtils.isEmpty(storedEmail)
        } catch (e: Exception) {
            // SDK not initialized yet
            isSignedIn = false
            storedEmail = null
        }

        // Populate fields if available
        if (!TextUtils.isEmpty(storedApiKey)) {
            apiKeyInput?.setText(storedApiKey)
            apiKeyInput?.isEnabled = false
            initializeSDKButton?.visibility = View.GONE
            emailInputLayout?.visibility = View.VISIBLE
            signInButton?.visibility = View.VISIBLE
        }

        // Always prepopulate email field if IterableApi has an email
        if (!TextUtils.isEmpty(storedEmail)) {
            emailInput?.setText(storedEmail)
            signInButton?.text = "Sign out"
            isSignedIn = true
            // Show continue button if both API key and email are set
            continueButton?.visibility = View.VISIBLE
        } else {
            signInButton?.text = "Sign in"
            continueButton?.visibility = View.GONE
        }

        // Auto-navigate to mainFragment if both API key and email are already set
        // Only do this on initial app launch (start destination), not when navigating from other fragments
        val navController = findNavController()
        val isStartDestination = navController.graph.startDestinationId == R.id.configurationFragment
        val hasBackStack = navController.previousBackStackEntry != null
        if (savedInstanceState == null && !TextUtils.isEmpty(storedApiKey) && !TextUtils.isEmpty(storedEmail) 
            && isStartDestination && !hasBackStack) {
            // Both are set - navigate to main fragment automatically
            view.post {
                // Double-check we're still on configuration fragment before navigating
                if (navController.currentDestination?.id == R.id.configurationFragment) {
                    navController.navigate(R.id.mainFragment)
                }
            }
        }

        initializeSDKButton.setOnClickListener {
            val apiKey = apiKeyInput?.text?.toString()?.trim()
            if (TextUtils.isEmpty(apiKey)) {
                Toast.makeText(context, "Please enter an API key", Toast.LENGTH_SHORT).show()
            } else {
                // Dismiss keyboard
                dismissKeyboard(apiKeyInput)
                // Initialize SDK with the provided API key
                DataManager.initializeIterableApi(requireContext(), apiKey!!)
                // Show success toast
                Toast.makeText(context, "Initialization successful. Sign in with an email-id or userID to continue...", Toast.LENGTH_LONG).show()
                // Disable API key input and hide InitializeSDK button
                apiKeyInput?.isEnabled = false
                initializeSDKButton?.visibility = View.GONE
                // Show email/userId input and sign in button
                emailInputLayout?.visibility = View.VISIBLE
                signInButton?.visibility = View.VISIBLE
            }
        }

        continueButton.setOnClickListener {
            // Navigate to main fragment
            findNavController().navigate(R.id.mainFragment)
        }

        resetButton.setOnClickListener {
            // Clear all fields
            apiKeyInput?.text?.clear()
            emailInput?.text?.clear()
            // Show InitializeSDK button again
            initializeSDKButton?.visibility = View.VISIBLE
            // Re-enable API key input
            apiKeyInput?.isEnabled = true
            // Hide email/userId input, sign in button, and continue button
            emailInputLayout?.visibility = View.GONE
            signInButton?.visibility = View.GONE
            continueButton?.visibility = View.GONE
            // Reset sign in state
            isSignedIn = false
            signInButton?.text = "Sign in"
            // Dismiss keyboard
            dismissKeyboard(view)
        }

        signInButton.setOnClickListener {
            if (isSignedIn) {
                // Sign out
                try {
                    IterableApi.getInstance().setEmail(null)
                    emailInput?.text?.clear()
                    isSignedIn = false
                    signInButton.text = "Sign in"
                    // Hide continue button when signed out
                    continueButton?.visibility = View.GONE
                    Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error signing out: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                // Sign in
                val emailOrUserId = emailInput?.text?.toString()?.trim()
                if (TextUtils.isEmpty(emailOrUserId)) {
                    Toast.makeText(context, "Please enter an email or user ID", Toast.LENGTH_SHORT).show()
                } else {
                    // Dismiss keyboard
                    dismissKeyboard(emailInput)
                    // Show continue button immediately when setEmail is clicked
                    continueButton?.visibility = View.VISIBLE
                    continueButton?.isEnabled = true
                    continueButton?.alpha = 1.0f
                    // Set email/userId with success and failure handlers
                    try {
                        IterableApi.getInstance().setEmail(
                            emailOrUserId,
                            object : IterableHelper.SuccessHandler {
                                override fun onSuccess(data: org.json.JSONObject) {
                                    activity?.runOnUiThread {
                                        isSignedIn = true
                                        signInButton.text = "Sign out"
                                        Toast.makeText(context, "Signed in successfully", Toast.LENGTH_SHORT).show()
                                        // Load data after successful sign in
                                        DataManager.loadData("simple-inbox-messages.json")
                                    }
                                }
                            },
                            object : IterableHelper.FailureHandler {
                                override fun onFailure(reason: String, data: org.json.JSONObject?) {
                                    activity?.runOnUiThread {
                                        Toast.makeText(context, "Sign in failed: $reason", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }


        return view
    }


    private fun dismissKeyboard(view: View?) {
        view?.let {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
    }
}

