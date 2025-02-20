package com.iterable.iterableapi;

public enum AuthFailureReason {
    /**
     * The auth token's expiration time (exp) is set too far in the future.
     * The expiration must be less than one year from the issued-at time (iat).
     */
    AUTH_TOKEN_EXPIRATION_INVALID,

    /**
     * The auth token has expired. The current time is past the token's
     * expiration time (exp claim).
     */
    AUTH_TOKEN_EXPIRED,

    /**
     * The auth token's format is invalid. The token failed basic JWT format validation
     * (three dot-separated base64-encoded sections).
     */
    AUTH_TOKEN_FORMAT_INVALID,

    /**
     * An error occurred while generating the auth token.
     * The onAuthTokenRequested callback threw an exception.
     */
    AUTH_TOKEN_GENERATION_ERROR,

    /**
     * A generic auth token error occurred that isn't covered by other specific error types.
     * This is a catch-all for unexpected authentication failures.
     */
    AUTH_TOKEN_GENERIC_ERROR,

    /**
     * The auth token has been explicitly invalidated by Iterable and can no longer be used.
     * A new token must be generated.
     */
    AUTH_TOKEN_INVALIDATED,

    /**
     * The auth token is null.
     * The onAuthTokenRequested callback returned null instead of a valid JWT token.
     */
    AUTH_TOKEN_NULL,

    /**
     * The auth token's payload is invalid or malformed.
     * Iterable could not decode or validate required claims (iat, exp, email, userId).
     */
    AUTH_TOKEN_PAYLOAD_INVALID,

    /**
     * The auth token's signature is invalid.
     * Iterable could not verify the token's authenticity using the project's JWT secret.
     */
    AUTH_TOKEN_SIGNATURE_INVALID,

    /**
     * The auth token contains invalid user identification.
     * Either:
     * - Both email and userId are missing from the token
     * - The provided email/userId doesn't match any user in the Iterable project
     */
    AUTH_TOKEN_USER_KEY_INVALID,

    /**
     * The auth token is missing from the API request.
     * No JWT authorization header was included in the request to Iterable's API.
     */
    AUTH_TOKEN_MISSING,
}
