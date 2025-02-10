package com.iterable.iterableapi.auth.classes;

import com.iterable.iterableapi.auth.enums.AuthFailureReason;

/**
 * Represents an auth failure object.
 */
public class AuthFailure {

    /** userId or email of the signed-in user */
    public final String userKey;

    /** the authToken which caused the failure */
    public final String failedAuthToken;

    /** the timestamp of the failed request */
    public final long failedRequestTime;

    /** indicates a reason for failure */
    public final AuthFailureReason failureReason;

    public AuthFailure(String userKey,
                       String failedAuthToken,
                       long failedRequestTime,
                       AuthFailureReason failureReason) {
        this.userKey = userKey;
        this.failedAuthToken = failedAuthToken;
        this.failedRequestTime = failedRequestTime;
        this.failureReason = failureReason;
    }
}