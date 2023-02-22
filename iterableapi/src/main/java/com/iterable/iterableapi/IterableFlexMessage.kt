package com.iterable.iterableapi

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

val TAG = "IterableFlexMessage"
data class IterableFlexMessage (
    val metadata: FlexMessageMetadata,
    val elements: FlexMessageElements? = null,
    val payload: HashMap<Any, Any>? = null
)
class FlexMessageMetadata(
    var id: String,
    val placementId: String,
    val campaignId: String? = null,
    val isProof: Boolean? = null
) {
    companion object {
        val TAG = "ItblFlexMessageMetadata"

        fun fromJSONObject(flexMessageMetadataJson: JSONObject): FlexMessageMetadata {
            val id: String = flexMessageMetadataJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_ID)
            val placementId: String = flexMessageMetadataJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_PLACEMENT_ID)
            val campaignId: String = flexMessageMetadataJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_CAMPAIGN_ID)
            val isProof: Boolean = flexMessageMetadataJson.optBoolean(IterableConstants.ITERABLE_FLEX_MESSAGE_IS_PROOF)

            return FlexMessageMetadata(id, placementId, campaignId, isProof)
        }
        fun toJSONObject(flexMessageMetadata: FlexMessageMetadata): JSONObject {
            val metadataJson = JSONObject()

            try {
                metadataJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_ID, flexMessageMetadata.id)
                metadataJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_PLACEMENT_ID, flexMessageMetadata.placementId)
                metadataJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_CAMPAIGN_ID, flexMessageMetadata.campaignId)
                metadataJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_IS_PROOF, flexMessageMetadata.isProof)
            } catch (e: JSONException) {
                IterableLogger.e(TAG, "Error while serializing flex metadata", e)
            }

            return metadataJson
        }
    }
}
data class FlexMessageElements (
    val title: String? = null,
    val body: String? = null,
    val mediaURL: String? = null,
    val defaultAction: FlexMessageElementsDefaultAction? = null,
    val buttons: List<FlexMessageElementsButton>? = null,
    val text: List<FlexMessageElementsText>? = null
) {
    companion object {
        fun fromJSONObject(flexMessageElementsJson: JSONObject): FlexMessageElements {
            val title: String = flexMessageElementsJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_TITLE)
            val body: String = flexMessageElementsJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_BODY)
            val mediaURL: String = flexMessageElementsJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_MEDIA_URL)

            val defaultActionJson: JSONObject = flexMessageElementsJson.optJSONObject(IterableConstants.ITERABLE_FLEX_MESSAGE_DEFAULT_ACTION)
            val defaultAction: FlexMessageElementsDefaultAction = FlexMessageElementsDefaultAction.fromJSONObject(defaultActionJson)

            val buttonsJson: JSONArray = flexMessageElementsJson.optJSONArray(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTONS)
            var buttons: List<FlexMessageElementsButton> = listOf()
            for(i in 0..buttonsJson.length()) {
                val buttonJson: JSONObject = buttonsJson.getJSONObject(i)
                val button: FlexMessageElementsButton = FlexMessageElementsButton.fromJSONObject(buttonJson)
                buttons.plus(button)
            }

            val textJson: JSONArray = flexMessageElementsJson.optJSONArray(IterableConstants.ITERABLE_FLEX_MESSAGE_TEXT)
            var texts: List<FlexMessageElementsText> = listOf()
            for(i in 0..buttonsJson.length()) {
                val textJson: JSONObject = textJson.getJSONObject(i)
                val text: FlexMessageElementsButton = FlexMessageElementsButton.fromJSONObject(textJson)
                texts.plus(text)
            }

            return FlexMessageElements(title, body, mediaURL, defaultAction, buttons, texts)
        }
        fun toJSONObject(flexMessageElements: FlexMessageElements): JSONObject {
            val elementsJson = JSONObject()

            try {
                elementsJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_TITLE, flexMessageElements.title)
                elementsJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_BODY, flexMessageElements.body)
                elementsJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_MEDIA_URL, flexMessageElements.mediaURL)
                elementsJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_DEFAULT_ACTION, flexMessageElements.defaultAction)
                elementsJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTONS, flexMessageElements.buttons)
                elementsJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_TEXT, flexMessageElements.text)
            } catch (e: JSONException) {
                IterableLogger.e(FlexMessageMetadata.TAG, "Error while serializing flex elements", e)
            }

            return elementsJson
        }
    }

}
data class FlexMessageElementsButton (
    val id: String,
    val title: String? = null,
    val action: String? = null
) {

}
data class FlexMessageElementsDefaultAction (
    val type: String,
    val data: String
)
data class FlexMessageElementsText (
    val id: String,
    val text: String? = null,
    val label: String? = null
)