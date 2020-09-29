package com.iterable.iterableapi;

import android.util.Base64;

import androidx.annotation.VisibleForTesting;

import com.iterable.iterableapi.util.Future;

import org.json.JSONException;
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
    private boolean pendingAuth;
    private boolean requiresAuthRefresh;

    IterableAuthManager(IterableApi api, IterableAuthHandler authHandler, long authRefreshPeriod) {
        timer = new Timer(true);
        this.api = api;
        this.authHandler = authHandler;
        this.authRefreshPeriod = authRefreshPeriod;
    }

    public synchronized void requestNewAuthToken(boolean hasFailedPriorAuth) {
        if (authHandler != null) {
            if (!pendingAuth) {
                if (!(this.hasFailedPriorAuth && hasFailedPriorAuth)) {
                    this.hasFailedPriorAuth = hasFailedPriorAuth;
                    pendingAuth = true;
                    Future.runAsync(new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            return authHandler.onAuthTokenRequested();
                        }
                    }).onSuccess(new Future.SuccessCallback<String>() {
                        @Override
                        public void onSuccess(String authToken) {
                            if (authToken != null) {
                                queueExpirationRefresh(authToken);
                            }
                            IterableApi.getInstance().setAuthToken(authToken);
                            pendingAuth = false;
                            reSyncAuth();
                        }
                    })
                    .onFailure(new Future.FailureCallback() {
                        @Override
                        public void onFailure(Throwable throwable) {
                            IterableLogger.e(TAG, "Error while requesting Auth Token", throwable);
                            pendingAuth = false;
                            reSyncAuth();
                        }
                    });
                }
            } else if (!hasFailedPriorAuth) {
                //setFlag to resync auth after current auth returns
                requiresAuthRefresh = true;
            }

        } else {
            IterableApi.getInstance().setAuthToken(null);
        }
    }

    public void queueExpirationRefresh(String encodedJWT) {
        try {
            long expirationTimeSeconds = decodedExpiration(encodedJWT);
            long triggerExpirationRefreshTime = expirationTimeSeconds * 1000L - authRefreshPeriod - IterableUtil.currentTimeMillis();
            if (triggerExpirationRefreshTime > 0) {
                scheduleAuthTokenRefresh(triggerExpirationRefreshTime);
            }
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while parsing JWT for the expiration: " + encodedJWT, e);
        }
    }

    void resetFailedAuth() {
        hasFailedPriorAuth = false;
    }

    void reSyncAuth() {
        if (requiresAuthRefresh) {
            requiresAuthRefresh = false;
            requestNewAuthToken(false);
        }
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

    private static long decodedExpiration(String encodedJWT) throws Exception {
        long exp = 0;
            String[] split = encodedJWT.split("\\.");
            String body = getJson(split[1]);
            JSONObject jObj = new JSONObject(body);
            exp = jObj.getLong(expirationString);
        return exp;
    }

    private static String getJson(String strEncoded) throws UnsupportedEncodingException {
        byte[] decodedBytes = Base64.decode(strEncoded, Base64.URL_SAFE);
        return new String(decodedBytes, "UTF-8");
    }
}

