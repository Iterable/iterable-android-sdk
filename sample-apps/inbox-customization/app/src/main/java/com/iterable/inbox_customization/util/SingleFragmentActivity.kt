package com.iterable.inbox_customization.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class SingleFragmentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragmentName = intent.getStringExtra(FRAGMENT_NAME)
        val fragment: Fragment = Fragment.instantiate(this, fragmentName)
        val fragmentArguments = intent.getBundleExtra(FRAGMENT_ARGUMENTS)
        fragment.arguments = fragmentArguments
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment, "fragment").commit()
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