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
        runResizeScript(view);
    }

    public void runResizeScript(WebView view) {
        view.getSettings().setJavaScriptEnabled(true);
        view.loadUrl("javascript:ITBL.resize(document.body.getBoundingClientRect().height)");
        view.getSettings().setJavaScriptEnabled(false);
    }
}
