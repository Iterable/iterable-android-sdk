package com.iterable.inbox_customization.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class SingleFragmentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val fragmentName = intent.getStringExtra(FRAGMENT_NAME)
        val fragment: Fragment? = fragmentName?.let { Fragment.instantiate(this, it) }
        val fragmentArguments = intent.getBundleExtra(FRAGMENT_ARGUMENTS)
        fragment?.arguments = fragmentArguments
        fragment?.let {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, it, "fragment").commit()
        }
    }

    companion object {
        const val FRAGMENT_NAME = "fragmentName"
        const val FRAGMENT_ARGUMENTS = "fragmentArguments"

        fun <T> createIntentWithFragment(activity: FragmentActivity, fragmentClass: Class<T>, arguments: Bundle? = null): Intent {
            val intent = Intent(activity, SingleFragmentActivity::class.java)
            intent.putExtra(FRAGMENT_NAME, fragmentClass.name)
            intent.putExtra(FRAGMENT_ARGUMENTS, arguments)
            return intent
        }
    }
}