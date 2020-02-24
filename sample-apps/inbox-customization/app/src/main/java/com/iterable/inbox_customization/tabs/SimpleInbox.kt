package com.iterable.inbox_customization.tabs

import android.content.Intent
import com.iterable.inbox_customization.MainFragment
import com.iterable.inbox_customization.util.DataManager
import com.iterable.iterableapi.ui.inbox.IterableInboxActivity
import com.iterable.iterableapi.ui.inbox.IterableInboxFragment

class SimpleInbox : IterableInboxFragment() {

}

fun MainFragment.onSimpleInboxClicked() {
    DataManager.loadData("simple-inbox-messages.json")
    startActivity(Intent(activity, IterableInboxActivity::class.java))
}