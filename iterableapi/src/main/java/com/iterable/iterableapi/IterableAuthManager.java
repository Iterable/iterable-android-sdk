package com.iterable.iterableapi;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

public class IterableAuthManager {
    private static final String TAG = "IterableAuth";
    //For expiration handling
    private static final int refreshWindowTime = 60000; // 60 seconds
    private static final String expirationString = "exp";

    private final IterableApi api;
    private final IterableAuthHandler authHandler;

    @VisibleForTesting
    Timer timer;
    private boolean hasFailedPriorAuth;

    IterableAuthManager(IterableApi api, IterableAuthHandler authHandler) {
        timer = new Timer(true);
        this.api = api;
        this.authHandler = authHandler;
    }

    public void requestNewAuthToken(boolean hasFailedPriorAuth, final @Nullable IterableHelper.SuccessAuthHandler successHandler) {
        if (!this.hasFailedPriorAuth || !hasFailedPriorAuth) {
            this.hasFailedPriorAuth = hasFailedPriorAuth;
            if (authHandler != null) {
                String authToken = authHandler.onAuthTokenRequested();
                if (authToken != null ) {
                    queueExpirationRefresh(authToken);
                }
                successHandler.onSuccess(authToken);
            } else {
                successHandler.onSuccess(null);
            }
        }
    }

    public void queueExpirationRefresh(String encodedJWT) {
        long expirationTimeSeconds = decodedExpiration(encodedJWT);
        long triggerExpirationRefreshTime = expirationTimeSeconds * 1000L - refreshWindowTime - IterableUtil.currentTimeMillis();
        if (triggerExpirationRefreshTime > 0) {
            scheduleAuthTokenRefresh(triggerExpirationRefreshTime);
        }
    }

    private void scheduleAuthTokenRefresh(long timeDuration) {
        timer.cancel();
        timer = new Timer(true);
        try {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    api.getAuthManager().requestNewAuthToken(false, new IterableHelper.SuccessAuthHandler() {
                        @Override
                        public void onSuccess(@NonNull String authToken) {
                            api.onSetAuthToken(authToken);
                        }
                    });
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

