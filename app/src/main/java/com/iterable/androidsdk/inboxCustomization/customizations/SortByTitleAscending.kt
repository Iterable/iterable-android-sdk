package com.iterable.androidsdk.inboxCustomization.customizations

import android.os.Bundle
import com.iterable.androidsdk.inboxCustomization.MainFragment
import com.iterable.androidsdk.inboxCustomization.util.DataManager
import com.iterable.androidsdk.inboxCustomization.util.SingleFragmentActivity
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