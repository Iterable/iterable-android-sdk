package com.iterable.androidsdk.inboxCustomization

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableInAppManager
import com.iterable.iterableapi.testapp.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), IterableInAppManager.Listener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupNavigation()
    }

    private fun setupNavigation() {
        val navController = Navigation.findNavController(this, R.id.mainNavigationFragment)
        setupActionBarWithNavController(navController)
        bottomNavigationView.setupWithNavController(navController)
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
        val simpleInboxBadge = bottomNavigationView.getOrCreateBadge(R.id.simpleInboxFragment)
        val customInboxBadge = bottomNavigationView.getOrCreateBadge(R.id.customInboxFragment)
        simpleInboxBadge.number = value
        customInboxBadge.number = value
    }
}
