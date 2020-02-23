package com.iterable.inbox_customization.customizations

import android.os.Bundle
import com.iterable.inbox_customization.MainFragment
import com.iterable.inbox_customization.util.DataManager
import com.iterable.inbox_customization.util.SingleFragmentActivity
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