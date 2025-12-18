package com.iterable.inbox_customization

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
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

        val storedApiKey = DataManager.getStoredApiKey(requireContext())
        var isSDKInitialized = IterableApi.isSDKInitialized()
        if (!isSDKInitialized && !TextUtils.isEmpty(storedApiKey)) {
            DataManager.initializeIterableApi(requireContext(), storedApiKey!!)
            isSDKInitialized = IterableApi.isSDKInitialized()
        }
        
        val storedEmail = if (isSDKInitialized) {
            try {
                IterableApi.getInstance().getEmail().also { email ->
                    isSignedIn = !TextUtils.isEmpty(email)
                }
            } catch (e: Exception) {
                isSignedIn = false
                null
            }
        } else {
            isSignedIn = false
            null
        }

        storedApiKey?.let {
            apiKeyInput?.setText(it)
            apiKeyInput?.isEnabled = false
        }
        if (isSDKInitialized || !storedApiKey.isNullOrEmpty()) {
            initializeSDKButton?.visibility = View.GONE
            emailInputLayout?.visibility = View.VISIBLE
            signInButton?.visibility = View.VISIBLE
        }
        updateEmailUI(storedEmail, emailInput, signInButton, continueButton)

        val navController = findNavController()
        if (savedInstanceState == null && !TextUtils.isEmpty(storedApiKey) && !TextUtils.isEmpty(storedEmail) 
            && navController.graph.startDestinationId == R.id.configurationFragment 
            && navController.previousBackStackEntry == null) {
            view.post {
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
                dismissKeyboard(apiKeyInput)
                DataManager.initializeIterableApi(requireContext(), apiKey!!)
                Toast.makeText(context, "Initialization successful. Sign in with an email-id or userID to continue...", Toast.LENGTH_LONG).show()
                apiKeyInput?.isEnabled = false
                initializeSDKButton?.visibility = View.GONE
                emailInputLayout?.visibility = View.VISIBLE
                signInButton?.visibility = View.VISIBLE
            }
        }

        continueButton.setOnClickListener {
            findNavController().navigate(R.id.mainFragment)
        }

        resetButton.setOnClickListener {
            if (IterableApi.isSDKInitialized()) {
                try {
                    IterableApi.getInstance().setEmail(null)
                } catch (e: Exception) {
                    Log.w("ConfigurationFragment", "Error clearing email: ${e.message}")
                }
            }
            DataManager.clearApiKeyFromPreferences(requireContext())
            apiKeyInput?.text?.clear()
            emailInput?.text?.clear()
            initializeSDKButton?.visibility = View.VISIBLE
            apiKeyInput?.isEnabled = true
            emailInputLayout?.visibility = View.GONE
            signInButton?.visibility = View.GONE
            continueButton?.visibility = View.GONE
            isSignedIn = false
            signInButton?.text = "Sign in"
            dismissKeyboard(view)
        }

        signInButton.setOnClickListener {
            val currentEmail = try { IterableApi.getInstance().getEmail() } catch (e: Exception) { null }
            val hasEmail = !TextUtils.isEmpty(currentEmail)
            
            if (hasEmail || isSignedIn) {
                try {
                    IterableApi.getInstance().setEmail(null)
                    emailInput?.text?.clear()
                    isSignedIn = false
                    signInButton.text = "Sign in"
                    continueButton?.visibility = View.GONE
                    Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error signing out: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                val emailOrUserId = emailInput?.text?.toString()?.trim()
                if (TextUtils.isEmpty(emailOrUserId)) {
                    Toast.makeText(context, "Please enter an email or user ID", Toast.LENGTH_SHORT).show()
                } else {
                    dismissKeyboard(emailInput)
                    continueButton?.visibility = View.VISIBLE
                    continueButton?.isEnabled = true
                    continueButton?.alpha = 1.0f
                    try {
                        IterableApi.getInstance().setEmail(
                            emailOrUserId,
                            object : IterableHelper.SuccessHandler {
                                override fun onSuccess(data: org.json.JSONObject) {
                                    activity?.runOnUiThread {
                                        isSignedIn = true
                                        signInButton.text = "Sign out"
                                        Toast.makeText(context, "Signed in successfully", Toast.LENGTH_SHORT).show()
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

    override fun onResume() {
        super.onResume()
        updateSignInButtonText()
    }

    private fun updateSignInButtonText() {
        view?.findViewById<android.widget.Button>(R.id.signInButton)?.let { signInButton ->
            try {
                val email = IterableApi.getInstance().getEmail()
                isSignedIn = !TextUtils.isEmpty(email)
                signInButton.text = if (isSignedIn) "Sign out" else "Sign in"
            } catch (e: Exception) {
                signInButton.text = "Sign in"
                isSignedIn = false
            }
        }
    }

    private fun updateEmailUI(email: String?, emailInput: TextInputEditText?, signInButton: android.widget.Button?, continueButton: android.widget.Button?) {
        if (!TextUtils.isEmpty(email)) {
            emailInput?.setText(email)
            signInButton?.text = "Sign out"
            isSignedIn = true
            continueButton?.visibility = View.VISIBLE
        } else {
            signInButton?.text = "Sign in"
            continueButton?.visibility = View.GONE
        }
    }

    private fun dismissKeyboard(view: View?) {
        view?.let {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
    }
}

