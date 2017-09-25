package com.iterable.iterableapi;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by David Truong dt@iterable.com.
 */

public class IterableInAppHTMLNotification extends Dialog {

    static final String HTML_STRING = "html";

    final String mimeType = "text/html";
    final String encoding = "UTF-8";
    final String resizeScript = "javascript:ITBL.resize(document.body.getBoundingClientRect().height)";

    static IterableInAppHTMLNotification notification;

    Context context;
    IterableWebView webView;
    String htmlString;

    public static IterableInAppHTMLNotification instance(Context context, String htmlString)
    {
        if (notification == null) {
            notification = new IterableInAppHTMLNotification(context, htmlString);
        }

        //else update

        return notification;
    }

    public static IterableInAppHTMLNotification getInstance() {
        return notification;
    }

    private IterableInAppHTMLNotification(Context context, String htmlString) {
        super(context, android.R.style.Theme_NoTitleBar);
        this.context = context;
        this.htmlString = htmlString;
    }

    @Override
    protected void onStart() {
        super.onStart();

        this.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

//        webView = new WebView(context);
        webView = new IterableWebView(context);
        webView.createWithHtml(this, htmlString);
        webView.addJavascriptInterface(this, "ITBL");

        setContentView(webView);
    }

    @JavascriptInterface
    public void resize(final float height) {
        Activity ownerActivity = getOwnerActivity();
        getOwnerActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DisplayMetrics displayMetrics = getOwnerActivity().getResources().getDisplayMetrics();
                int webViewHeight = (int) displayMetrics.heightPixels;
                int webViewWidth = (int) displayMetrics.widthPixels;

                Window window = notification.getWindow();
                WindowManager.LayoutParams wlp = window.getAttributes();

                Rect rect = new Rect(1,2,3,4);


                if (true) {//bottom & top != auto)

                    //Configurable constants
                    float dimAmount = 0.5f;
                    float widthPercentage = .8f;
                    int gravity = Gravity.CENTER; //Gravity.TOP, Gravity.CENTER, Gravity.BOTTOM;

                    int maxHeight = Math.min((int) (height * displayMetrics.scaledDensity), webViewHeight);
                    int maxWidth = Math.min(webViewWidth, (int) (webViewWidth * widthPercentage));
                    window.setLayout(maxWidth, maxHeight);

                    wlp.gravity = gravity;
                    wlp.dimAmount = dimAmount;
                    wlp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                    window.setAttributes(wlp);
                } else { //bottom/top/left/right = 0; full screen
                    //Is this necessary
                    webView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            //disables scrolling for full screen
                            return (event.getAction() == MotionEvent.ACTION_MOVE);
                        }
                    });
                }
            }
        });
    }
}

class IterableWebView extends WebView {
    final String mimeType = "text/html";
    final String encoding = "UTF-8";

    IterableWebView(Context context) {
        super(context);
    }

    void createWithHtml(IterableInAppHTMLNotification notificationDialog, String html) {
        IterableWebViewClient webViewClient = new IterableWebViewClient(notificationDialog, new IterableInAppWebViewListener());
        loadDataWithBaseURL("", html, mimeType, encoding, "");
        setWebViewClient(webViewClient);

        //don't overscroll
        setOverScrollMode(WebView.OVER_SCROLL_NEVER);

        //transparent
        setBackgroundColor(Color.TRANSPARENT);

        //resize:
        getSettings().setJavaScriptEnabled(true);
    }
}

class IterableWebViewClient extends WebViewClient {
    IterableInAppHTMLNotification inAppHTMLNotification;
    IterableInAppWebViewListener listener;

    IterableWebViewClient(IterableInAppHTMLNotification inAppHTMLNotification, IterableInAppWebViewListener listener) {
        this.inAppHTMLNotification = inAppHTMLNotification;
        this.listener = listener;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView  view, String  url) {
        //TODO: handle the button click here
        System.out.println("urlClicked: "+ url);

        Uri uri = Uri.parse(url);
        String authority = uri.getAuthority();

        listener.close(inAppHTMLNotification);

        return true;
    }

    @Override
    public void onPageStarted (WebView view,
                               String url,
                               Bitmap favicon) {
        System.out.println("urlClicked: "+ url);
        view.addJavascriptInterface(inAppHTMLNotification, "ITBL");
        view.loadUrl("javascript:ITBL.resize(document.body.getBoundingClientRect().height)");
    }
}

class IterableInAppWebViewListener {
    public void close(IterableInAppHTMLNotification inApp) {
        inApp.dismiss();
    }
}
