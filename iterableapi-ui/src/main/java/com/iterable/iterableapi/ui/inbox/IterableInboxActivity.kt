package com.iterable.iterableapi.ui.inbox

import android.content.Intent
import android.os.Bundle
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity

import com.iterable.iterableapi.IterableConstants
import com.iterable.iterableapi.IterableLogger
import com.iterable.iterableapi.ui.R

import com.iterable.iterableapi.ui.inbox.IterableInboxFragment.Companion.INBOX_MODE
import com.iterable.iterableapi.ui.inbox.IterableInboxFragment.Companion.ITEM_LAYOUT_ID

/**
 * An activity wrapping [IterableInboxFragment]
 *
 * Supports optional extras:
 * [IterableInboxFragment.INBOX_MODE] - [InboxMode] value with the inbox mode
 * [IterableInboxFragment.ITEM_LAYOUT_ID] - Layout resource id for inbox items
 */
class IterableInboxActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "IterableInboxActivity"
        const val ACTIVITY_TITLE = "activityTitle"
    }

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IterableLogger.printInfo()
        setContentView(R.layout.iterable_inbox_activity)
        
        val inboxFragment = intent?.let {
            val inboxModeExtra = it.getSerializableExtra(INBOX_MODE)
            val itemLayoutId = it.getIntExtra(ITEM_LAYOUT_ID, 0)
            var inboxMode = InboxMode.POPUP
            if (inboxModeExtra is InboxMode) {
                inboxMode = inboxModeExtra
            }
            
            val extraBundle = intent.extras
            val noMessageTitle = extraBundle?.getString(IterableConstants.NO_MESSAGES_TITLE, null)
            val noMessageBody = extraBundle?.getString(IterableConstants.NO_MESSAGES_BODY, null)
            
            val fragment = IterableInboxFragment.newInstance(inboxMode, itemLayoutId, noMessageTitle, noMessageBody)

            val activityTitle = it.getStringExtra(ACTIVITY_TITLE)
            if (activityTitle != null) {
                setTitle(activityTitle)
            }
            
            fragment
        } ?: IterableInboxFragment.newInstance()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, inboxFragment)
                .commitNow()
        }
    }

}
