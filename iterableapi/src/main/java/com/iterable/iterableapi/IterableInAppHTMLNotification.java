package com.iterable.iterableapi;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

/**
 * Created by David Truong dt@iterable.com.
 */

public class IterableInAppHTMLNotification extends Dialog {

    static IterableInAppHTMLNotification notification;

    Context context;
    IterableWebView webView;
    Boolean loaded;
    OrientationEventListener orientationListener;

    String htmlString;
    String messageId;
    double backgroundAlpha;
    Rect insetPadding;
    IterableHelper.IterableActionHandler clickCallback;

    public static IterableInAppHTMLNotification createInstance(Context context, String htmlString) {
        notification = new IterableInAppHTMLNotification(context, htmlString);
        return notification;
    }

    public static IterableInAppHTMLNotification getInstance() {
        return notification;
    }

    private IterableInAppHTMLNotification(Context context, String htmlString) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);

        this.context = context;
        this.htmlString = htmlString;
        this.loaded = false;
        this.backgroundAlpha = 0;
        this.messageId = ""; // should this be null?
        insetPadding = new Rect();
    }

    public void setTrackParams(String messageId) {
        this.messageId = messageId;
    }

    public void setCallback(IterableHelper.IterableActionHandler clickCallback) {
        this.clickCallback = clickCallback;
    }

    public void setLoaded(Boolean loaded) {
        this.loaded = loaded;
    }

    public void setBackgroundAlpha(double backgroundAlpha) {
        this.backgroundAlpha = backgroundAlpha;
    }

    public void setPadding(Rect insetPadding) {
        this.insetPadding = insetPadding;
    }

    @Override
    protected void onStart() {
        super.onStart();

        this.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        webView = new IterableWebView(context);
        webView.createWithHtml(this, htmlString);
        webView.addJavascriptInterface(this, "ITBL");

        if (orientationListener == null) {
            orientationListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
                public void onOrientationChanged(int orientation) {
                    System.out.print("changed Orientation");
                    // Re-layout the webview dialog.
                    // Figure out how to do this on an activity handler (rotation complete)
                    if (loaded) {
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadUrl(IterableWebViewClient.resizeScript);
                            }
                        }, 1000);
                    }
                }
            };
        }
        orientationListener.enable();

        RelativeLayout relativeLayout = new RelativeLayout(this.getContext());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);

        relativeLayout.addView(webView,layoutParams);
        setContentView(relativeLayout,layoutParams);

        IterableApi.sharedInstance.trackInAppOpen(messageId);
    }

    @Override
    protected void onStop() {
        orientationListener.disable();
    }

    @JavascriptInterface
    public void resize(final float height) {
        getOwnerActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DisplayMetrics displayMetrics = getOwnerActivity().getResources().getDisplayMetrics();
//                int webViewHeight = (int) displayMetrics.heightPixels;
//                int webViewWidth = (int) displayMetrics.widthPixels;

                Window window = notification.getWindow();

                Rect insetPadding = notification.insetPadding;

//                Rect rectangle = new Rect();
//                window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
//                int windowWidth = rectangle.width();
//                int windowHeight = rectangle.height();

                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();

                Point size = new Point();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    display.getRealSize(size);
                } else {
                    display.getSize(size);
                }
                int webViewWidth = size.x;
                int webViewHeight = size.y;

                int orientation = getOwnerActivity().getResources().getConfiguration().orientation;

//                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
//                    if (windowHeight < windowWidth) {

//                        webViewHeight = webViewHeightSwap;//(int) displayMetrics.widthPixels;
//                        webViewWidth = webViewWidthSwap;//(int) displayMetrics.heightPixels;

//                    } else {
//
//                    }
//                } else {
//                    if (windowHeight > windowWidth) {
//                        webViewHeight = webViewHeightSwap;//(int) displayMetrics.widthPixels;
//                        webViewWidth = webViewWidthSwap;//(int) displayMetrics.heightPixels;
//                    } else {
//
//                    }
//                }
                //2464 , 1800
                //2560, 1656 window height
                //1800, 2465 display metrics

                //2560, 1800 expected

                int location = getLocation(insetPadding);
                if (insetPadding.bottom == 0 && insetPadding.top == 0) {
                    window.setLayout(webViewWidth, webViewHeight);

                    WindowManager.LayoutParams wlp = window.getAttributes();
                    wlp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);


                    //TODO: Is this necessary?
//                    webView.setOnTouchListener(new View.OnTouchListener() {
//                        @Override
//                        public boolean onTouch(View v, MotionEvent event) {
//                            //disables scrolling for full screen
//                            return (event.getAction() == MotionEvent.ACTION_MOVE);
//                        }
//                    });

                } else {
                    //Configurable constants
                    float dimAmount = 0.5f;
                    float widthPercentage = 1f;

                    //should this be here as well?
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);


                    int gravity = getLocation(insetPadding);
                    int maxHeight = Math.min((int) (height * displayMetrics.scaledDensity), webViewHeight);
                    int maxWidth = Math.min(webViewWidth, (int) (webViewWidth * widthPercentage));
                    window.setLayout(maxWidth, maxHeight);

                    WindowManager.LayoutParams wlp = window.getAttributes();
                    wlp.gravity = gravity;
                    wlp.dimAmount = dimAmount;
                    wlp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;

                    window.setAttributes(wlp);
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
        setBackgroundColor(Color.GREEN);

        //Fixes the webView to be the size of the screen
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setUseWideViewPort(true);

        //resize:
        getSettings().setJavaScriptEnabled(true);
    }
}

class IterableWebViewClient extends WebViewClient {
    static final String resizeScript = "javascript:ITBL.resize(document.body.getBoundingClientRect().height)";

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
        inAppHTMLNotification.setLoaded(true);
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
