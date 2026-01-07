package com.iterable.inbox_customization

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
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
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Handle window insets for safe areas
        // Root view gets top, left, right padding for status bar and side cutouts
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = insets.top,
                left = insets.left,
                right = insets.right
            )
            windowInsets
        }
        
        // Bottom navigation view handles its own bottom padding for navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigationView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = insets.bottom)
            windowInsets
        }
        
        setupNavigation()
    }

    private fun setupNavigation() {
        val navController = Navigation.findNavController(this, R.id.mainNavigationFragment)
        setupActionBarWithNavController(navController)
        binding.bottomNavigationView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigationView.visibility = if (destination.id == R.id.configurationFragment) View.GONE else View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            IterableApi.getInstance().inAppManager.addListener(this)
            onInboxUpdated()
        } catch (e: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        try {
            IterableApi.getInstance().inAppManager.removeListener(this)
        } catch (e: Exception) {}
    }

    override fun onInboxUpdated() {
        try {
            updateNotificationBadge(IterableApi.getInstance().inAppManager.unreadInboxMessagesCount)
        } catch (e: Exception) {}
    }

    private fun updateNotificationBadge(value: Int) {
        val simpleInboxBadge = binding.bottomNavigationView.getOrCreateBadge(R.id.simpleInboxFragment)
        val customInboxBadge = binding.bottomNavigationView.getOrCreateBadge(R.id.customInboxFragment)
        simpleInboxBadge.number = value
        customInboxBadge.number = value
    }
}
