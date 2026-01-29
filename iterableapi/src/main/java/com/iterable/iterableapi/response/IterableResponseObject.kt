package com.iterable.iterableapi.response

import org.json.JSONObject

sealed class IterableResponseObject(
    val message: String,
    val code: IterableResponseCode
) {
    sealed class Success(
        message: String,
    ): IterableResponseObject(message, IterableResponseCode.SUCCESS)

    class RemoteSuccess(val responseJson: JSONObject): Success(
        message = SuccessMessages.REMOTE_SUCCESS
    )

    class LocalSuccess(localMessage: String = SuccessMessages.LOCAL_SUCCESS): Success(
        message = localMessage,
    )


    sealed class Failure(message: String): IterableResponseObject(
        message = message,
        code = IterableResponseCode.FAILURE
    )

    class RemoteFailure(remoteMessage: String, val errorCode: Int): Failure(
        message = remoteMessage
    )

    object SuccessMessages {
        const val REMOTE_SUCCESS = "Successfully received response from remote API"
        const val AUTH_TOKEN_SUCCESS = "Successfully obtained authentication token"
        const val LOCAL_SUCCESS = "Operation completed locally without remote API call"
    }
}

enum class IterableResponseCode {
    SUCCESS, FAILURE
}