package com.iterable.iterableapi.ui.flex

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iterable.iterableapi.*

class IterableFlexViewViewModel : ViewModel() {

    // example flex message data from a payload
    private val flexMessageMetaData: FlexMessageMetadata = FlexMessageMetadata(
        "doibjo4590340oidiobnw",
        "mbn8489b7ehycy",
        "noj9iyjthfvhs",
        false
    )

    private val flexMessageDefaultAction: FlexMessageElementsDefaultAction = FlexMessageElementsDefaultAction(
        "someType",
        "someData"
    )

    private val flexMessageButtons: List<FlexMessageElementsButton> = listOf(
        FlexMessageElementsButton("reward-button", "REDEEM MEOW", "success")
    )

    private val flexMessageText: List<FlexMessageElementsText> = listOf(
        FlexMessageElementsText("body", "CATS RULE!!!")
    )

    private val flexMessageElements: FlexMessageElements = FlexMessageElements(
        "Iterable Coffee Shoppe",
        "Get 15% off now through July 4th on any Mocha beverage",
        "http://placekitten.com/200/300",
        flexMessageDefaultAction,
        flexMessageButtons,
        flexMessageText
    )

    private var _flexMessage = MutableLiveData<IterableFlexMessage>()
    val flexMessage: LiveData<IterableFlexMessage>
        get() = _flexMessage

    init {
        _flexMessage.value = IterableFlexMessage(flexMessageMetaData, flexMessageElements, hashMapOf(1 to "a"))
    }
}