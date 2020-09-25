package com.iterable.iterableapi;

import android.util.Base64;

import androidx.annotation.VisibleForTesting;

import com.iterable.iterableapi.util.Future;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

public class IterableAuthManager {
    private static final String TAG = "IterableAuth";
    private static final String expirationString = "exp";

    private final IterableApi api;
    private final IterableAuthHandler authHandler;
    private final long authRefreshPeriod;

    @VisibleForTesting
    Timer timer;
    private boolean hasFailedPriorAuth;

    IterableAuthManager(IterableApi api, IterableAuthHandler authHandler, long authRefreshPeriod) {
        timer = new Timer(true);
        this.api = api;
        this.authHandler = authHandler;
        this.authRefreshPeriod = authRefreshPeriod;
    }

    public void requestNewAuthToken(boolean hasFailedPriorAuth) {
        if (!this.hasFailedPriorAuth || !hasFailedPriorAuth) {
            this.hasFailedPriorAuth = hasFailedPriorAuth;
            if (authHandler != null) {
                Future.runAsync(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return authHandler.onAuthTokenRequested();
                    }
                })
                .onSuccess(new Future.SuccessCallback<String>() {
                    @Override
                    public void onSuccess(String authToken) {
                        if (authToken != null ) {
                            queueExpirationRefresh(authToken);
                        }
                        IterableApi.getInstance().setAuthToken(authToken);
                    }
                })
                .onFailure(new Future.FailureCallback() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        IterableLogger.e(TAG, "Error while requesting Auth Token", throwable);
                    }
                });
            } else {
                IterableApi.getInstance().setAuthToken(null);
            }
        }
    }

    public void queueExpirationRefresh(String encodedJWT) {
        long expirationTimeSeconds = decodedExpiration(encodedJWT);
        long triggerExpirationRefreshTime = expirationTimeSeconds * 1000L - authRefreshPeriod - IterableUtil.currentTimeMillis();
        if (triggerExpirationRefreshTime > 0) {
            scheduleAuthTokenRefresh(triggerExpirationRefreshTime);
        }
    }

    void resetFailedAuth() {
        hasFailedPriorAuth = false;
    }

    private void scheduleAuthTokenRefresh(long timeDuration) {
        timer.cancel();
        timer = new Timer(true);
        try {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    api.getAuthManager().requestNewAuthToken(false);
                }
            }, timeDuration);
        } catch (Exception e) {
            IterableLogger.e(TAG, "timer exception: " + timer, e);
        }
    }

    private static long decodedExpiration(String encodedJWT) {
        long exp = 0;
        try {
            String[] split = encodedJWT.split("\\.");
            String body = getJson(split[1]);
            JSONObject jObj = new JSONObject(body);
            exp = jObj.getLong(expirationString);
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while parsing JWT for the expiration: " + encodedJWT, e);
        }
        return exp;
    }

    private static String getJson(String strEncoded) throws UnsupportedEncodingException {
        byte[] decodedBytes = Base64.decode(strEncoded, Base64.URL_SAFE);
        return new String(decodedBytes, "UTF-8");
    }
}

