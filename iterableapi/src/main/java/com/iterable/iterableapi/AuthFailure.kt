package com.iterable.iterableapi

/**
 * Represents an auth failure object.
 */
class AuthFailure(
    /** userId or email of the signed-in user */
    val userKey: String,
    /** the authToken which caused the failure */
    val failedAuthToken: String,
    /** the timestamp of the failed request */
    val failedRequestTime: Long,
    /** indicates a reason for failure */
    val failureReason: AuthFailureReason
)