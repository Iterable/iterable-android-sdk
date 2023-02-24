package com.iterable.iterableapi

import org.json.JSONObject

public class IterableEmbeddedManager {
    fun getMessages(): List<IterableEmbeddedMessage> {
        // example of a JSON payload being serialized
        val flexMessageMetadata = EmbeddedMessageMetadata(
            "doibjo4590340oidiobnw",
            "mbn8489b7ehycy",
            "noj9iyjthfvhs",
            false
        )

        val flexMessageDefaultAction = EmbeddedMessageElementsDefaultAction(
            "someType", "someAction"
        )

        val flexMessageButtons: List<EmbeddedMessageElementsButton> = listOf(
            EmbeddedMessageElementsButton("reward-button", "REDEEM MEOW", "success")
        )

        val flexMessageText: List<EmbeddedMessageElementsText> = listOf(
            EmbeddedMessageElementsText("body", "CATS RULE!!!", "label")
        )

        val flexMessageElements = EmbeddedMessageElements(
            "Iterable Coffee Shoppe",
            "SAVE 15% OFF NOW",
            "http://placekitten.com/200/300",
            flexMessageDefaultAction,
            flexMessageButtons,
            flexMessageText
        )

        val payload = JSONObject()

        val flexMessageJson = JSONObject()
        flexMessageJson.put("metadata", flexMessageMetadata.toJSONObject())
        flexMessageJson.put("elements", flexMessageElements.toJSONObject())
        flexMessageJson.put("payload", payload)

        val embeddedMessages = listOf(
            IterableEmbeddedMessage.fromJSONObject(flexMessageJson),
            IterableEmbeddedMessage.fromJSONObject(flexMessageJson),
            IterableEmbeddedMessage.fromJSONObject(flexMessageJson)
        )

        return embeddedMessages
    }
}