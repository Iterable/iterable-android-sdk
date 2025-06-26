package com.iterable.iterableapi

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import org.json.JSONException
import org.json.JSONObject

class IterableAttributionInfo(
    val campaignId: Int,
    val templateId: Int,
    @Nullable val messageId: String?
) {

    @NonNull
    fun toJSONObject(): JSONObject {
        val jsonObject = JSONObject()
        try {
            jsonObject.put(IterableConstants.KEY_CAMPAIGN_ID, campaignId)
            jsonObject.put(IterableConstants.KEY_TEMPLATE_ID, templateId)
            jsonObject.put(IterableConstants.KEY_MESSAGE_ID, messageId)
        } catch (ignored: JSONException) {
        }
        return jsonObject
    }

    companion object {
        @Nullable
        fun fromJSONObject(@Nullable jsonObject: JSONObject?): IterableAttributionInfo? {
            return if (jsonObject != null) {
                IterableAttributionInfo(
                    jsonObject.optInt(IterableConstants.KEY_CAMPAIGN_ID),
                    jsonObject.optInt(IterableConstants.KEY_TEMPLATE_ID),
                    jsonObject.optString(IterableConstants.KEY_MESSAGE_ID)
                )
            } else {
                null
            }
        }
    }
}
