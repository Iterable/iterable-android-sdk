package com.iterable.iterableapi.ui.flex

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iterable.iterableapi.*
import org.json.JSONObject

class IterableFlexViewViewModel : ViewModel() {

    // example flex message data from a payload
//    private val flexMessageMetaData: FlexMessageMetadata = FlexMessageMetadata(
//        "doibjo4590340oidiobnw",
//        "mbn8489b7ehycy",
//        "noj9iyjthfvhs",
//        false
//    )
//
//    private val flexMessageDefaultAction: FlexMessageElementsDefaultAction = FlexMessageElementsDefaultAction(
//        "someType",
//        "someData"
//    )
//
//    private val flexMessageButtons: List<FlexMessageElementsButton> = listOf(
//        FlexMessageElementsButton("reward-button", "REDEEM MEOW", "success")
//    )
//
//    private val flexMessageText: List<FlexMessageElementsText> = listOf(
//        FlexMessageElementsText("body", "CATS RULE!!!")
//    )
//
//    private val flexMessageElements: FlexMessageElements = FlexMessageElements(
//        "Iterable Coffee Shoppe",
//        "Get 15% off now through July 4th on any Mocha beverage",
//        "http://placekitten.com/200/300",
//        flexMessageDefaultAction,
//        flexMessageButtons,
//        flexMessageText
//    )

    private var _flexMessage = MutableLiveData<IterableFlexMessage>()
    val flexMessage: LiveData<IterableFlexMessage>
        get() = _flexMessage

    init {
        val flexMessageMetaData = FlexMessageMetadata(
            "doibjo4590340oidiobnw",
            "mbn8489b7ehycy",
            "noj9iyjthfvhs",
            false
        )

        val flexMessageDefaultAction = FlexMessageElementsDefaultAction(
            "someType",
            "someData"
        )

        val flexMessageButtons: List<FlexMessageElementsButton> = listOf(
            FlexMessageElementsButton("reward-button", "REDEEM MEOW", "success")
        )

        val flexMessageText: List<FlexMessageElementsText> = listOf(
            FlexMessageElementsText("body", "CATS RULE!!!")
        )

        val flexMessageElements = FlexMessageElements(
            "someTitle",
            "someBody",
            "someURL",
            flexMessageDefaultAction,
            flexMessageButtons,
            flexMessageText
        )

        val payload = JSONObject()
        payload.put("a", "1")
        payload.put("someAction", "doSomething")

        var flexMessage = IterableFlexMessage(flexMessageMetaData, flexMessageElements, payload)

        val flexMessageJson: JSONObject = flexMessage.toJSONObject()
        flexMessage = IterableFlexMessage.fromJSONObject(flexMessageJson)

        Log.i(IterableFlexMessage.TAG, "id - ${flexMessage.metadata.id}")
        Log.i(IterableFlexMessage.TAG, "placementId - ${flexMessage.metadata.placementId}")
        Log.i(IterableFlexMessage.TAG, "campaignId - ${flexMessage.metadata.campaignId}")
        Log.i(IterableFlexMessage.TAG, "isProof - ${flexMessage.metadata.isProof}")
        Log.i(IterableFlexMessage.TAG, "title - ${flexMessage.elements?.title}")
        Log.i(IterableFlexMessage.TAG, "body - ${flexMessage.elements?.body}")
        Log.i(IterableFlexMessage.TAG, "media url - ${flexMessage.elements?.mediaURL}")
        Log.i(IterableFlexMessage.TAG, "default action type - ${flexMessage.elements?.defaultAction?.type}")
        Log.i(IterableFlexMessage.TAG, "button title - ${flexMessage.elements?.buttons?.get(0)?.title}")
        Log.i(IterableFlexMessage.TAG, "text text - ${flexMessage.elements?.text?.get(0)?.text}")

//        _flexMessage.value = IterableFlexMessage(flexMessageMetaData, flexMessageElements, hashMapOf(1 to "a"))
    }
}