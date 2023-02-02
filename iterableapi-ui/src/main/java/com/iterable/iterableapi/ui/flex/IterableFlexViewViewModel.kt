package com.iterable.iterableapi.ui.flex

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iterable.iterableapi.*

class IterableFlexViewViewModel : ViewModel() {

    // example flex message data from a payload
    private val flexMessageMetaData: IterableFlexMessageMetaData = IterableFlexMessageMetaData(
        "doibjo4590340oidiobnw",
        "mbn8489b7ehycy",
        "noj9iyjthfvhs",
        false
    )

    private val flexMessageButtons: List<IterableFlexMessageButton> = listOf(
        IterableFlexMessageButton("reward-button", "REDEEM MEOW", "success")
    )

    private val flexMessageImages: List<IterableFlexMessageImage> = listOf(
        IterableFlexMessageImage("coffee-image", "https://example-image-url.com/first-image")
    )

    private val flexMessageText: List<IterableFlexMessageText> = listOf(
        IterableFlexMessageText("body", "CATS RULE!!!")
    )

    private val flexMessageElements: IterableFlexMessageElements = IterableFlexMessageElements(
        "hero",
        flexMessageButtons,
        flexMessageImages,
        flexMessageText
    )

    private var _flexMessage = MutableLiveData<IterableFlexMessage>()
    val flexMessage: LiveData<IterableFlexMessage>
        get() = _flexMessage

    init {
        _flexMessage.value = IterableFlexMessage(flexMessageMetaData, flexMessageElements,  "")
    }
}