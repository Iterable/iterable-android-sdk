package com.iterable.iterableapi

import android.content.Context
import android.os.AsyncTask

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import org.json.JSONException
import org.json.JSONObject

import java.util.Date

internal class OnlineRequestProcessor : RequestProcessor {

    companion object {
        private const val TAG = "OnlineRequestProcessor"
    }

    override fun processGetRequest(apiKey: String?, @NonNull resourcePath: String, @NonNull json: JSONObject, authToken: String?, onCallback: IterableHelper.IterableActionHandler?) {
        val request = IterableApiRequest(apiKey ?: "", resourcePath, addCreatedAtToJson(json), IterableApiRequest.GET, authToken, onCallback)
        IterableRequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request)
    }

    override fun processGetRequest(apiKey: String?, @NonNull resourcePath: String, @NonNull json: JSONObject, authToken: String?, onSuccess: IterableHelper.SuccessHandler?, onFailure: IterableHelper.FailureHandler?) {
        val request = IterableApiRequest(apiKey ?: "", resourcePath, addCreatedAtToJson(json), IterableApiRequest.GET, authToken, onSuccess, onFailure)
        IterableRequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request)
    }

    override fun processPostRequest(apiKey: String?, @NonNull resourcePath: String, @NonNull json: JSONObject, authToken: String?, onSuccess: IterableHelper.SuccessHandler?, onFailure: IterableHelper.FailureHandler?) {
        val request = IterableApiRequest(apiKey ?: "", resourcePath, addCreatedAtToJson(json), IterableApiRequest.POST, authToken, onSuccess, onFailure)
        IterableRequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request)
    }

    override fun onLogout(context: Context) {

    }

    private fun addCreatedAtToJson(jsonObject: JSONObject): JSONObject {
        try {
            jsonObject.put(IterableConstants.KEY_CREATED_AT, Date().time / 1000)
        } catch (e: JSONException) {
            IterableLogger.e(TAG, "Could not add createdAt timestamp to json object")
        }
        return jsonObject
    }

}
