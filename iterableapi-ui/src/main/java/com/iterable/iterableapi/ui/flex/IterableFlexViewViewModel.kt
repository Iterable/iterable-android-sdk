package com.iterable.iterableapi.ui.flex

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableFlexManager
import com.iterable.iterableapi.IterableFlexMessage

class IterableFlexViewViewModel : ViewModel() {

    private var _flexMessages = MutableLiveData<List<IterableFlexMessage>>()
    val flexMessages: LiveData<List<IterableFlexMessage>>
        get() = _flexMessages

    init {
        val messages = IterableApi.getInstance().getFlexManager().getMessages()

        _flexMessages.value = messages
    }
}