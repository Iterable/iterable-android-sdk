package com.iterable.iterableapi;

import android.webkit.WebView;
import android.webkit.WebViewClient;

class IterableWebViewClient extends WebViewClient {
    IterableWebView.HTMLNotificationCallbacks inAppHTMLNotification;

    IterableWebViewClient(IterableWebView.HTMLNotificationCallbacks inAppHTMLNotification) {
        this.inAppHTMLNotification = inAppHTMLNotification;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        inAppHTMLNotification.onUrlClicked(url);
        return true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        inAppHTMLNotification.setLoaded(true);
        view.postDelayed(inAppHTMLNotification::runResizeScript, 100);
    }
}