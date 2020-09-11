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

    private final IterableApi api;
    IterableAuthHandler authHandler;

    //For expiration handling
    private static final int refreshWindowTime = 60000; // 60 seconds
    private static final String expirationString = "exp";

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

    private void queueExpirationRefresh(String encodedJWT) {
        int expirationTimeSeconds = decodedExpiration(encodedJWT);
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
                            api.setAuthToken(authToken);
                        }
                    });
                }
            }, timeDuration);

            timer.purge();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    api.getAuthManager().requestNewAuthToken(false, new IterableHelper.SuccessAuthHandler() {
                        @Override
                        public void onSuccess(@NonNull String authToken) {
                            api.setAuthToken(authToken);
                        }
                    });
                }
            }, timeDuration);

        } catch (Exception e) {
            IterableLogger.e(TAG, "timer exception: " + timer, e);
        }
    }

    private static int decodedExpiration(String encodedJWT) {
        int exp = 0;
        try {
            String[] split = encodedJWT.split("\\.");
            String body = getJson(split[1]);
            JSONObject jObj = new JSONObject(body);
            exp = jObj.getInt(expirationString);
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

