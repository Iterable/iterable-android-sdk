package com.iterable.iterableapi

import org.json.JSONObject

public class IterableFlexManager {

    fun getMessages(): List<IterableFlexMessage> {
        // example of a JSON payload being serialized
        val flexMessageMetadata = FlexMessageMetadata(
            "doibjo4590340oidiobnw",
            "mbn8489b7ehycy",
            "noj9iyjthfvhs",
            false
        )

        val flexMessageDefaultAction = FlexMessageElementsDefaultAction(
            "someType", "someAction"
        )

        val flexMessageButtons: List<FlexMessageElementsButton> = listOf(
            FlexMessageElementsButton("reward-button", "REDEEM MEOW", "success")
        )

        val flexMessageText: List<FlexMessageElementsText> = listOf(
            FlexMessageElementsText("body", "CATS RULE!!!", "label")
        )

        val flexMessageElements = FlexMessageElements(
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

        val flexMessages = listOf(
            IterableFlexMessage.fromJSONObject(flexMessageJson),
            IterableFlexMessage.fromJSONObject(flexMessageJson),
            IterableFlexMessage.fromJSONObject(flexMessageJson)
        )

        return flexMessages
    }
}