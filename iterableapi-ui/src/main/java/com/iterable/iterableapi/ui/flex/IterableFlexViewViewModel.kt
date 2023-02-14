package com.iterable.iterableapi.ui.flex

import android.view.View
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

    private val flexMessageText1: List<IterableFlexMessageText> = listOf(
        IterableFlexMessageText("body", "CATS RULE!!!")
    )

    private val flexMessageText2: List<IterableFlexMessageText> = listOf(
        IterableFlexMessageText("body", "DOGS DROOL!!!")
    )

    private val flexMessageElements1: IterableFlexMessageElements = IterableFlexMessageElements(
        "hero",
        flexMessageButtons,
        flexMessageImages,
        flexMessageText1
    )

    private val flexMessageElements2: IterableFlexMessageElements = IterableFlexMessageElements(
        "hero",
        flexMessageButtons,
        flexMessageImages,
        flexMessageText2
    )

    private var _flexMessages = MutableLiveData<List<IterableFlexMessage>>()
    val flexMessages: LiveData<List<IterableFlexMessage>>
        get() = _flexMessages

    init {
        _flexMessages.value = listOf(
            IterableFlexMessage(flexMessageMetaData, flexMessageElements2, ""),
            IterableFlexMessage(flexMessageMetaData, flexMessageElements2, ""),
            IterableFlexMessage(flexMessageMetaData, flexMessageElements2, ""),
            IterableFlexMessage(flexMessageMetaData, flexMessageElements2, ""),
            IterableFlexMessage(flexMessageMetaData, flexMessageElements2, ""),
            IterableFlexMessage(flexMessageMetaData, flexMessageElements2, ""),
            IterableFlexMessage(flexMessageMetaData, flexMessageElements2, ""),
            IterableFlexMessage(flexMessageMetaData, flexMessageElements2, "")
        )
    }
}