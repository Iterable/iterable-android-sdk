package com.iterable.inbox_customization

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
    }

    override fun onResume() {
        super.onResume()
        //Add listener to inbox updates
        IterableApi.getInstance().inAppManager.addListener(this)
        onInboxUpdated()
    }

    override fun onPause() {
        super.onPause()
        IterableApi.getInstance().inAppManager.removeListener(this)
    }

    override fun onInboxUpdated() {
        updateNotificationBadge(IterableApi.getInstance().inAppManager.unreadInboxMessagesCount)
    }

    private fun updateNotificationBadge(value: Int) {
        val simpleInboxBadge = binding.bottomNavigationView.getOrCreateBadge(R.id.simpleInboxFragment)
        val customInboxBadge = binding.bottomNavigationView.getOrCreateBadge(R.id.customInboxFragment)
        simpleInboxBadge.number = value
        customInboxBadge.number = value
    }
}
