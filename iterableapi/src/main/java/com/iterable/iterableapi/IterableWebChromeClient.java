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
        inAppHTMLNotification.resizeContent(view.getContentHeight());
    }
}
