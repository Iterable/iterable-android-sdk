package com.iterable.iterableapi;

import android.os.Build;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebView;
import android.webkit.WebViewClient;

class IterableWebViewClient extends WebViewClient {
    private static final String TAG = "IterableWebViewClient";

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

    @Override
    public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean didCrash = detail.didCrash();
            IterableLogger.e(TAG, "WebView render process gone. didCrash: " + didCrash);

            // Destroy the WebView to clean up its internal state
            if (view != null) {
                view.destroy();
            }

            // Return true to indicate we handled this, preventing the app from crashing
            return true;
        }
        return super.onRenderProcessGone(view, detail);
    }
}
