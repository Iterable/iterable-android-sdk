package com.iterable.iterableapi;

public interface IterableAuthHandler {
    String onAuthTokenRequested();
    void onTokenRegistrationSuccessful(String authToken);
    void onAuthFailure(AuthFailure authFailure);
}
