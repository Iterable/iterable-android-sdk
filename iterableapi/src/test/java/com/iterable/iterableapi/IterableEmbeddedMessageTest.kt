package com.iterable.iterableapi

import org.hamcrest.Matchers.`is`
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class IterableEmbeddedMessageTest {
    @Test
    fun embeddedMessageDeserialization_elementsAndCustomPayloadDefined() {
        val payload = JSONObject(IterableTestUtils.getResourceString("embedded_payload_optional_elements_and_custom_payload.json"))
        val jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENTS)

        if (jsonArray != null) {
            // GIVEN an embedded message placement payload
            val placementJson = jsonArray.optJSONObject(0)

            // WHEN you deserialize the embedded message placement payload
            val placement = IterableEmbeddedPlacement.fromJSONObject(placementJson)

            val message = placement.messages[0]

            val payload = JSONObject()
            payload.put("someKey", "someValue")

            // THEN we get appropriate embedded message object and associated placement id
            assertNotNull(placement)
            assertThat("0", `is` (placement.placementId))

            assertThat("doibjo4590340oidiobnw", `is` (message.metadata.messageId))
            assertThat("mbn8489b7ehycy", `is` (message.metadata.placementId))
            assertThat("noj9iyjthfvhs",`is` (message.metadata.campaignId))
            assertThat(true, `is` (message.metadata.isProof))

            assertThat("Iterable Coffee Shoppe", `is`(message.elements?.title))
            assertThat("SAVE 15% OFF NOW", `is` (message.elements?.body))
            assertThat("http://placekitten.com/200/300", `is` (message.elements?.mediaURL))

            assertThat("someType", `is`(message.elements?.defaultAction?.type))
            assertThat("someData", `is` (message.elements?.defaultAction?.data))

            assertThat("reward-button", `is`(message.elements?.buttons?.get(0)?.id))
            assertThat("REDEEM MEOW", `is` (message.elements?.buttons?.get(0)?.title))
            assertThat("openUrl", `is` (message.elements?.buttons?.get(0)?.action?.type))
            assertThat("https://www.google.com", `is` (message.elements?.buttons?.get(0)?.action?.data))

            assertThat("body", `is`(message.elements?.text?.get(0)?.id))
            assertThat("CATS RULE!!!", `is` (message.elements?.text?.get(0)?.text))
            assertThat("label", `is` (message.elements?.text?.get(0)?.label))

            JSONAssert.assertEquals(payload, message.payload, JSONCompareMode.STRICT_ORDER)
        }
    }

    @Test
    fun embeddedMessageDeserialization_noCustomPayloadDefined() {
        val payload = JSONObject(IterableTestUtils.getResourceString("embedded_payload_no_custom_payload.json"))
        val jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENTS)

        if (jsonArray != null) {
            // GIVEN an embedded message placement payload
            val placementJson = jsonArray.optJSONObject(0)

            // WHEN you deserialize the embedded message placement payload
            val placement = IterableEmbeddedPlacement.fromJSONObject(placementJson)

            val message = placement.messages[0]

            // THEN we get appropriate embedded message object and associated placement id
            assertNotNull(placement)
            assertThat("0", `is` (placement.placementId))

            assertThat("doibjo4590340oidiobnw", `is` (message.metadata.messageId))
            assertThat("mbn8489b7ehycy", `is` (message.metadata.placementId))
            assertThat("noj9iyjthfvhs",`is` (message.metadata.campaignId))
            assertThat(true, `is` (message.metadata.isProof))

            assertThat("Iterable Coffee Shoppe", `is`(message.elements?.title))
            assertThat("SAVE 15% OFF NOW", `is` (message.elements?.body))
            assertThat("http://placekitten.com/200/300", `is` (message.elements?.mediaURL))

            assertThat("someType", `is`(message.elements?.defaultAction?.type))
            assertThat("someData", `is` (message.elements?.defaultAction?.data))

            assertThat("reward-button", `is`(message.elements?.buttons?.get(0)?.id))
            assertThat("REDEEM MEOW", `is` (message.elements?.buttons?.get(0)?.title))

            assertThat("someType", `is`(message.elements?.defaultAction?.type))
            assertThat("someData", `is` (message.elements?.defaultAction?.data))

            assertThat("body", `is`(message.elements?.text?.get(0)?.id))
            assertThat("CATS RULE!!!", `is` (message.elements?.text?.get(0)?.text))
            assertThat("label", `is` (message.elements?.text?.get(0)?.label))

            assertNull(message.payload)
        }
    }

    @Test
    fun embeddedMessageDeserialization_noButtonsOrText() {
        val payload = JSONObject(IterableTestUtils.getResourceString("embedded_payload_no_buttons_no_text.json"))
        val jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENTS)

        if (jsonArray != null) {
            // GIVEN an embedded message placement payload
            val placementJson = jsonArray.optJSONObject(0)

            // WHEN you deserialize the embedded message placement payload
            val placement = IterableEmbeddedPlacement.fromJSONObject(placementJson)

            val message = placement.messages[0]

            val payload = JSONObject()
            payload.put("someKey", "someValue")

            // THEN we get appropriate embedded message object and associated placement id
            assertNotNull(placement)
            assertThat("0", `is` (placement.placementId))

            assertThat("doibjo4590340oidiobnw", `is` (message.metadata.messageId))
            assertThat("mbn8489b7ehycy", `is` (message.metadata.placementId))
            assertThat("noj9iyjthfvhs",`is` (message.metadata.campaignId))
            assertThat(true, `is` (message.metadata.isProof))

            assertThat("Iterable Coffee Shoppe", `is`(message.elements?.title))
            assertThat("SAVE 15% OFF NOW", `is` (message.elements?.body))
            assertThat("http://placekitten.com/200/300", `is` (message.elements?.mediaURL))

            assertThat("someType", `is`(message.elements?.defaultAction?.type))
            assertThat("someData", `is` (message.elements?.defaultAction?.data))

            assertNull(message.elements?.buttons)
            assertNull(message.elements?.text)

            JSONAssert.assertEquals(payload, message.payload, JSONCompareMode.STRICT_ORDER)
        }
    }

    @Test
    fun embeddedMessageDeserialization_noElementsOrCustomPayloadDefined() {
        val payload = JSONObject(IterableTestUtils.getResourceString("embedded_payload_no_elements_no_custom_payload.json"))
        val jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENTS)

        if (jsonArray != null) {
            // GIVEN an embedded message placement payload with optional elements
            val placementJson = jsonArray.optJSONObject(0)

            // WHEN you deserialize the embedded message placement payload
            val placement = IterableEmbeddedPlacement.fromJSONObject(placementJson)

            val message = placement.messages[0]

            // THEN we get appropriate embedded message object and associated placement id
            assertNotNull(placement)
            assertThat("0", `is` (placement.placementId))

            assertThat("doibjo4590340oidiobnw", `is` (message.metadata.messageId))
            assertThat("mbn8489b7ehycy", `is` (message.metadata.placementId))
            assertThat("noj9iyjthfvhs",`is` (message.metadata.campaignId))
            assertThat(true, `is` (message.metadata.isProof))

            assertNull(message.elements)
            assertNull(message.payload)
        }
    }

    @Test
    fun embeddedMessageSerialization_elementsAndCustomPayloadDefined() {
        val embeddedMessageMetadata = EmbeddedMessageMetadata(
            "doibjo4590340oidiobnw",
            "mbn8489b7ehycy",
            "noj9iyjthfvhs",
            true
        )

        val embeddedMessageDefaultAction = EmbeddedMessageElementsDefaultAction(
            "someType", "someData"
        )

        val embeddedMessageElementsButtonAction = EmbeddedMessageElementsButtonAction(
            "openUrl", "https://www.google.com"
        )

        val embeddedMessageButtons: List<EmbeddedMessageElementsButton> = listOf(
            EmbeddedMessageElementsButton("reward-button", "REDEEM MEOW", embeddedMessageElementsButtonAction)
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

        val customPayload = JSONObject()
        customPayload.put("someKey", "someValue")

        val embeddedMessage = IterableEmbeddedMessage(embeddedMessageMetadata, embeddedMessageElements, customPayload)

        val placementId: String = "0"
        val messages: List<IterableEmbeddedMessage> = listOf(embeddedMessage)

        val embeddedMessagePlacement = IterableEmbeddedPlacement(placementId, messages)

        val payload = JSONObject(IterableTestUtils.getResourceString("embedded_payload_optional_elements_and_custom_payload.json"))
        val jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENTS)

        if (jsonArray != null) {
            // GIVEN an embedded message placement payload
            val expectedPlacementJson = jsonArray.optJSONObject(0)

            // WHEN you serialize the embedded message payload
            val placementJson = IterableEmbeddedPlacement.toJSONObject(embeddedMessagePlacement)

            // THEN we get appropriate embedded message object
            JSONAssert.assertEquals(expectedPlacementJson, placementJson, JSONCompareMode.STRICT_ORDER)
        }
    }

    @Test
    fun embeddedMessageSerialization_noButtons_noText() {
        val embeddedMessageMetadata = EmbeddedMessageMetadata(
            "doibjo4590340oidiobnw",
            "mbn8489b7ehycy",
            "noj9iyjthfvhs",
            true
        )

        val embeddedMessageDefaultAction = EmbeddedMessageElementsDefaultAction(
            "someType", "someData"
        )

        val embeddedMessageElements = EmbeddedMessageElements(
            "Iterable Coffee Shoppe",
            "SAVE 15% OFF NOW",
            "http://placekitten.com/200/300",
            embeddedMessageDefaultAction
        )

        val customPayload = JSONObject()
        customPayload.put("someKey", "someValue")

        val embeddedMessage = IterableEmbeddedMessage(embeddedMessageMetadata, embeddedMessageElements, customPayload)

        val placementId: String = "0"
        val messages: List<IterableEmbeddedMessage> = listOf(embeddedMessage)

        val embeddedMessagePlacement = IterableEmbeddedPlacement(placementId, messages)

        val payload = JSONObject(IterableTestUtils.getResourceString("embedded_payload_no_buttons_no_text.json"))
        val jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE)

        if (jsonArray != null) {
            // GIVEN an embedded message placement payload
            val expectedPlacementJson = jsonArray.optJSONObject(0)

            // WHEN you serialize the embedded message payload
            val placementJson = IterableEmbeddedPlacement.toJSONObject(embeddedMessagePlacement)

            // THEN we get appropriate embedded message object
            JSONAssert.assertEquals(expectedPlacementJson, placementJson, JSONCompareMode.STRICT_ORDER)
        }
    }
}