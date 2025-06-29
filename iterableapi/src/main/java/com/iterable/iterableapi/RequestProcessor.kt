package com.iterable.iterableapi

import android.content.Context

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import org.json.JSONObject

interface RequestProcessor {
    fun processGetRequest(apiKey: String?, @NonNull resourcePath: String, @NonNull json: JSONObject, authToken: String?, onCallback: IterableHelper.IterableActionHandler?)

    fun processGetRequest(apiKey: String?, @NonNull resourcePath: String, @NonNull json: JSONObject, authToken: String?, onSuccess: IterableHelper.SuccessHandler?, onFailure: IterableHelper.FailureHandler?)

    fun processPostRequest(apiKey: String?, @NonNull resourcePath: String, @NonNull json: JSONObject, authToken: String?, onSuccess: IterableHelper.SuccessHandler?, onFailure: IterableHelper.FailureHandler?)
    fun onLogout(context: Context)
}
