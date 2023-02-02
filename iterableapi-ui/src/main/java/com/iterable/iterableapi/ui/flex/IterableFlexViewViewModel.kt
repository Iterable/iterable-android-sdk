package com.iterable.iterableapi.ui.flex

import android.util.Log
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

    var flexMessage: IterableFlexMessage = IterableFlexMessage(flexMessageMetaData, flexMessageElements,  "")

    init {
        Log.i("ItblFlexViewViewModel", "IterableFlexViewViewModel created!")
    }

    override fun onCleared() {
        super.onCleared()
        Log.i("ItblFlexViewViewModel", "IterableFlexViewViewModel destroyed!")
    }
}