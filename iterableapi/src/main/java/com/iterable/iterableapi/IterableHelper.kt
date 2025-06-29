package com.iterable.iterableapi

import android.net.Uri
import androidx.annotation.NonNull
import androidx.annotation.Nullable

import org.json.JSONObject

/**
 * Created by David Truong dt@iterable.com
 */
class IterableHelper {

    /**
     * Interface to handle Iterable Actions
     */
    interface IterableActionHandler {
        fun execute(data: String?)
    }

    interface IterableUrlCallback {
        fun execute(url: Uri?)
    }

    interface SuccessHandler {
        fun onSuccess(@NonNull data: JSONObject)
    }

    interface FailureHandler {
        fun onFailure(@NonNull reason: String, data: JSONObject?)
    }

    interface SuccessAuthHandler {
        fun onSuccess(@NonNull authToken: String)
    }
}