package com.iterable.inbox_customization.customizations

import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import com.iterable.inbox_customization.MainFragment
import com.iterable.inbox_customization.R
import com.iterable.inbox_customization.util.DataManager
import com.iterable.inbox_customization.util.SingleFragmentActivity
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.ui.inbox.IterableInboxAdapter
import com.iterable.iterableapi.ui.inbox.IterableInboxAdapterExtension
import com.iterable.iterableapi.ui.inbox.IterableInboxFragment
import java.util.*

fun MainFragment.onInboxWithAdditionalFieldsClicked() {
    DataManager.loadData("inbox-with-additional-fields-messages.json")
    val intent = SingleFragmentActivity.createIntentWithFragment(
        activity!!,
        InboxWithAdditionalFieldsFragment::class.java
    )
    startActivity(intent)
}

class InboxWithAdditionalFieldsFragment : IterableInboxFragment(),
    IterableInboxAdapterExtension<InboxWithAdditionalFieldsFragment.ViewHolder> {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAdapterExtension(this)
    }

    override fun createViewHolderExtension(view: View, viewType: Int): ViewHolder? {
        return ViewHolder(view)
    }

    override fun getItemViewType(message: IterableInAppMessage): Int {
        return 0
    }

    override fun getLayoutForViewType(viewType: Int): Int {
        return R.layout.additional_fields_cell
    }

    override fun onBindViewHolder(
        viewHolder: IterableInboxAdapter.ViewHolder,
        holderExtension: ViewHolder?,
        message: IterableInAppMessage
    ) {
        holderExtension?.discountText?.text = message.customPayload.optString("discount")
    }

    class ViewHolder(view: View) {
        var discountText: TextView? = null

        init {
            this.discountText = view.findViewById(R.id.discountText)
        }
    }
}