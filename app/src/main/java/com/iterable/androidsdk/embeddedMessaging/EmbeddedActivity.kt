package com.iterable.androidsdk.embeddedMessaging

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.iterable.iterableapi.testapp.R

import kotlinx.android.synthetic.main.activity_main.bottomNavigationView

class EmbeddedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_embedded)
        setupNavigation()
    }

    private fun setupNavigation() {
        val navController = Navigation.findNavController(this, R.id.embeddedNavigationFragment)
        setupActionBarWithNavController(navController)
        bottomNavigationView.setupWithNavController(navController)
    }
}