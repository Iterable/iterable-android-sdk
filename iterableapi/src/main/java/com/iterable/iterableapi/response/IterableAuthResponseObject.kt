package com.iterable.iterableapi.response

sealed class IterableAuthResponseObject(
    message: String,
    code: IterableResponseCode
): IterableResponseObject(message, code) {

    class Success(
        val authToken: String
    ): IterableResponseObject.Success(
        message = SuccessMessages.AUTH_TOKEN_SUCCESS,
    )


}