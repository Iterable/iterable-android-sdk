package com.iterable.iterableapi;

import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class IterableWebChromeClient extends WebChromeClient {
    IterableWebView.HTMLNotificationCallbacks inAppHTMLNotification;
    private boolean hasTriggeredResize = false;

    IterableWebChromeClient(IterableWebView.HTMLNotificationCallbacks inAppHTMLNotification) {
        this.inAppHTMLNotification = inAppHTMLNotification;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        // Only trigger resize once when page is fully loaded (100%)
        // Avoid triggering on subsequent 100% progress reports which can cause redraw loops
        if (newProgress == 100 && !hasTriggeredResize) {
            hasTriggeredResize = true;
            inAppHTMLNotification.runResizeScript();
        }
    }
}
