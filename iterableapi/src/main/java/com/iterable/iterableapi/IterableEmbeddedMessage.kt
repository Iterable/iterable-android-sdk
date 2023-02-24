package com.iterable.iterableapi

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class IterableEmbeddedMessage (
    val metadata: EmbeddedMessageMetadata,
    val elements: EmbeddedMessageElements? = null,
    val payload: JSONObject? = null
) {
    fun toJSONObject(): JSONObject {
        val embeddedMessageJson = JSONObject()

        try {
            embeddedMessageJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_METADATA, metadata.toJSONObject())
            embeddedMessageJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_ELEMENTS, elements?.toJSONObject())
            embeddedMessageJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PAYLOAD, payload)
        } catch(e: JSONException) {
            IterableLogger.e(TAG, "Error while serializing flex message", e)
        }

        return embeddedMessageJson
    }

    companion object {
        val TAG = "ItblFlexMessage"

        fun fromJSONObject(flexMessageJson: JSONObject): IterableEmbeddedMessage {
            val metadataJson: JSONObject = flexMessageJson.getJSONObject(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_METADATA)
            val metadata: EmbeddedMessageMetadata = EmbeddedMessageMetadata.fromJSONObject(metadataJson)

            val elementsJson: JSONObject = flexMessageJson.getJSONObject(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_ELEMENTS)
            val elements: EmbeddedMessageElements = EmbeddedMessageElements.fromJSONObject(elementsJson)

            val payload: JSONObject = flexMessageJson.getJSONObject(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PAYLOAD)

            return IterableEmbeddedMessage(metadata, elements, payload)
        }
    }
}

class EmbeddedMessageMetadata(
    var id: String,
    val placementId: String,
    val campaignId: String? = null,
    val isProof: Boolean? = null
) {
    fun toJSONObject(): JSONObject {
        val metadataJson = JSONObject()

        try {
            metadataJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_ID, id)
            metadataJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENT_ID, placementId)
            metadataJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_CAMPAIGN_ID, campaignId)
            metadataJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_IS_PROOF, isProof)
        } catch (e: JSONException) {
            IterableLogger.e(TAG, "Error while serializing flex metadata", e)
        }

        return metadataJson
    }

    companion object {
        val TAG = "ItblFlexMessageMetadata"

        fun fromJSONObject(flexMessageMetadataJson: JSONObject): EmbeddedMessageMetadata {
            val id: String = flexMessageMetadataJson.getString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_ID)
            val placementId: String = flexMessageMetadataJson.getString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENT_ID)
            val campaignId: String = flexMessageMetadataJson.optString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_CAMPAIGN_ID)
            val isProof: Boolean = flexMessageMetadataJson.optBoolean(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_IS_PROOF)

            return EmbeddedMessageMetadata(id, placementId, campaignId, isProof)
        }
    }
}

class EmbeddedMessageElements (
    val title: String? = null,
    val body: String? = null,
    val mediaURL: String? = null,
    val defaultAction: EmbeddedMessageElementsDefaultAction? = null,
    val buttons: List<EmbeddedMessageElementsButton>? = null,
    val text: List<EmbeddedMessageElementsText>? = null
) {
    fun toJSONObject(): JSONObject {
        val elementsJson = JSONObject()

        try {
            elementsJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TITLE, title)
            elementsJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BODY, body)
            elementsJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_MEDIA_URL, mediaURL)

            elementsJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION, defaultAction?.toJSONObject())

            if(buttons != null) {
                val buttonsJson = JSONArray()
                for(i in 0..buttons.size - 1) {
                    buttonsJson.put(buttons.get(i).toJSONObject())
                }
                elementsJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTONS, buttonsJson)
            }

            if(text != null) {
                val textJson = JSONArray()
                for(i in 0..text.size - 1) {
                    textJson.put(text.get(i).toJSONObject())
                }

                elementsJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TEXT, textJson)
            }
        } catch (e: JSONException) {
            IterableLogger.e(TAG, "Error while serializing flex elements", e)
        }

        return elementsJson
    }

    companion object {
        val TAG = "ItblFlexMessageElements"
        fun fromJSONObject(elementsJson: JSONObject): EmbeddedMessageElements {
            val title: String? = elementsJson.optString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TITLE)
            val body: String? = elementsJson.optString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BODY)
            val mediaURL: String? = elementsJson.optString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_MEDIA_URL)

            val defaultActionJson: JSONObject? = elementsJson.optJSONObject(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION)
            var defaultAction: EmbeddedMessageElementsDefaultAction? = null
            if (defaultActionJson != null) {
                defaultAction = EmbeddedMessageElementsDefaultAction.fromJSONObject(defaultActionJson)
            }

            val buttonsJson: JSONArray? = elementsJson.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTONS)
            val buttons: MutableList<EmbeddedMessageElementsButton> = mutableListOf()
            if (buttonsJson != null) {
                for(i in 0..buttonsJson.length() - 1) {
                    val buttonJson: JSONObject = buttonsJson.getJSONObject(i)
                    val button: EmbeddedMessageElementsButton = EmbeddedMessageElementsButton.fromJSONObject(buttonJson)
                    buttons.add(button)
                }
            }

            val textsJson: JSONArray? = elementsJson.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TEXT)
            val texts: MutableList<EmbeddedMessageElementsText> = mutableListOf()
            if (textsJson != null) {
                for(i in 0..textsJson.length() - 1) {
                    val textJson: JSONObject = textsJson.getJSONObject(i)
                    val text: EmbeddedMessageElementsText = EmbeddedMessageElementsText.fromJSONObject(textJson)
                    texts.add(text)
                }
            }

            return EmbeddedMessageElements(title, body, mediaURL, defaultAction, buttons, texts)
        }

    }
}

class EmbeddedMessageElementsButton (
    val id: String,
    val title: String? = null,
    val action: String? = null
) {
    fun toJSONObject(): JSONObject {
        val buttonJson = JSONObject()

        try {
            buttonJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_ID, id)
            buttonJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_TITLE, title)
            buttonJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_ACTION, action)
        } catch (e: JSONException) {
            IterableLogger.e(TAG, "Error while serializing flex message button", e)
        }

        return buttonJson
    }
    companion object {
        val TAG = "ItblFlexMessageButtons"
        fun fromJSONObject(buttonJson: JSONObject): EmbeddedMessageElementsButton {
            val id: String = buttonJson.getString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_ID)
            val title: String = buttonJson.optString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_TITLE)
            val action: String = buttonJson.optString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_ACTION)

            return EmbeddedMessageElementsButton(id, title, action)
        }

    }
}

class EmbeddedMessageElementsDefaultAction (
    val type: String,
    val data: String
) {
    fun toJSONObject(): JSONObject {
        val defaultActionJson = JSONObject()

        try {
            defaultActionJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION_TYPE, type)
            defaultActionJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION_DATA, data)
        } catch (e: JSONException) {
            IterableLogger.e(TAG, "Error while serializing flex default action", e)
        }

        return defaultActionJson
    }

    companion object {
        val TAG = "ItblDefaultAction"
        fun fromJSONObject(flexMessageElementsDefaultActionJson: JSONObject): EmbeddedMessageElementsDefaultAction {
            val type: String = flexMessageElementsDefaultActionJson.getString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION_TYPE)
            val data: String = flexMessageElementsDefaultActionJson.getString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION_DATA)

            return EmbeddedMessageElementsDefaultAction(type, data)
        }
    }
}

class EmbeddedMessageElementsText (
    val id: String,
    val text: String? = null,
    val label: String? = null
) {
    fun toJSONObject(): JSONObject {
        val textJson = JSONObject()

        try {
            textJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_ID, id)
            textJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_TITLE, text)
            textJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_ACTION, label)
        } catch (e: JSONException) {
            IterableLogger.e(TAG, "Error while serializing flex message text", e)
        }

        return textJson
    }
    companion object {
        val TAG = "ItblFlexMessageText"
        fun fromJSONObject(textJson: JSONObject): EmbeddedMessageElementsText {
            val id: String = textJson.getString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TEXT_ID)
            val text: String = textJson.optString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TEXT_TITLE)
            val label: String = textJson.optString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TEXT_LABEL)

            return EmbeddedMessageElementsText(id, text, label)
        }
    }
}