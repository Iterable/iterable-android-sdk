package com.iterable.androidsdk.inboxCustomization.tabs

import android.content.Intent
import com.iterable.androidsdk.inboxCustomization.MainFragment
import com.iterable.androidsdk.inboxCustomization.util.DataManager
import com.iterable.iterableapi.ui.inbox.IterableInboxActivity
import com.iterable.iterableapi.ui.inbox.IterableInboxFragment

class SimpleInbox : IterableInboxFragment() {

}

fun MainFragment.onSimpleInboxClicked() {
    DataManager.loadData("simple-inbox-messages.json")
    startActivity(Intent(activity, IterableInboxActivity::class.java))
}