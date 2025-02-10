package com.iterable.iterableapi.auth.interfaces;

import com.iterable.iterableapi.auth.classes.AuthFailure;

public interface IterableAuthHandler {
    String onAuthTokenRequested();
    void onTokenRegistrationSuccessful(String authToken);
    void onAuthFailure(AuthFailure authFailure);
}
