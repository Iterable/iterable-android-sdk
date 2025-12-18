package com.iterable.inbox_customization

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.navigation.Navigation
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableInAppManager
import com.iterable.inbox_customization.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), IterableInAppManager.Listener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()
    }

    private fun setupNavigation() {
        val navController = Navigation.findNavController(this, R.id.mainNavigationFragment)
        setupActionBarWithNavController(navController)
        binding.bottomNavigationView.setupWithNavController(navController)
        
        // Hide bottom navigation when on ConfigurationFragment
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.configurationFragment) {
                binding.bottomNavigationView.visibility = View.GONE
            } else {
                binding.bottomNavigationView.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //Add listener to inbox updates only if SDK is initialized
        try {
            val iterableApi = IterableApi.getInstance()
            iterableApi.inAppManager.addListener(this)
            onInboxUpdated()
        } catch (e: Exception) {
            // SDK not initialized yet, skip listener setup
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            IterableApi.getInstance().inAppManager.removeListener(this)
        } catch (e: Exception) {
            // SDK not initialized, skip listener removal
        }
    }

    override fun onInboxUpdated() {
        try {
            val unreadCount = IterableApi.getInstance().inAppManager.unreadInboxMessagesCount
            updateNotificationBadge(unreadCount)
        } catch (e: Exception) {
            // SDK not initialized, skip badge update
        }
    }

    private fun updateNotificationBadge(value: Int) {
        val simpleInboxBadge = binding.bottomNavigationView.getOrCreateBadge(R.id.simpleInboxFragment)
        val customInboxBadge = binding.bottomNavigationView.getOrCreateBadge(R.id.customInboxFragment)
        simpleInboxBadge.number = value
        customInboxBadge.number = value
    }
}
