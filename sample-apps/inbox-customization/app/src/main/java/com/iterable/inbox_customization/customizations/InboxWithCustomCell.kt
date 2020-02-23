package com.iterable.inbox_customization.customizations

import android.content.Intent
import com.iterable.inbox_customization.MainFragment
import com.iterable.inbox_customization.R
import com.iterable.inbox_customization.util.DataManager
import com.iterable.iterableapi.ui.inbox.IterableInboxActivity

fun MainFragment.onInboxWithCustomCellClicked() {
    DataManager.loadData("simple-inbox-messages.json")
    val intent = Intent(context, IterableInboxActivity::class.java)
    intent.putExtra("itemLayoutId", R.layout.dark_inbox_cell)
    startActivity(intent)
}