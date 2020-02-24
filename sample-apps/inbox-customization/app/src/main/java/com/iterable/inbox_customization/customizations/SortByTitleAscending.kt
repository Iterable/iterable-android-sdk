package com.iterable.inbox_customization.customizations

import android.os.Bundle
import com.iterable.inbox_customization.MainFragment
import com.iterable.inbox_customization.util.DataManager
import com.iterable.inbox_customization.util.SingleFragmentActivity
import com.iterable.iterableapi.ui.inbox.IterableInboxAdapterExtension
import com.iterable.iterableapi.ui.inbox.IterableInboxFragment

fun MainFragment.onSortByTitleAscendingClicked() {
    DataManager.loadData("simple-inbox-messages.json")
    val intent = SingleFragmentActivity.createIntentWithFragment(
        activity!!,
        SortByTitleAscendingInboxFragment::class.java
    )
    startActivity(intent)
}

class SortByTitleAscendingInboxFragment : IterableInboxFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setComparator { message1, message2 ->
            message1.inboxMetadata!!.title!!.compareTo(message2.inboxMetadata!!.title!!)
        }
    }
    
}