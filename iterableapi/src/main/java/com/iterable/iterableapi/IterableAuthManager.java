package com.iterable.iterableapi;

import android.util.Base64;

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

    //For rate limiting
    private boolean pendingAuth;
    private long lastTokenRequestTime;
    private long rateLimitTime = 1000;
    @VisibleForTesting
    Timer timer;

    private int failedSequentialAuthRequestCount;

    IterableAuthManager(IterableApi api, IterableAuthHandler authHandler) {
        timer = new Timer(true);
        this.api = api;
        this.authHandler = authHandler;
    }

    public void requestNewAuthToken() {
        //What do we do if there's already a pending auth? Ignore since it will eventually fix itself
        //I think the pending auth will help since push pushRegistration and getMessages happen sequentially.
        if (!pendingAuth) {
            long currentTime = IterableUtil.currentTimeMillis();
            if (currentTime - lastTokenRequestTime >= rateLimitTime) {
                pendingAuth = true;

                failedSequentialAuthRequestCount++;

                //TODO: Make this a Blocking call on a separate thread
                String authToken = null;
                if (authHandler != null) {
                    authToken = authHandler.onAuthTokenRequested();
                }
                updateAuthToken(authToken);
            } else {
                //Rate Limiter: queue up another requestNewAuthToken at currentTime + rateLimitTime
                scheduleAuthTokenRefresh(lastTokenRequestTime + rateLimitTime);
            }
            lastTokenRequestTime = currentTime;
        }
    }

    public void resetFailedSequentialAuthRequestCount() {
        failedSequentialAuthRequestCount = 0;
    }

    public int getFailedSequentialAuthRequestCount() {
        return failedSequentialAuthRequestCount;
    }

    private void updateAuthToken(String authToken) {
       if (authToken == null) {
           api.setAuthToken(authToken);
       } else if (!authToken.equals(api.getAuthToken())) {
           api.setAuthToken(authToken);
            pendingAuth = false;

            //re-register for push and in-apps
           if (api.config.autoPushRegistration) {
               api.registerForPush();
           }
           api.getInAppManager().syncInApp();

            queueExpirationRefresh(authToken);
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
        timer.purge();
        try {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    requestNewAuthToken();
                }
            }, 0, timeDuration);
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

