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
            IterableLogger.w(TAG, "WebView render process gone. didCrash: " + didCrash);

            // Clean up the WebView to prevent further issues
            if (view != null) {
                try {
                    view.destroy();
                } catch (Exception e) {
                    IterableLogger.e(TAG, "Error destroying WebView after render process gone", e);
                }
            }

            // Return true to prevent the app from crashing
            return true;
        }
        return super.onRenderProcessGone(view, detail);
    }
}
