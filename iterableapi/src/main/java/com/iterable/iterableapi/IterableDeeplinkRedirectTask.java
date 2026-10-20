package com.iterable.iterableapi;

import androidx.annotation.Nullable;

import java.util.concurrent.Executor;

final class IterableDeeplinkRedirectTask implements Runnable {
    private final String url;
    @Nullable
    private final IterableHelper.IterableActionHandler callback;
    private final IterableDeeplinkRedirectResolver redirectResolver;
    private final Executor callbackExecutor;

    IterableDeeplinkRedirectTask(
            String url,
            @Nullable IterableHelper.IterableActionHandler callback,
            IterableDeeplinkRedirectResolver redirectResolver,
            Executor callbackExecutor
    ) {
        this.url = url;
        this.callback = callback;
        this.redirectResolver = redirectResolver;
        this.callbackExecutor = callbackExecutor;
    }

    @Override
    public void run() {
        IterableDeeplinkRedirectResult result = redirectResolver.resolve(url);
        callbackExecutor.execute(() -> deliver(result));
    }

    private void deliver(IterableDeeplinkRedirectResult result) {
        if (callback != null) {
            callback.execute(result.url);
        }

        if (result.campaignId != 0) {
            IterableAttributionInfo attributionInfo = new IterableAttributionInfo(
                    result.campaignId,
                    result.templateId,
                    result.messageId
            );
            IterableApi.sharedInstance.setAttributionInfo(attributionInfo);
        }
    }
}
