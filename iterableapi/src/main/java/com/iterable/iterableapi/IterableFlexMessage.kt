package com.iterable.iterableapi

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
data class IterableFlexMessage (
    val metadata: FlexMessageMetadata,
    val elements: FlexMessageElements? = null,
    val payload: JSONObject? = null
) {
    fun toJSONObject(): JSONObject {
        val flexMessageJson = JSONObject()

        try {
            flexMessageJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_METADATA, metadata.toJSONObject())
            flexMessageJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_ELEMENTS, elements?.toJSONObject())
            flexMessageJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_PAYLOAD, payload)
        } catch(e: JSONException) {
            IterableLogger.e(FlexMessageMetadata.TAG, "Error while serializing flex message", e)
        }

        return flexMessageJson
    }

    companion object {
        val TAG = "ItblFlexMessage"

        fun fromJSONObject(flexMessageJson: JSONObject): IterableFlexMessage {
            val metadataJson: JSONObject = flexMessageJson.getJSONObject(IterableConstants.ITERABLE_FLEX_MESSAGE_METADATA)
            val metadata: FlexMessageMetadata = FlexMessageMetadata.fromJSONObject(metadataJson)

            val elementsJson: JSONObject = flexMessageJson.getJSONObject(IterableConstants.ITERABLE_FLEX_MESSAGE_ELEMENTS)
            val elements: FlexMessageElements = FlexMessageElements.fromJSONObject(elementsJson)

            val payload: JSONObject = flexMessageJson.getJSONObject(IterableConstants.ITERABLE_FLEX_MESSAGE_PAYLOAD)

            return IterableFlexMessage(metadata, elements, payload)
        }
    }
}
class FlexMessageMetadata(
    var id: String,
    val placementId: String,
    val campaignId: String? = null,
    val isProof: Boolean? = null
) {
    fun toJSONObject(): JSONObject {
        val metadataJson = JSONObject()

        try {
            metadataJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_ID, id)
            metadataJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_PLACEMENT_ID, placementId)
            metadataJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_CAMPAIGN_ID, campaignId)
            metadataJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_IS_PROOF, isProof)
        } catch (e: JSONException) {
            IterableLogger.e(TAG, "Error while serializing flex metadata", e)
        }

        return metadataJson
    }
    companion object {
        val TAG = "ItblFlexMessageMetadata"

        fun fromJSONObject(flexMessageMetadataJson: JSONObject): FlexMessageMetadata {
            val id: String = flexMessageMetadataJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_ID)
            val placementId: String = flexMessageMetadataJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_PLACEMENT_ID)
            val campaignId: String = flexMessageMetadataJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_CAMPAIGN_ID)
            val isProof: Boolean = flexMessageMetadataJson.optBoolean(IterableConstants.ITERABLE_FLEX_MESSAGE_IS_PROOF)

            return FlexMessageMetadata(id, placementId, campaignId, isProof)
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
    fun toJSONObject(): JSONObject {
        val elementsJson = JSONObject()

        try {
            elementsJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_TITLE, title)
            elementsJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_BODY, body)
            elementsJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_MEDIA_URL, mediaURL)

//            if(defaultAction != null) {
                elementsJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_DEFAULT_ACTION, defaultAction?.toJSONObject())
//            }

            if(buttons != null) {
                val buttonsJson = JSONArray()
                for(i in 0..buttons.size - 1) {
                    buttonsJson.put(buttons.get(i).toJSONObject())
                }
                elementsJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTONS, buttonsJson)
            }

            if(text != null) {
                val textJson = JSONArray()
                for(i in 0..text.size - 1) {
                    textJson.put(text.get(i).toJSONObject())
                }

                elementsJson.putOpt(IterableConstants.ITERABLE_FLEX_MESSAGE_TEXT, textJson)
            }
        } catch (e: JSONException) {
            IterableLogger.e(TAG, "Error while serializing flex elements", e)
        }

        return elementsJson
    }
    companion object {
        val TAG = "ItblFlexMessageElements"
        fun fromJSONObject(flexMessageElementsJson: JSONObject): FlexMessageElements {
            val title: String? = flexMessageElementsJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_TITLE)
            val body: String? = flexMessageElementsJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_BODY)
            val mediaURL: String? = flexMessageElementsJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_MEDIA_URL)

            val defaultActionJson: JSONObject? = flexMessageElementsJson.optJSONObject(IterableConstants.ITERABLE_FLEX_MESSAGE_DEFAULT_ACTION)
            var defaultAction: FlexMessageElementsDefaultAction? = null
            if (defaultActionJson != null) {
                defaultAction = FlexMessageElementsDefaultAction.fromJSONObject(defaultActionJson)
            }

            val buttonsJson: JSONArray? = flexMessageElementsJson.optJSONArray(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTONS)
            val buttons: MutableList<FlexMessageElementsButton> = mutableListOf()
            if (buttonsJson != null) {
                for(i in 0..buttonsJson.length() - 1) {
                    val buttonJson: JSONObject = buttonsJson.getJSONObject(i)
                    val button: FlexMessageElementsButton = FlexMessageElementsButton.fromJSONObject(buttonJson)
                    buttons.add(button)
                }
            }

            val textsJson: JSONArray? = flexMessageElementsJson.optJSONArray(IterableConstants.ITERABLE_FLEX_MESSAGE_TEXT)
            val texts: MutableList<FlexMessageElementsText> = mutableListOf()
            if (textsJson != null) {
                for(i in 0..textsJson.length() - 1) {
                    val textJson: JSONObject = textsJson.getJSONObject(i)
                    val text: FlexMessageElementsText = FlexMessageElementsText.fromJSONObject(textJson)
                    texts.add(text)
                }
            }

            return FlexMessageElements(title, body, mediaURL, defaultAction, buttons, texts)
        }

    }
}

class FlexMessageElementsButton (
    val id: String,
    val title: String? = null,
    val action: String? = null
) {
    fun toJSONObject(): JSONObject {
        val buttonJson = JSONObject()

        try {
            buttonJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_ID, id)
            buttonJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_TITLE, title)
            buttonJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_ACTION, action)
        } catch (e: JSONException) {
            IterableLogger.e(TAG, "Error while serializing flex message button", e)
        }

        return buttonJson
    }
    companion object {
        val TAG = "ItblFlexMessageButtons"
        fun fromJSONObject(flexMessageElementsButtonJson: JSONObject): FlexMessageElementsButton {
            val id: String = flexMessageElementsButtonJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_ID)
            val title: String = flexMessageElementsButtonJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_TITLE)
            val action: String = flexMessageElementsButtonJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_ACTION)

            return FlexMessageElementsButton(id, title, action)
        }

    }
}
class FlexMessageElementsDefaultAction (
    val type: String,
    val data: String
) {
    fun toJSONObject(): JSONObject {
        val defaultActionJson = JSONObject()

        try {
            defaultActionJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_DEFAULT_ACTION_TYPE, type)
            defaultActionJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_DEFAULT_ACTION_DATA, data)
        } catch (e: JSONException) {
            IterableLogger.e(TAG, "Error while serializing flex default action", e)
        }

        return defaultActionJson
    }

    companion object {
        val TAG = "ItblDefaultAction"
        fun fromJSONObject(flexMessageElementsDefaultActionJson: JSONObject): FlexMessageElementsDefaultAction {
            val type: String = flexMessageElementsDefaultActionJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_DEFAULT_ACTION_TYPE)
            val data: String = flexMessageElementsDefaultActionJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_DEFAULT_ACTION_DATA)

            return FlexMessageElementsDefaultAction(type, data)
        }
    }
}
class FlexMessageElementsText (
    val id: String,
    val text: String? = null,
    val label: String? = null
) {
    fun toJSONObject(): JSONObject {
        val textJson = JSONObject()

        try {
            textJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_ID, id)
            textJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_TITLE, text)
            textJson.put(IterableConstants.ITERABLE_FLEX_MESSAGE_BUTTON_ACTION, label)
        } catch (e: JSONException) {
            IterableLogger.e(TAG, "Error while serializing flex message text", e)
        }

        return textJson
    }
    companion object {
        val TAG = "ItblFlexMessageText"
        fun fromJSONObject(flexMessageElementsTextJson: JSONObject): FlexMessageElementsText {
            val id: String = flexMessageElementsTextJson.getString(IterableConstants.ITERABLE_FLEX_MESSAGE_TEXT_ID)
            val text: String = flexMessageElementsTextJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_TEXT_TITLE)
            val label: String = flexMessageElementsTextJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_TEXT_LABEL)

            return FlexMessageElementsText(id, text, label)
        }
    }
}