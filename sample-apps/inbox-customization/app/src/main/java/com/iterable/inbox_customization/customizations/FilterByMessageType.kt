package com.iterable.inbox_customization.customizations

import android.os.Bundle
import com.iterable.inbox_customization.MainFragment
import com.iterable.inbox_customization.util.DataManager
import com.iterable.inbox_customization.util.SingleFragmentActivity
import com.iterable.iterableapi.ui.inbox.IterableInboxAdapterExtension
import com.iterable.iterableapi.ui.inbox.IterableInboxFragment

fun MainFragment.onFilterByMessageTypeClicked() {
    DataManager.loadData("filter-by-message-type-messages.json")
    val intent = SingleFragmentActivity.createIntentWithFragment(
        activity!!,
        FilterByMessageTypeInboxFragment::class.java
    )
    startActivity(intent)
}

class FilterByMessageTypeInboxFragment : IterableInboxFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFilter { message ->
            message.customPayload.optString("messageType") in setOf("transactional", "promotional")
        }
    }
}