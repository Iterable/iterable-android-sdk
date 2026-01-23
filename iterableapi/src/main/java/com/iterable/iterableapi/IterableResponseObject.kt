package com.iterable.iterableapi

import org.json.JSONObject

sealed class IterableResponseObject(
    val message: String,
    val code: IterableResponseCode
) {
    sealed class Success(
        message: String,
    ): IterableResponseObject(message, IterableResponseCode.SUCCESS)

    class GenericSuccess(
        message: String,
    ): Success(message)

    class RemoteSuccess(val responseJson: JSONObject): Success(
        message = SuccessMessages.REMOTE_SUCCESS
    )

    class AuthTokenSuccess(
        val authToken: String
    ): Success(
        message = SuccessMessages.AUTH_TOKEN_SUCCESS,
    )

    object LocalSuccess: Success(
        message = SuccessMessages.LOCAL_SUCCESS,
    )


    class Failure(remoteMessage: String): IterableResponseObject(
        message = remoteMessage,
        code = IterableResponseCode.FAILURE
    )

    companion object {
        @JvmField
        val LocalSuccessResponse = LocalSuccess
    }

    private object SuccessMessages {
        const val REMOTE_SUCCESS = "Successfully received response from remote API"
        const val AUTH_TOKEN_SUCCESS = "Successfully obtained authentication token"
        const val LOCAL_SUCCESS = "Operation completed locally without remote API call"
    }
}

enum class IterableResponseCode {
    SUCCESS, FAILURE
}