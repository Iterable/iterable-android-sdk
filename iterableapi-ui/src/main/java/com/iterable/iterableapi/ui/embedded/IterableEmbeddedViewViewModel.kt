package com.iterable.iterableapi.ui.flex

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iterable.iterableapi.*
import org.json.JSONObject

class IterableEmbeddedViewViewModel: ViewModel() {
    // example flex message data from a payload
    private val embeddedMessageMetaData = EmbeddedMessageMetadata(
        "doibjo4590340oidiobnw",
        "mbn8489b7ehycy",
        "noj9iyjthfvhs",
        false
    )

    private val embeddedMessageDefaultAction = EmbeddedMessageElementsDefaultAction(
        "someType", "someAction"
    )

    private val embeddedMessageButtons: List<EmbeddedMessageElementsButton> = listOf(
        EmbeddedMessageElementsButton("reward-button", "REDEEM MEOW", "success")
    )


    private val embeddedMessageText: List<EmbeddedMessageElementsText> = listOf(
        EmbeddedMessageElementsText("body", "CATS RULE!!!", "label")
    )

    private val embeddedMessageElements = EmbeddedMessageElements(
        "Iterable Coffee Shoppe",
        "Get 15% OFF",
        "http://placekitten.com/200/300",
        embeddedMessageDefaultAction,
        embeddedMessageButtons,
        embeddedMessageText
    )

    val payload = JSONObject()

    private var _embeddedMessages = MutableLiveData<List<IterableEmbeddedMessage>>()
    val embeddedMessages: LiveData<List<IterableEmbeddedMessage>>
        get() = _embeddedMessages

    init {
        _embeddedMessages.value = listOf(
            IterableEmbeddedMessage(embeddedMessageMetaData, embeddedMessageElements, payload),
            IterableEmbeddedMessage(embeddedMessageMetaData, embeddedMessageElements, payload),
            IterableEmbeddedMessage(embeddedMessageMetaData, embeddedMessageElements, payload)
        )
    }
}