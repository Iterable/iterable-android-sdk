package com.iterable.iterableapi

import org.hamcrest.Matchers.`is`
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class IterableEmbeddedPlacementTest {
    @Test
    fun embeddedPlacementDeserialization_elementsAndCustomPayloadDefined() {
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
            assertThat(411L, `is`(placement.placementId))

            assertThat("doibjo4590340oidiobnw", `is` (message.metadata.messageId))
            assertThat(411L, `is` (message.metadata.placementId))
            assertThat(2324,`is` (message.metadata.campaignId))
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
    fun embeddedPlacementDeserialization_noCustomPayloadDefined() {
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
            assertThat(411L, `is` (placement.placementId))

            assertThat("doibjo4590340oidiobnw", `is` (message.metadata.messageId))
            assertThat(411L, `is` (message.metadata.placementId))
            assertThat(2324,`is` (message.metadata.campaignId))
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
    fun embeddedPlacementDeserialization_noButtonsOrText() {
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
            assertThat(411L, `is` (placement.placementId))

            assertThat("doibjo4590340oidiobnw", `is` (message.metadata.messageId))
            assertThat(411L, `is` (message.metadata.placementId))
            assertThat(2324,`is` (message.metadata.campaignId))
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
    fun embeddedPlacementDeserialization_noElementsOrCustomPayloadDefined() {
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
            assertThat(411L, `is` (placement.placementId))

            assertThat("doibjo4590340oidiobnw", `is` (message.metadata.messageId))
            assertThat(411L, `is` (message.metadata.placementId))
            assertThat(2324,`is` (message.metadata.campaignId))
            assertThat(true, `is` (message.metadata.isProof))

            assertNull(message.elements)
            assertNull(message.payload)
        }
    }

    @Test
    fun embeddedPlacementDeserialization_multiplePlacements() {
        val payload = JSONObject(IterableTestUtils.getResourceString("embedded_payload_multiple_1.json"))
        val jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENTS)

        if (jsonArray != null) {
            // GIVEN an embedded message placement payload with optional elements
            val placementJson1 = jsonArray.optJSONObject(0)
            val placementJson2 = jsonArray.optJSONObject(1)

            // WHEN you deserialize the embedded message placement payload
            val placement1 = IterableEmbeddedPlacement.fromJSONObject(placementJson1)
            val placement2 = IterableEmbeddedPlacement.fromJSONObject(placementJson2)

            val message1 = placement1.messages[0]
            val message2 = placement2.messages[0]

            // THEN we get appropriate embedded message object and associated placement id
            assertNotNull(placement1)
            assertThat(0L, `is` (placement1.placementId))

            assertThat("doibjo4590340oidiobnw", `is` (message1.metadata.messageId))
            assertThat(0L, `is` (message1.metadata.placementId))
            assertThat(2324,`is` (message1.metadata.campaignId))
            assertThat(true, `is` (message1.metadata.isProof))

            assertThat("Iterable Coffee Shoppe", `is`(message1.elements?.title))
            assertThat("SAVE 15% OFF NOW", `is` (message1.elements?.body))
            assertThat("http://placekitten.com/200/300", `is` (message1.elements?.mediaURL))

            assertThat("someType", `is`(message1.elements?.defaultAction?.type))
            assertThat("someData", `is` (message1.elements?.defaultAction?.data))

            assertThat("reward-button", `is`(message1.elements?.buttons?.get(0)?.id))
            assertThat("REDEEM MEOW", `is` (message1.elements?.buttons?.get(0)?.title))

            assertThat("body", `is`(message1.elements?.text?.get(0)?.id))
            assertThat("CATS RULE!!!", `is` (message1.elements?.text?.get(0)?.text))
            assertThat("label", `is` (message1.elements?.text?.get(0)?.label))

            assertThat("someValue", `is` (message1.payload?.getString("someKey")))

            assertNotNull(placement1)
            assertThat(1L, `is` (placement2.placementId))

            assertThat("faert442rjasiri99", `is` (message2.metadata.messageId))
            assertThat(1L, `is` (message2.metadata.placementId))
            assertThat(132,`is` (message2.metadata.campaignId))
            assertThat(true, `is` (message2.metadata.isProof))

            assertThat("DEALS DEALS DEALS", `is`(message2.elements?.title))
            assertThat("ACT NOW! FIRST ONE FREE", `is` (message2.elements?.body))
            assertThat("http://placekitten.com/200/300", `is` (message2.elements?.mediaURL))

            assertThat("someType", `is`(message2.elements?.defaultAction?.type))
            assertThat("someData", `is` (message2.elements?.defaultAction?.data))

            assertThat("reward-button", `is`(message2.elements?.buttons?.get(0)?.id))
            assertThat("REDEEM MEOW", `is` (message2.elements?.buttons?.get(0)?.title))

            assertThat("body", `is`(message2.elements?.text?.get(0)?.id))
            assertThat("CATS RULE!!!", `is` (message2.elements?.text?.get(0)?.text))
            assertThat("label", `is` (message2.elements?.text?.get(0)?.label))

            assertThat("someValue", `is` (message2.payload?.getString("someKey")))
        }
    }

    @Test
    fun embeddedPlacementSerialization_elementsAndCustomPayloadDefined() {
        val embeddedMessageMetadata = EmbeddedMessageMetadata(
            "doibjo4590340oidiobnw",
            411L,
            2324,
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

        val placementId: Long = 411L
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
    fun embeddedPlacementSerialization_noButtons_noText() {
        val embeddedMessageMetadata = EmbeddedMessageMetadata(
            "doibjo4590340oidiobnw",
            411L,
            2324,
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

        val placementId: Long = 411L
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

    @Test
    fun embeddedPlacementSerialization_multiplePlacements() {
        val embeddedMessageMetadata1 = EmbeddedMessageMetadata(
            "ewsd3fdrtj6ty",
            1L,
            19853,
            true
        )

        val embeddedMessageDefaultAction1 = EmbeddedMessageElementsDefaultAction(
            "someType", "someData"
        )

        val embeddedMessageElementsButtonAction1 = EmbeddedMessageElementsButtonAction(
            "openUrl", "https://www.google.com"
        )

        val embeddedMessageButtons1: List<EmbeddedMessageElementsButton> = listOf(
            EmbeddedMessageElementsButton("reward-button", "REDEEM MEOW", embeddedMessageElementsButtonAction1)
        )

        val embeddedMessageText1: List<EmbeddedMessageElementsText> = listOf(
            EmbeddedMessageElementsText("body", "CATS RULE!!!", "label")
        )

        val embeddedMessageElements1 = EmbeddedMessageElements(
            "Subscribe to the newsletter",
            "Learn all about colorado kittens",
            "http://placekitten.com/200/300",
            embeddedMessageDefaultAction1,
            embeddedMessageButtons1,
            embeddedMessageText1
        )

        val customPayload1 = JSONObject()
        customPayload1.put("someKey", "someValue")

        val embeddedMessage1 = IterableEmbeddedMessage(embeddedMessageMetadata1, embeddedMessageElements1, customPayload1)

        val placementId1: Long = 1
        val messages1: List<IterableEmbeddedMessage> = listOf(embeddedMessage1)

        val embeddedMessagePlacement1 = IterableEmbeddedPlacement(placementId1, messages1)

        val embeddedMessageMetadata2 = EmbeddedMessageMetadata(
            "grewdvb54ut87y",
            2,
            1910,
            true
        )

        val embeddedMessageDefaultAction2 = EmbeddedMessageElementsDefaultAction(
            "someType", "someData"
        )

        val embeddedMessageElementsButtonAction2 = EmbeddedMessageElementsButtonAction(
            "openUrl", "https://www.google.com"
        )

        val embeddedMessageButtons2: List<EmbeddedMessageElementsButton> = listOf(
            EmbeddedMessageElementsButton("reward-button", "REDEEM MEOW", embeddedMessageElementsButtonAction2)
        )

        val embeddedMessageText2: List<EmbeddedMessageElementsText> = listOf(
            EmbeddedMessageElementsText("body", "CATS RULE!!!", "label")
        )

        val embeddedMessageElements2 = EmbeddedMessageElements(
            "Experience the great outdoors",
            "Trips are going fast!",
            "http://placekitten.com/200/300",
            embeddedMessageDefaultAction2,
            embeddedMessageButtons2,
            embeddedMessageText2
        )

        val customPayload2 = JSONObject()
        customPayload2.put("someKey", "someValue")

        val embeddedMessage2 = IterableEmbeddedMessage(embeddedMessageMetadata2, embeddedMessageElements2, customPayload2)

        val placementId2: Long = 2
        val messages2: List<IterableEmbeddedMessage> = listOf(embeddedMessage2)

        val embeddedMessagePlacement2 = IterableEmbeddedPlacement(placementId2, messages2)

        val payload = JSONObject(IterableTestUtils.getResourceString("embedded_payload_multiple_2.json"))
        val jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENTS)

        if (jsonArray != null) {
            // GIVEN an embedded message placement payload
            val expectedPlacementJson1 = jsonArray.optJSONObject(0)
            val expectedPlacementJson2 = jsonArray.optJSONObject(1)

            // WHEN you serialize the embedded message payload
            val placementJson1 = IterableEmbeddedPlacement.toJSONObject(embeddedMessagePlacement1)
            val placementJson2 = IterableEmbeddedPlacement.toJSONObject(embeddedMessagePlacement2)

            // THEN we get appropriate embedded message object
            JSONAssert.assertEquals(expectedPlacementJson1, placementJson1, JSONCompareMode.STRICT_ORDER)
            JSONAssert.assertEquals(expectedPlacementJson2, placementJson2, JSONCompareMode.STRICT_ORDER)
        }
    }
}