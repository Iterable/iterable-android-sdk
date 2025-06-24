package com.iterable.iterableapi

interface IterableAuthHandler {
    fun onAuthTokenRequested(): String?
    fun onTokenRegistrationSuccessful(authToken: String)
    fun onAuthFailure(authFailure: AuthFailure)
}
