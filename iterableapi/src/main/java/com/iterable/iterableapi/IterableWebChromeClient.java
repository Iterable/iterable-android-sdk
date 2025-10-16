package com.iterable.iterableapi;

import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class IterableWebChromeClient extends WebChromeClient {
    IterableWebView.HTMLNotificationCallbacks inAppHTMLNotification;

    IterableWebChromeClient(IterableWebView.HTMLNotificationCallbacks inAppHTMLNotification) {
        this.inAppHTMLNotification = inAppHTMLNotification;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        // Only trigger resize when page is fully loaded (100%) to avoid multiple rapid calls
        if (newProgress == 100) {
            inAppHTMLNotification.runResizeScript();
        }
    }
}
