package com.iterable.iterableapi.ui.inbox

import android.os.Bundle
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity

import com.iterable.iterableapi.IterableLogger
import com.iterable.iterableapi.ui.R

class IterableInboxMessageActivity : AppCompatActivity() {
    
    companion object {
        const val ARG_MESSAGE_ID = "messageId"
    }

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.iterable_inbox_message_activity)
        IterableLogger.printInfo()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, IterableInboxMessageFragment.newInstance(intent.getStringExtra(ARG_MESSAGE_ID)))
                .commitNow()
        }
    }
}
