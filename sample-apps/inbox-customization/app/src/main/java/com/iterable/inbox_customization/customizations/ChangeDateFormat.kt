package com.iterable.inbox_customization.customizations

import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import com.iterable.inbox_customization.MainFragment
import com.iterable.inbox_customization.R
import com.iterable.inbox_customization.util.DataManager
import com.iterable.inbox_customization.util.SingleFragmentActivity
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.ui.inbox.IterableInboxAdapter
import com.iterable.iterableapi.ui.inbox.IterableInboxAdapterExtension
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