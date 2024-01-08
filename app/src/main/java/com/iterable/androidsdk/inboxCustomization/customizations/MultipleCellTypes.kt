package com.iterable.androidsdk.inboxCustomization.customizations

import android.os.Bundle
import android.view.View
import com.iterable.androidsdk.inboxCustomization.MainFragment
import com.iterable.androidsdk.inboxCustomization.util.DataManager
import com.iterable.androidsdk.inboxCustomization.util.SingleFragmentActivity
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.testapp.R
import com.iterable.iterableapi.ui.inbox.IterableInboxAdapter
import com.iterable.iterableapi.ui.inbox.IterableInboxAdapterExtension
import com.iterable.iterableapi.ui.inbox.IterableInboxFragment

fun MainFragment.onInboxWithMultipleCellTypesClicked() {
    DataManager.loadData("inbox-with-multiple-cell-types-messages.json")
    val intent = SingleFragmentActivity.createIntentWithFragment(
        activity!!, MultipleCellTypesInboxFragment::class.java
    )
    startActivity(intent)
}

class MultipleCellTypesInboxFragment : IterableInboxFragment(),
    IterableInboxAdapterExtension<Nothing> {

    val ITEM_TYPE_DEFAULT = 0
    val ITEM_TYPE_LIGHT = 1
    val ITEM_TYPE_DARK = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAdapterExtension(this)
    }

    override fun getItemViewType(message: IterableInAppMessage): Int {
        return when (message.customPayload.optString("messageType")) {
            "light" -> ITEM_TYPE_LIGHT
            "dark" -> ITEM_TYPE_DARK
            else -> ITEM_TYPE_DEFAULT
        }
    }

    override fun getLayoutForViewType(viewType: Int): Int {
        return when (viewType) {
            ITEM_TYPE_LIGHT -> R.layout.light_inbox_cell
            ITEM_TYPE_DARK -> R.layout.dark_inbox_cell
            else -> R.layout.iterable_inbox_item
        }
    }

    override fun createViewHolderExtension(view: View, viewType: Int): Nothing? {
        return null
    }

    override fun onBindViewHolder(
        viewHolder: IterableInboxAdapter.ViewHolder,
        holderExtension: Nothing?,
        message: IterableInAppMessage
    ) {
    }
}