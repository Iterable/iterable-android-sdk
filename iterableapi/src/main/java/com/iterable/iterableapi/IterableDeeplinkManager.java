package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class IterableDeeplinkManager {

    private static final Pattern DEEP_LINK_PATTERN =
            Pattern.compile(IterableConstants.ITBL_DEEPLINK_IDENTIFIER);

    /**
     * Tracks a link click and passes the redirected URL to the callback
     * @param url The URL that was clicked
     * @param callback The callback to execute the original URL is retrieved
     */
    static void getAndTrackDeepLink(@Nullable String url, @NonNull IterableHelper.IterableActionHandler callback) {
        if (url == null) {
            callback.execute(null);
            return;
        }

        if (!IterableUtil.isUrlOpenAllowed(url)) {
            return;
        }

        if (!isIterableDeeplink(url)) {
            callback.execute(url);
            return;
        }

        IterableDeeplinkRedirectTask redirectTask =
                new IterableDeeplinkRedirectTask(
                        url,
                        callback,
                        new IterableDeeplinkRedirectResolver(),
                        IterableExecutors.main()
                );
        IterableExecutors.deepLink().execute(redirectTask);
    }

    /**
     * Checks if the URL looks like a link rewritten by Iterable
     * @param url The URL to check
     * @return `true` if it looks like a link rewritten by Iterable, `false` otherwise
     */
    static boolean isIterableDeeplink(@Nullable String url) {
        if (url != null) {
            Matcher m = DEEP_LINK_PATTERN.matcher(url);
            if (m.find()) {
                return true;
            }
        }
        return false;
    }
}
