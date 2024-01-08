package com.iterable.androidsdk.inboxCustomization.customizations

import android.content.Intent
import com.iterable.androidsdk.inboxCustomization.MainFragment
import com.iterable.androidsdk.inboxCustomization.util.DataManager
import com.iterable.iterableapi.testapp.R
import com.iterable.iterableapi.ui.inbox.IterableInboxActivity

fun MainFragment.onInboxWithCustomCellClicked() {
    DataManager.loadData("simple-inbox-messages.json")
    val intent = Intent(context, IterableInboxActivity::class.java)
    intent.putExtra("itemLayoutId", R.layout.dark_inbox_cell)
    startActivity(intent)
}