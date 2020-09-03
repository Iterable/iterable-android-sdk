package com.iterable.iterableapi;

import android.util.Base64;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class IterableAuthManager {
    private static final String TAG = "IterableAuth";

    private static final int refreshWindowSeconds = 60;
    private static final String expirationString = "exp";
    private String authToken;
    private boolean pendingAuth;

    private long lastTokenRequestTime;

    private int failedSequentialAuthRequestCount;

    IterableAuthManager()
    {
    }

    public static void queueExpirationRefresh(String JWTEncoded) {


        int expirationTime = decodedExpiration(JWTEncoded);
        int triggerRefreshTime = expirationTime - refreshWindowSeconds;

        //Schedule timer to trigger to refresh the auth token
            //cancel existing authRefreshTimer
            //call onAuthExpiration
    }

    public void requestNewAuthToken() {
        //What do we do if there's already a pending auth? Ignore since it will eventually fix itself
        if (pendingAuth == false) {
            long currentTime = IterableUtil.currentTimeMillis();

            if (currentTime - lastTokenRequestTime > 1000) {
                pendingAuth = true;

                //rate limit
                lastTokenRequestTime = IterableUtil.currentTimeMillis();

                failedSequentialAuthRequestCount++;
                //Blocking call on a separate thread
                String authToken = "";//onAuthTokenRequested();
                updateAuthToken(this.authToken);
            } else {
                //queue up another requestNewAuthToken at currentTime + 1 second
            }
        }
    }

    public void resetFailedAuthRequestCount() {
        failedSequentialAuthRequestCount = 0;
    }

    public void updateAuthToken(String authToken) {
        //TODO: What if the authToken is the same authToken as before?
        this.authToken = authToken;
        pendingAuth = false;

        queueExpirationRefresh(authToken);
    }

    public static int decodedExpiration(String JWTEncoded) {
        int exp = 0;
        try {
            String[] split = JWTEncoded.split("\\.");
            String body = getJson(split[1]);
            JSONObject jObj = new JSONObject(body);
            exp = jObj.getInt(expirationString);
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while parsing JWT for the expiration: " + JWTEncoded, e);
        }
        return exp;
    }

    private static String getJson(String strEncoded) throws UnsupportedEncodingException {
        byte[] decodedBytes = Base64.decode(strEncoded, Base64.URL_SAFE);
        return new String(decodedBytes, "UTF-8");
    }
}