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
    public void onLoadResource(WebView view, String url) {
        runResizeScript(view);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        inAppHTMLNotification.setLoaded(true);
        runResizeScript(view);
    }

    public void runResizeScript(WebView view) {
        view.getSettings().setJavaScriptEnabled(true);
        view.loadUrl("javascript:ITBL.resize(document.body.getBoundingClientRect().height)");
        view.getSettings().setJavaScriptEnabled(false);
    }
}