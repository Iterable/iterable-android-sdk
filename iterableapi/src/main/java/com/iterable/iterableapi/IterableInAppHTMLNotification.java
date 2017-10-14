package com.iterable.iterableapi;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Handler;
import android.util.DisplayMetrics;
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
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.context = context;
        this.htmlString = htmlString;
    }

    public void setTrackParams(String messageId) {
        this.messageId = messageId;
    }

    public void setCallback(IterableHelper.IterableActionHandler clickCallback) {
        this.clickCallback = clickCallback;
    }

    public void setLoaded(Boolean loaded) {

    }

    public void setPadding(Rect insetPadding) {
        this.insetPadding = insetPadding;
    }

    @Override
    protected void onStart() {
        super.onStart();

        this.getWindow().setBackgroundDrawable(new ColorDrawable(Color.GREEN));
        webView = new IterableWebView(context);
        webView.createWithHtml(this, htmlString);
        webView.addJavascriptInterface(this, "ITBL");

        OrientationEventListener orientationListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            public void onOrientationChanged(int orientation) {
                System.out.print("changed Orientation");
                // Re-layout the webview dialog.
                // Figure out how to do this on an activity handler (rotation complete)
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl(IterableWebViewClient.resizeScript);
                    }
                }, 1000);
            }
        };
        orientationListener.enable();

        RelativeLayout relativeLayout = new RelativeLayout(this.getContext());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);

        //this.addView(relativeLayout, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        relativeLayout.addView(webView,layoutParams);
        setContentView(relativeLayout,layoutParams);
    }

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        //setContentView(R.layout.main);
//
//        //Add this
//        if(webView == null){
//            System.out.print("shouldn't get here");
////            WebSettings webSettings =webView.getSettings();
////            webSettings.setJavaScriptEnabled(true);
////            webView.setWebViewClient (new HelloWebViewClient());
////            webView.loadUrl("http://google.com");
//        }
//
//        if (savedInstanceState != null)
//            (webView).restoreState(savedInstanceState);
//    }

    @JavascriptInterface
    public void resize(final float height) {
        getOwnerActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DisplayMetrics displayMetrics = getOwnerActivity().getResources().getDisplayMetrics();
                int webViewHeight = (int) displayMetrics.heightPixels;
                int webViewWidth = (int) displayMetrics.widthPixels;

                Window window = notification.getWindow();

                Rect insetPadding = notification.insetPadding;

                Rect rectangle = new Rect();
                window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
//                int statusBarHeight = rectangle.top;
//                int statusBarl = rectangle.left;
//                int statusBarr = rectangle.right;
//                int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
//                int titleBarHeight= contentViewTop - statusBarHeight;

                int windowWidth = rectangle.width();
                int windowHeight = rectangle.height();

//                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//                Display display = wm.getDefaultDisplay();

//                Point size = new Point();
//                display.getSize(size);
//                int screenWidth = size.x;
//                int screenHeight = size.y;

                int orientation = getOwnerActivity().getResources().getConfiguration().orientation; //1 port / 2 land
//                int requestedOrientation = getOwnerActivity().getRequestedOrientation(); //-1
//                int rotation = display.getRotation(); //rotation from default?
//                if (rotation == Surface.ROTATION_0) {
//                    //figure out some rotations here
//                }
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    if (windowHeight < windowWidth) {
                        webViewHeight = (int) displayMetrics.widthPixels;
                        webViewWidth = (int) displayMetrics.heightPixels;
                    }
                } else {
                    if (windowHeight > windowWidth) {
                        webViewHeight = (int) displayMetrics.widthPixels;
                        webViewWidth = (int) displayMetrics.heightPixels;
                    }
                }

                int location = getLocation(insetPadding);
                if (insetPadding.bottom == 0 && insetPadding.top == 0) {
                    window.setLayout(webViewWidth, webViewHeight);

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
        setBackgroundColor(Color.TRANSPARENT);

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
