package com.iterable.iterableapi;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by David Truong dt@iterable.com.
 */

public class IterableInAppHTMLNotification extends Dialog {

    static IterableInAppHTMLNotification notification;

    Context context;
    IterableWebView webView;
    String htmlString;

    String messageId;
    double backgroundAlpha = 0;
    IterableHelper.IterableActionHandler clickCallback;
    Rect insetPadding;


    public static IterableInAppHTMLNotification createInstance(Context context, String htmlString) {
        notification = new IterableInAppHTMLNotification(context, htmlString);
        return notification;
    }

    public static IterableInAppHTMLNotification getInstance() {
        return notification;
    }

    private IterableInAppHTMLNotification(Context context, String htmlString) {
        super(context, android.R.style.Theme_NoTitleBar_Fullscreen);
        this.context = context;
        this.htmlString = htmlString;
    }

    public void ITESetTrackParams(String messageId) {
        this.messageId = messageId;
    }

    public void ITESetCallback(IterableHelper.IterableActionHandler clickCallback) {
        this.clickCallback = clickCallback;
    }

    public void ITESetPadding(Rect insetPadding) {
        this.insetPadding = insetPadding;
    }

    @Override
    protected void onStart() {
        super.onStart();

        this.getWindow().setBackgroundDrawable(new ColorDrawable(Color.GREEN));
        webView = new IterableWebView(context);
        webView.createWithHtml(this, htmlString);
        webView.addJavascriptInterface(this, "ITBL");

        setContentView(webView);
    }

    @JavascriptInterface
    public void resize(final float height) {
        getOwnerActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DisplayMetrics displayMetrics = getOwnerActivity().getResources().getDisplayMetrics();
                int webViewHeight = (int) displayMetrics.heightPixels;
                int webViewWidth = (int) displayMetrics.widthPixels;

                Window window = notification.getWindow();

                Rect rect = notification.insetPadding;

                //Check if statusbar is present
                Rect rectangle = new Rect();
                window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
                int statusBarHeight = rectangle.top;
                int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
                int titleBarHeight= contentViewTop - statusBarHeight;

                if (true) {//bottom & top != auto)

                    //Configurable constants
                    float dimAmount = 0.5f;
                    float widthPercentage = 1f; // left and right
                    int gravity = Gravity.CENTER; //Gravity.TOP, Gravity.CENTER, Gravity.BOTTOM;

                    WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                    Display display = wm.getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int width = size.x;
                    int heightScreen = size.y;

                    int maxHeight = Math.min((int) (height * displayMetrics.scaledDensity), webViewHeight);
                    //int maxHeight = (int)(height * displayMetrics.scaledDensity);
                    int maxWidth = Math.min(webViewWidth, (int) (webViewWidth * widthPercentage));
                    window.setLayout(maxWidth, maxHeight);

                    WindowManager.LayoutParams wlp = window.getAttributes();
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

    int getLocation(Rect padding) {

//        if (_insetPadding.top == 0 && _insetPadding.bottom == 0) {
//            location = INAPP_FULL;
//        } else if (_insetPadding.top == 0 && _insetPadding.bottom < 0) {
//            location = INAPP_TOP;
//        } else if (_insetPadding.top < 0 && _insetPadding.bottom == 0) {
//            location = INAPP_BOTTOM;
//        } else if (_insetPadding.top < 0 && _insetPadding.bottom < 0) {
//            location = INAPP_MIDDLE;
//        }

        int gravity = Gravity.CENTER;
        if (padding.top  == 0 && padding.bottom < 0) {
            gravity = Gravity.TOP;
        } else if (padding.top < 0 && padding.bottom == 0) {
            gravity = Gravity.BOTTOM;
        }
        return gravity;
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
    final String resizeScript = "javascript:ITBL.resize(document.body.getBoundingClientRect().height)";

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

        listener.onClose(inAppHTMLNotification);

        return true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        view.loadUrl(resizeScript);
    }
}

class IterableInAppWebViewListener {
    public void onClose(IterableInAppHTMLNotification inApp) {
        inApp.dismiss();
    }

    public void onClick(IterableInAppHTMLNotification inApp) {

    }
}
