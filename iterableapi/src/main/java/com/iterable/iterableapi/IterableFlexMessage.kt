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
                metadataJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_ID, flexMessageMetadata.id)
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
class FlexMessageElements (
    val title: String? = null,
    val body: String? = null,
    val mediaURL: String? = null,
    val defaultAction: FlexMessageElementsDefaultAction? = null,
    val buttons: List<FlexMessageElementsButton>? = null,
    val text: List<FlexMessageElementsText>? = null
) {
    companion object {
        val TAG = "ItblFlexMessageElements"
        fun fromJSONObject(flexMessageElementsJson: JSONObject): FlexMessageElements {
            val title: String = flexMessageElementsJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_TITLE)
            val body: String = flexMessageElementsJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_BODY)
            val mediaURL: String = flexMessageElementsJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_MEDIA_URL)

            val defaultActionJson: JSONObject = flexMessageElementsJson.getJSONObject(IterableConstants.ITERABLE_FLEX_MESSAGE_DEFAULT_ACTION)
            val defaultAction: FlexMessageElementsDefaultAction = FlexMessageElementsDefaultAction.fromJSONObject(defaultActionJson)

            val buttonsJson: JSONArray = flexMessageElementsJson.getJSONArray(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTONS)
            val buttons: List<FlexMessageElementsButton> = listOf()
            for(i in 0..buttonsJson.length()) {
                val buttonJson: JSONObject = buttonsJson.getJSONObject(i)
                val button: FlexMessageElementsButton = FlexMessageElementsButton.fromJSONObject(buttonJson)
                buttons.plus(button)
            }

            val textsJson: JSONArray = flexMessageElementsJson.getJSONArray(IterableConstants.ITERABLE_FLEX_MESSAGE_TEXT)
            val texts: List<FlexMessageElementsText> = listOf()
            for(i in 0..buttonsJson.length()) {
                val textJson: JSONObject = textsJson.getJSONObject(i)
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
class FlexMessageElementsButton (
    val id: String,
    val title: String? = null,
    val action: String? = null
) {
    companion object {
        val TAG = "ItblFlexMessageButtons"
        fun fromJSONObject(flexMessageElementsButtonJson: JSONObject): FlexMessageElementsButton {
            val id: String = flexMessageElementsButtonJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_ID)
            val title: String = flexMessageElementsButtonJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_TITLE)
            val action: String = flexMessageElementsButtonJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_ACTION)

            return FlexMessageElementsButton(id, title, action)
        }
        fun toJSONObject(flexMessageElementsButton: FlexMessageElementsButton): JSONObject {
            val buttonJson = JSONObject()

            try {
                buttonJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_ID, flexMessageElementsButton.id)
                buttonJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_TITLE, flexMessageElementsButton.title)
                buttonJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_ACTION, flexMessageElementsButton.action)
            } catch (e: JSONException) {
                IterableLogger.e(FlexMessageMetadata.TAG, "Error while serializing flex message button", e)
            }

            return buttonJson
        }
    }
}
class FlexMessageElementsDefaultAction (
    val type: String,
    val data: String
) {
    companion object {
        val TAG = "ItblFlexMessageDefaultAction"
        fun fromJSONObject(flexMessageElementsDefaultActionJson: JSONObject): FlexMessageElementsDefaultAction {
            val type: String = flexMessageElementsDefaultActionJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_DEFAULT_ACTION_TYPE)
            val data: String = flexMessageElementsDefaultActionJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_DEFAULT_ACTION_DATA)

            return FlexMessageElementsDefaultAction(type, data)
        }
        fun toJSONObject(flexMessageElementsDefaultAction: FlexMessageElementsDefaultAction): JSONObject {
            val defaultActionJson = JSONObject()

            try {
                defaultActionJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_DEFAULT_ACTION_TYPE, flexMessageElementsDefaultAction.type)
                defaultActionJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_DEFAULT_ACTION_DATA, flexMessageElementsDefaultAction.data)
            } catch (e: JSONException) {
                IterableLogger.e(FlexMessageMetadata.TAG, "Error while serializing flex default action", e)
            }

            return defaultActionJson
        }
    }
}
class FlexMessageElementsText (
    val id: String,
    val text: String? = null,
    val label: String? = null
) {
    companion object {
        val TAG = "ItblFlexMessageText"
        fun fromJSONObject(flexMessageElementsTextJson: JSONObject): FlexMessageElementsText {
            val id: String = flexMessageElementsTextJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_TEXT_ID)
            val text: String = flexMessageElementsTextJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_TEXT_TITLE)
            val label: String = flexMessageElementsTextJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_TEXT_LABEL)

            return FlexMessageElementsText(id, text, label)
        }
        fun toJSONObject(flexMessageElementsText: FlexMessageElementsText): JSONObject {
            val textJson = JSONObject()

            try {
                textJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_ID, flexMessageElementsText.id)
                textJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_TITLE, flexMessageElementsText.text)
                textJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_ACTION, flexMessageElementsText.label)
            } catch (e: JSONException) {
                IterableLogger.e(FlexMessageMetadata.TAG, "Error while serializing flex message text", e)
            }

            return textJson
        }
    }
}

