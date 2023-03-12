package com.iterable.iterableapi

import org.json.JSONObject
import java.io.File

public class IterableEmbeddedManager {
    fun getMessages(): List<IterableEmbeddedMessage> {
        val embeddedMessageMetadata = EmbeddedMessageMetadata(
            "doibjo4590340oidiobnw",
            "mbn8489b7ehycy",
            "noj9iyjthfvhs",
            false
        )

        val embeddedMessageDefaultAction = EmbeddedMessageElementsDefaultAction(
            "someType", "someAction"
        )

        val embeddedMessageButtons: List<EmbeddedMessageElementsButton> = listOf(
            EmbeddedMessageElementsButton("reward-button", "REDEEM MEOW", "success")
        )

        val embeddedMessageText: List<EmbeddedMessageElementsText> = listOf(
            EmbeddedMessageElementsText("body", "CATS RULE!!!", "label")
        )

        val embeddedMessageElements = EmbeddedMessageElements(
            "Iterable Coffee Shoppe",
            "SAVE 15% OFF NOW",
            "http://placekitten.com/200/300",
            embeddedMessageDefaultAction,
            embeddedMessageButtons,
            embeddedMessageText
        )

        val payload = JSONObject()

        val embeddedMessageJson = JSONObject()
        embeddedMessageJson.put("metadata", embeddedMessageMetadata.toJSONObject())
        embeddedMessageJson.put("elements", embeddedMessageElements.toJSONObject())
        embeddedMessageJson.put("payload", payload)

        val embeddedMessages = listOf(
            IterableEmbeddedMessage.fromJSONObject(embeddedMessageJson),
            IterableEmbeddedMessage.fromJSONObject(embeddedMessageJson),
            IterableEmbeddedMessage.fromJSONObject(embeddedMessageJson)
        )

        return embeddedMessages
    }
}