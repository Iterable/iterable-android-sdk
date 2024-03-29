package com.iterable.iterableapi

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class IterableEmbeddedPlacement(
    val placementId: Long,
    val messages: List<IterableEmbeddedMessage>
) {
    companion object {
        val TAG = "ItblEmbeddedPlacement"
        fun toJSONObject(placement: IterableEmbeddedPlacement): JSONObject {

            val embeddedPlacementJson = JSONObject()

            try {
                embeddedPlacementJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENT_ID, placement.placementId)

                val messagesJson = JSONArray()
                for(i in 0 until placement.messages.size) {
                    messagesJson.put(IterableEmbeddedMessage.toJSONObject(placement.messages[i]))
                }
                embeddedPlacementJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE, messagesJson)
            } catch(e: JSONException) {
                IterableLogger.e(TAG, "Error while serializing flex message", e)
            }

            return embeddedPlacementJson
        }

        fun fromJSONObject(placementJson: JSONObject): IterableEmbeddedPlacement {
            val placementId: Long = placementJson.getLong(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENT_ID)

            val messagesJson: JSONArray = placementJson.getJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE)
            var messages: MutableList<IterableEmbeddedMessage> = mutableListOf()

            for(i in 0 until messagesJson.length()) {
                val messageJson: JSONObject = messagesJson.getJSONObject(i)
                val message: IterableEmbeddedMessage = IterableEmbeddedMessage.fromJSONObject(messageJson)
                messages.add(message)
            }

            return IterableEmbeddedPlacement(placementId, messages)
        }
    }
}

data class IterableEmbeddedMessage (
    val metadata: EmbeddedMessageMetadata,
    val elements: EmbeddedMessageElements? = null,
    val payload: JSONObject? = null
) {
    companion object {
        val TAG = "ItblEmbeddedMessage"
        fun toJSONObject(message: IterableEmbeddedMessage): JSONObject {
            val embeddedMessageJson = JSONObject()

            try {
                embeddedMessageJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_METADATA, EmbeddedMessageMetadata.toJSONObject(message.metadata))
                embeddedMessageJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_ELEMENTS, EmbeddedMessageElements.toJSONObject(message.elements))
                embeddedMessageJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PAYLOAD, message.payload)
            } catch(e: JSONException) {
                IterableLogger.e(TAG, "Error while serializing flex message", e)
            }

            return embeddedMessageJson
        }


        fun fromJSONObject(messageJson: JSONObject): IterableEmbeddedMessage {
            val metadataJson: JSONObject = messageJson.getJSONObject(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_METADATA)
            val metadata: EmbeddedMessageMetadata = EmbeddedMessageMetadata.fromJSONObject(metadataJson)

            val elementsJson: JSONObject? = messageJson.optJSONObject(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_ELEMENTS)
            val elements: EmbeddedMessageElements? = EmbeddedMessageElements.fromJSONObject(elementsJson)

            val payload: JSONObject? = messageJson.optJSONObject(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PAYLOAD)

            return IterableEmbeddedMessage(metadata, elements, payload)
        }
    }
}

class EmbeddedMessageMetadata(
    var messageId: String,
    val placementId: Long,
    val campaignId: Int? = null,
    val isProof: Boolean = false
) {
    companion object {
        val TAG = "ItblEmbeddedMessageMetadata"

        fun toJSONObject(metadata: EmbeddedMessageMetadata): JSONObject {
            val metadataJson = JSONObject()

            try {
                metadataJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_ID, metadata.messageId)
                metadataJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENT_ID, metadata.placementId)
                metadataJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_CAMPAIGN_ID, metadata.campaignId)
                metadataJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_IS_PROOF, metadata.isProof)
            } catch (e: JSONException) {
                IterableLogger.e(TAG, "Error while serializing flex metadata", e)
            }

            return metadataJson
        }

        fun fromJSONObject(metadataJson: JSONObject): EmbeddedMessageMetadata {
            val messageId: String = metadataJson.getString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_ID)
            val placementId: Long = metadataJson.optLong(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENT_ID)
            val campaignId: Int = metadataJson.optInt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_CAMPAIGN_ID)
            val isProof: Boolean = metadataJson.optBoolean(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_IS_PROOF)

            return EmbeddedMessageMetadata(messageId, placementId, campaignId, isProof)
        }
    }
}

class EmbeddedMessageElements (
    val title: String? = null,
    val body: String? = null,
    val mediaURL: String? = null,
    val mediaUrlCaption: String? = null,
    val defaultAction: EmbeddedMessageElementsDefaultAction? = null,
    val buttons: List<EmbeddedMessageElementsButton>? = null,
    val text: List<EmbeddedMessageElementsText>? = null
) {

    companion object {
        val TAG = "ItblEmbeddedMessageElements"

        fun toJSONObject(elements: EmbeddedMessageElements?): JSONObject {
            val elementsJson = JSONObject()

            try {
                elementsJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TITLE, elements?.title)
                elementsJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BODY, elements?.body)
                elementsJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_MEDIA_URL, elements?.mediaURL)
                elementsJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_MEDIA_URL_CAPTION, elements?.mediaUrlCaption)

                if(elements?.defaultAction != null) {
                    elementsJson.putOpt(
                        IterableConstants.ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION,
                        EmbeddedMessageElementsDefaultAction.toJSONObject(elements.defaultAction)
                    )
                }

                if(elements?.buttons != null) {
                    val buttonsJson = JSONArray()
                    for(i in 0..elements.buttons.size - 1) {
                        buttonsJson.put(EmbeddedMessageElementsButton.toJSONObject(elements.buttons.get(i)))
                    }
                    elementsJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTONS, buttonsJson)
                }

                if(elements?.text != null) {
                    val textJson = JSONArray()
                    for(i in 0..elements.text.size - 1) {
                        textJson.put(EmbeddedMessageElementsText.toJSONObject(elements.text.get(i)))
                    }

                    elementsJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TEXT, textJson)
                }
            } catch (e: JSONException) {
                IterableLogger.e(TAG, "Error while serializing flex elements", e)
            }

            return elementsJson
        }
        fun fromJSONObject(elementsJson: JSONObject?): EmbeddedMessageElements? {
            if(elementsJson == null) {
                return null
            }

            val title: String? = elementsJson.optString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TITLE)
            val body: String? = elementsJson.optString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BODY)
            val mediaURL: String? = elementsJson.optString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_MEDIA_URL)
            val mediaUrlCaption: String? = elementsJson.optString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_MEDIA_URL)

            val defaultActionJson: JSONObject? = elementsJson.optJSONObject(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION)
            var defaultAction: EmbeddedMessageElementsDefaultAction? = null
            if (defaultActionJson != null) {
                defaultAction = EmbeddedMessageElementsDefaultAction.fromJSONObject(defaultActionJson)
            }

            val buttonsJson: JSONArray? = elementsJson.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTONS)
            var buttons: MutableList<EmbeddedMessageElementsButton>? = mutableListOf()
            if (buttonsJson != null) {
                for(i in 0..buttonsJson.length() - 1) {
                    val buttonJson: JSONObject = buttonsJson.getJSONObject(i)
                    val button: EmbeddedMessageElementsButton = EmbeddedMessageElementsButton.fromJSONObject(buttonJson)
                    buttons?.add(button)
                }
            } else {
                buttons = null
            }

            val textsJson: JSONArray? = elementsJson.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TEXT)
            var texts: MutableList<EmbeddedMessageElementsText>? = mutableListOf()
            if (textsJson != null) {
                for(i in 0..textsJson.length() - 1) {
                    val textJson: JSONObject = textsJson.getJSONObject(i)
                    val text: EmbeddedMessageElementsText = EmbeddedMessageElementsText.fromJSONObject(textJson)
                    texts?.add(text)
                }
            } else {
                texts = null
            }

            return EmbeddedMessageElements(title, body, mediaURL, mediaUrlCaption, defaultAction, buttons, texts)
        }
    }
}

class EmbeddedMessageElementsButton (
    val id: String,
    val title: String? = null,
    val action: EmbeddedMessageElementsButtonAction? = null
) {

    companion object {
        val TAG = "ItblEmbeddedMessageButtons"

        fun toJSONObject(button: EmbeddedMessageElementsButton): JSONObject {
            val buttonJson = JSONObject()

            try {
                buttonJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_ID, button.id)
                buttonJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_TITLE, button.title)

                if(button.action != null) {
                    buttonJson.putOpt(
                        IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_ACTION,
                        EmbeddedMessageElementsButtonAction.toJSONObject(button.action)
                    )
                }

            } catch (e: JSONException) {
                IterableLogger.e(TAG, "Error while serializing flex message button", e)
            }

            return buttonJson
        }
        fun fromJSONObject(buttonJson: JSONObject): EmbeddedMessageElementsButton {
            val id: String = buttonJson.getString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_ID)
            val title: String = buttonJson.optString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_TITLE)

            val buttonActionJson: JSONObject? = buttonJson.optJSONObject(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_ACTION)
            var action: EmbeddedMessageElementsButtonAction? = null
            if (buttonActionJson != null) {
                action = EmbeddedMessageElementsButtonAction.fromJSONObject(buttonActionJson)
            }

            return EmbeddedMessageElementsButton(id, title, action)
        }

    }
}

class EmbeddedMessageElementsDefaultAction (
    val type: String,
    val data: String
) {

    companion object {
        val TAG = "ItblEmbeddedDefaultAction"

        fun toJSONObject(defaultAction: EmbeddedMessageElementsDefaultAction): JSONObject {
            val defaultActionJson = JSONObject()

            try {
                defaultActionJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION_TYPE, defaultAction.type)
                defaultActionJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION_DATA, defaultAction.data)
            } catch (e: JSONException) {
                IterableLogger.e(TAG, "Error while serializing flex default action", e)
            }

            return defaultActionJson
        }
        fun fromJSONObject(defaultActionJson: JSONObject): EmbeddedMessageElementsDefaultAction {
            val type: String = defaultActionJson.getString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION_TYPE)
            val data: String = defaultActionJson.getString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION_DATA)

            return EmbeddedMessageElementsDefaultAction(type, data)
        }
    }
}

class EmbeddedMessageElementsButtonAction (
    val type: String,
    val data: String
) {

    companion object {
        val TAG = "ItblEmbeddedButtonAction"

        fun toJSONObject(buttonAction: EmbeddedMessageElementsButtonAction): JSONObject {
            val buttonActionJson = JSONObject()

            try {
                buttonActionJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_ACTION_TYPE, buttonAction.type)
                buttonActionJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_ACTION_DATA, buttonAction.data)
            } catch (e: JSONException) {
                IterableLogger.e(TAG, "Error while serializing flex default action", e)
            }

            return buttonActionJson
        }
        fun fromJSONObject(buttonActionJson: JSONObject): EmbeddedMessageElementsButtonAction {
            val type: String = buttonActionJson.getString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_ACTION_TYPE)
            val data: String = buttonActionJson.getString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION_DATA)

            return EmbeddedMessageElementsButtonAction(type, data)
        }
    }
}
class EmbeddedMessageElementsText (
    val id: String,
    val text: String? = null,
    val label: String? = null
) {

    companion object {
        val TAG = "ItblEmbeddedMessageText"

        fun toJSONObject(text: EmbeddedMessageElementsText): JSONObject {
            val textJson = JSONObject()

            try {
                textJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TEXT_ID, text.id)
                textJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TEXT_TEXT, text.text)
                textJson.putOpt(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TEXT_LABEL, text.label)
            } catch (e: JSONException) {
                IterableLogger.e(TAG, "Error while serializing flex message text", e)
            }

            return textJson
        }
        fun fromJSONObject(textJson: JSONObject): EmbeddedMessageElementsText {
            val id: String = textJson.getString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TEXT_ID)
            val text: String = textJson.optString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TEXT_TEXT)
            val label: String = textJson.optString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_TEXT_LABEL)

            return EmbeddedMessageElementsText(id, text, label)
        }
    }
}