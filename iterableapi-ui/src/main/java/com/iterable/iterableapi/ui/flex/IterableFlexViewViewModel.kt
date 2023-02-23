package com.iterable.iterableapi.ui.flex

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iterable.iterableapi.*
import org.json.JSONObject

class IterableFlexViewViewModel : ViewModel() {

    // example flex message data from a payload
    private val flexMessageMetaData = FlexMessageMetadata(
        "doibjo4590340oidiobnw",
        "mbn8489b7ehycy",
        "noj9iyjthfvhs",
        false
    )

    private val flexMessageDefaultAction = FlexMessageElementsDefaultAction(
        "someType", "someAction"
    )

    private val flexMessageButtons: List<FlexMessageElementsButton> = listOf(
        FlexMessageElementsButton("reward-button", "REDEEM MEOW", "success")
    )


    private val flexMessageText: List<FlexMessageElementsText> = listOf(
        FlexMessageElementsText("body", "CATS RULE!!!", "label")
    )

    private val flexMessageElements = FlexMessageElements(
        "Iterable Coffee Shoppe",
        "Get 15% OFF",
        "http://placekitten.com/200/300",
        flexMessageDefaultAction,
        flexMessageButtons,
        flexMessageText
    )

    val payload = JSONObject()

    private var _flexMessage = MutableLiveData<IterableFlexMessage>()
    val flexMessage: LiveData<IterableFlexMessage>
        get() = _flexMessage

    init {
        _flexMessage.value = IterableFlexMessage(flexMessageMetaData, flexMessageElements, payload)
    }
}