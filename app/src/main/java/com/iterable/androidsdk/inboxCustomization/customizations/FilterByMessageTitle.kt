package com.iterable.androidsdk.inboxCustomization.customizations

import android.os.Bundle
import com.iterable.androidsdk.inboxCustomization.MainFragment
import com.iterable.androidsdk.inboxCustomization.util.DataManager
import com.iterable.androidsdk.inboxCustomization.util.SingleFragmentActivity
import com.iterable.iterableapi.ui.inbox.IterableInboxFragment

fun MainFragment.onFilterByMessageTitleClicked() {
    DataManager.loadData("simple-inbox-messages.json")
    val intent = SingleFragmentActivity.createIntentWithFragment(
        activity!!,
        FilterByMessageTitleInboxFragment::class.java
    )
    startActivity(intent)
}

class FilterByMessageTitleInboxFragment : IterableInboxFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFilter { message ->
            message.inboxMetadata?.title?.contains("mocha") ?: false
        }
    }
}