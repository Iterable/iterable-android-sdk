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

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val apiKeyInput = view.findViewById<TextInputEditText>(R.id.apiKeyInput)
        val continueButton = view.findViewById<android.widget.Button>(R.id.continueButton)
        val resetButton = view.findViewById<android.widget.Button>(R.id.resetButton)
        val emailInputLayout = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.emailInputLayout)
        val emailInput = view.findViewById<TextInputEditText>(R.id.emailInput)
        val signInButton = view.findViewById<android.widget.Button>(R.id.signInButton)

        continueButton.setOnClickListener {
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
                // Disable and gray out continue button
                continueButton.isEnabled = false
                continueButton.alpha = 0.5f
                // Disable API key input
                apiKeyInput?.isEnabled = false
                // Show email/userId input and sign in button
                emailInputLayout?.visibility = View.VISIBLE
                signInButton?.visibility = View.VISIBLE
            }
        }

        resetButton.setOnClickListener {
            // Clear all fields
            apiKeyInput?.text?.clear()
            emailInput?.text?.clear()
            // Re-enable continue button
            continueButton.isEnabled = true
            continueButton.alpha = 1.0f
            // Re-enable API key input
            apiKeyInput?.isEnabled = true
            // Hide email/userId input and sign in button
            emailInputLayout?.visibility = View.GONE
            signInButton?.visibility = View.GONE
            // Dismiss keyboard
            dismissKeyboard(view)
        }

        signInButton.setOnClickListener {
            val emailOrUserId = emailInput?.text?.toString()?.trim()
            if (TextUtils.isEmpty(emailOrUserId)) {
                Toast.makeText(context, "Please enter an email or user ID", Toast.LENGTH_SHORT).show()
            } else {
                // Dismiss keyboard
                dismissKeyboard(emailInput)
                // Set email/userId with success and failure handlers
                try {
                    IterableApi.getInstance().setEmail(
                        emailOrUserId,
                        object : IterableHelper.SuccessHandler {
                            override fun onSuccess(data: org.json.JSONObject) {
                                activity?.runOnUiThread {
                                    // Load data after successful sign in
                                    DataManager.loadData("simple-inbox-messages.json")
                                    // Navigate to the API list screen
                                    findNavController().navigate(R.id.mainFragment)
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

