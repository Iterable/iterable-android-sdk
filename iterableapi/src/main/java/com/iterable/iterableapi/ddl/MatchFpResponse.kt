package com.iterable.iterableapi.ddl

import org.json.JSONException
import org.json.JSONObject

class MatchFpResponse(
    val isMatch: Boolean,
    val destinationUrl: String,
    val campaignId: Int,
    val templateId: Int,
    val messageId: String
) {

    companion object {
        @Throws(JSONException::class)
        fun fromJSONObject(json: JSONObject): MatchFpResponse {
            return MatchFpResponse(
                json.getBoolean("isMatch"),
                json.getString("destinationUrl"),
                json.getInt("campaignId"),
                json.getInt("templateId"),
                json.getString("messageId")
            )
        }
    }
}
