package com.iterable.inbox_customization.tabs

import android.os.Bundle
import android.view.View
import com.iterable.inbox_customization.R
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.ui.inbox.IterableInboxAdapter
import com.iterable.iterableapi.ui.inbox.IterableInboxAdapterExtension
import com.iterable.iterableapi.ui.inbox.IterableInboxFragment

class CustomInbox : IterableInboxFragment(), IterableInboxAdapterExtension<Nothing> {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAdapterExtension(this)
    }

    override fun getItemViewType(message: IterableInAppMessage): Int {
        return 0
    }

    override fun getLayoutForViewType(viewType: Int): Int {
        return R.layout.dark_inbox_cell
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