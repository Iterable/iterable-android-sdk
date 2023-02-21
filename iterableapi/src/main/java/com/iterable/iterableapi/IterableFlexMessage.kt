package com.iterable.iterableapi

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
            val id: String = flexMessageMetadataJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_ID)
            val placementId: String = flexMessageMetadataJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_PLACEMENT_ID)
            val campaignId: String = flexMessageMetadataJson.optString(IterableConstants.ITERABLE_FLEX_MESSAGE_CAMPAIGN_ID)
            val isProof: Boolean = flexMessageMetadataJson.optBoolean(IterableConstants.ITERABLE_FLEX_MESSAGE_IS_PROOF)

            return FlexMessageMetadata(id, placementId, campaignId, isProof)
        }
        fun toJSONObject(flexMessageMetadata: FlexMessageMetadata): JSONObject {
            val metadataJson = JSONObject()

            try {
                metadataJson.putOpt(IterableConstants.ITERABLE_IN_APP_INBOX_TITLE, flexMessageMetadata.id)
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
)
data class FlexMessageElementsButton (
    val id: String,
    val title: String? = null,
    val action: String? = null
)
data class FlexMessageElementsDefaultAction (
    val type: String,
    val data: String
)
data class FlexMessageElementsText (
    val id: String,
    val text: String? = null,
    val label: String? = null
)