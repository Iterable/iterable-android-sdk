package com.iterable.androidsdk.inboxCustomization.customizations

import android.os.Bundle
import android.text.format.DateUtils
import com.iterable.androidsdk.inboxCustomization.MainFragment
import com.iterable.androidsdk.inboxCustomization.util.DataManager
import com.iterable.androidsdk.inboxCustomization.util.SingleFragmentActivity
import com.iterable.iterableapi.ui.inbox.IterableInboxFragment
import java.util.*

fun MainFragment.onChangeDateFormatClicked() {
    DataManager.loadData("simple-inbox-messages.json")
    val intent = SingleFragmentActivity.createIntentWithFragment(
        activity!!,
        CustomDateFormatInboxFragment::class.java
    )
    startActivity(intent)
}

class CustomDateFormatInboxFragment : IterableInboxFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDateMapper { message ->
            DateUtils.getRelativeTimeSpanString(
                message.createdAt.time,
                Date().time,
                0,
                DateUtils.FORMAT_ABBREV_ALL
            )
        }
    }
}