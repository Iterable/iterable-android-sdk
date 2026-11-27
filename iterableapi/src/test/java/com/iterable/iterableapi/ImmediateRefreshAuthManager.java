package com.iterable.iterableapi;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ImmediateRefreshAuthManager extends IterableAuthManager {
    static final String FRESH_TOKEN = "fresh-token";

    final List<IterableAuthRefreshReason> refreshReasons = Collections.synchronizedList(new ArrayList<>());

    ImmediateRefreshAuthManager(IterableApi api) {
        super(api, null, new RetryPolicy(1, 0, RetryPolicy.Type.LINEAR), 60_000);
    }

    @Override
    synchronized void scheduleAuthTokenRefresh(
            long timeDuration,
            IterableAuthRefreshReason reason,
            IterableHelper.SuccessHandler successCallback
    ) {
        refreshReasons.add(reason);
        if (successCallback == null) {
            return;
        }
        try {
            successCallback.onSuccess(new JSONObject().put("newAuthToken", FRESH_TOKEN));
        } catch (JSONException e) {
            throw new AssertionError(e);
        }
    }
}
