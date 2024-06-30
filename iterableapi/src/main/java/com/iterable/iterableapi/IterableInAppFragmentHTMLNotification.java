package com.iterable.iterableapi;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.DialogFragment;

public class IterableInAppFragmentHTMLNotification extends DialogFragment implements IterableWebView.HTMLNotificationCallbacks {
    private static final String BACK_BUTTON = "itbl://backButton";
    private static final String TAG = "IterableInAppFragmentHTMLNotification";
    private static final String HTML_STRING = "HTML";
    private static final String BACKGROUND_ALPHA = "BackgroundAlpha";
    private static final String INSET_PADDING = "InsetPadding";
    private static final String CALLBACK_ON_CANCEL = "CallbackOnCancel";
    private static final String MESSAGE_ID = "MessageId";
    private static final String IN_APP_OPEN_TRACKED = "InAppOpenTracked";
    private static final String IN_APP_BG_ALPHA = "InAppBgAlpha";
    private static final String IN_APP_BG_COLOR = "InAppBgColor";
    private static final String IN_APP_SHOULD_ANIMATE = "ShouldAnimate";

    private static final int DELAY_THRESHOLD_MS = 500;

    @Nullable static IterableInAppFragmentHTMLNotification notification;
    @Nullable static IterableHelper.IterableUrlCallback clickCallback;
    @Nullable static IterableInAppLocation location;

    private IterableWebView webView;
    private boolean loaded;
    private OrientationEventListener orientationListener;
    private boolean callbackOnCancel = false;
    private String htmlString;
    private String messageId;

    private double backgroundAlpha; //TODO: remove in a future version
    private Rect insetPadding;
    private boolean shouldAnimate;
    private double inAppBackgroundAlpha;
    private String inAppBackgroundColor;

    public static IterableInAppFragmentHTMLNotification createInstance(@NonNull String htmlString, boolean callbackOnCancel, @NonNull IterableHelper.IterableUrlCallback clickCallback, @NonNull IterableInAppLocation location, @NonNull String messageId, @NonNull Double backgroundAlpha, @NonNull Rect padding) {
        return IterableInAppFragmentHTMLNotification.createInstance(htmlString, callbackOnCancel, clickCallback, location, messageId, backgroundAlpha, padding, false, new IterableInAppMessage.InAppBgColor(null, 0.0f));
    }

    public static IterableInAppFragmentHTMLNotification createInstance(@NonNull String htmlString, boolean callbackOnCancel, @NonNull IterableHelper.IterableUrlCallback clickCallback, @NonNull IterableInAppLocation location, @NonNull String messageId, @NonNull Double backgroundAlpha, @NonNull Rect padding, @NonNull boolean shouldAnimate, IterableInAppMessage.InAppBgColor inAppBgColor) {
        notification = new IterableInAppFragmentHTMLNotification();
        Bundle args = new Bundle();
        args.putString(HTML_STRING, htmlString);
        args.putBoolean(CALLBACK_ON_CANCEL, callbackOnCancel);
        args.putString(MESSAGE_ID, messageId);
        args.putDouble(BACKGROUND_ALPHA, backgroundAlpha);
        args.putParcelable(INSET_PADDING, padding);
        args.putString(IN_APP_BG_COLOR, inAppBgColor.bgHexColor);
        args.putDouble(IN_APP_BG_ALPHA, inAppBgColor.bgAlpha);
        args.putBoolean(IN_APP_SHOULD_ANIMATE, shouldAnimate);

        IterableInAppFragmentHTMLNotification.clickCallback = clickCallback;
        IterableInAppFragmentHTMLNotification.location = location;
        notification.setArguments(args);
        return notification;
    }

    /**
     * Returns the notification instance currently being shown
     * @return notification instance
     */
    public static IterableInAppFragmentHTMLNotification getInstance() {
        return notification;
    }

    /**
     * HTML In-App Notification
     */
    public IterableInAppFragmentHTMLNotification() {
        this.loaded = false;
        this.backgroundAlpha = 0;
        this.messageId = "";
        insetPadding = new Rect();
        this.setStyle(DialogFragment.STYLE_NO_FRAME, androidx.appcompat.R.style.Theme_AppCompat_NoActionBar);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        if (args != null) {
            htmlString = args.getString(HTML_STRING, null);
            callbackOnCancel = args.getBoolean(CALLBACK_ON_CANCEL, false);
            messageId = args.getString(MESSAGE_ID);
            backgroundAlpha = args.getDouble(BACKGROUND_ALPHA);
            insetPadding = args.getParcelable(INSET_PADDING);
            inAppBackgroundAlpha = args.getDouble(IN_APP_BG_ALPHA);
            inAppBackgroundColor = args.getString(IN_APP_BG_COLOR, null);
            shouldAnimate = args.getBoolean(IN_APP_SHOULD_ANIMATE);
        }

        notification = this;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                IterableInAppFragmentHTMLNotification.this.onBackPressed();
                hideWebView();
            }
        };
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (callbackOnCancel && clickCallback != null) {
                    clickCallback.execute(null);
                }
            }
        });
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getInAppLayout(insetPadding) == InAppLayout.FULLSCREEN) {
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else if (getInAppLayout(insetPadding) != InAppLayout.TOP) {
            // For TOP layout in-app, status bar will be opaque so that the in-app content does not overlap with translucent status bar.
            // For other non-fullscreen in-apps layouts (BOTTOM and CENTER), status bar will be translucent
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (getInAppLayout(insetPadding) == InAppLayout.FULLSCREEN) {
            getDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        webView = new IterableWebView(getContext());
        webView.setId(R.id.webView);
        webView.createWithHtml(this, htmlString);

        webView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                runResizeScript();
                return true;
            }
        });

        if (orientationListener == null) {
            orientationListener = new OrientationEventListener(getContext(), SensorManager.SENSOR_DELAY_NORMAL) {
                // Resize the webView on device rotation
                public void onOrientationChanged(int orientation) {
                    if (loaded) {
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                runResizeScript();
                            }
                        }, 1000);
                    }
                }
            };
        }

        orientationListener.enable();

        RelativeLayout relativeLayout = new RelativeLayout(this.getContext());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        relativeLayout.setVerticalGravity(getVerticalLocation(insetPadding));
        relativeLayout.addView(webView, layoutParams);

        if (savedInstanceState == null || !savedInstanceState.getBoolean(IN_APP_OPEN_TRACKED, false)) {
            IterableApi.sharedInstance.trackInAppOpen(messageId, location);
        }

        prepareToShowWebView();
        return relativeLayout;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    /**
     * Sets up the webView and the dialog layout
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IN_APP_OPEN_TRACKED, true);
    }

    /**
     * On Stop of the dialog
     */
    @Override
    public void onStop() {
        if (orientationListener != null) {
            orientationListener.disable();
        }

        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (this.getActivity() != null && this.getActivity().isChangingConfigurations()) {
            return;
        }

        notification = null;
        clickCallback = null;
        location = null;
    }

    @Override
    public void onUrlClicked(String url) {
        IterableApi.sharedInstance.trackInAppClick(messageId, url, location);
        IterableApi.sharedInstance.trackInAppClose(messageId, url, IterableInAppCloseAction.LINK, location);

        if (clickCallback != null) {
            clickCallback.execute(Uri.parse(url));
        }

        processMessageRemoval();
        hideWebView();
    }

    /**
     * Tracks a button click when the back button is pressed
     */
    public void onBackPressed() {
        IterableApi.sharedInstance.trackInAppClick(messageId, BACK_BUTTON);
        IterableApi.sharedInstance.trackInAppClose(messageId, BACK_BUTTON, IterableInAppCloseAction.BACK, location);

        processMessageRemoval();
    }

    private void prepareToShowWebView() {
        try {
            webView.setAlpha(0.0f);
            webView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (getContext() != null && getDialog() != null && getDialog().getWindow() != null) {
                        showInAppBackground();
                        showAndAnimateWebView();
                    }
                }
            }, DELAY_THRESHOLD_MS);
        } catch (NullPointerException e) {
            IterableLogger.e(TAG, "View not present. Failed to hide before resizing inapp");
        }
    }

    private void showInAppBackground() {
        animateBackground(new ColorDrawable(Color.TRANSPARENT), getInAppBackgroundDrawable());
    }

    private void hideInAppBackground() {
        animateBackground(getInAppBackgroundDrawable(), new ColorDrawable(Color.TRANSPARENT));
    }

    private void animateBackground(Drawable from, Drawable to) {
        if (from == null || to == null) {
            return;
        }

        if (getDialog() == null || getDialog().getWindow() == null) {
            IterableLogger.e(TAG, "Dialog or Window not present. Skipping background animation");
            return;
        }

        Drawable[] layers = new Drawable[2];
        layers[0] = from;
        layers[1] = to;
        TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
        transitionDrawable.setCrossFadeEnabled(true);
        getDialog().getWindow().setBackgroundDrawable(transitionDrawable);
        transitionDrawable.startTransition(IterableConstants.ITERABLE_IN_APP_BACKGROUND_ANIMATION_DURATION);
    }

    private ColorDrawable getInAppBackgroundDrawable() {
        if (inAppBackgroundColor == null) {
            IterableLogger.d(TAG, "Background Color does not exist. In App background animation will not be performed");
            return null;
        }

        int backgroundColorWithAlpha;

        try {
            backgroundColorWithAlpha = ColorUtils.setAlphaComponent(Color.parseColor(inAppBackgroundColor), (int) (inAppBackgroundAlpha * 255));
        } catch (IllegalArgumentException e) {
            IterableLogger.e(TAG, "Background color could not be identified for input string \"" + inAppBackgroundColor + "\". Failed to load in-app background.");
            return null;
        }

        ColorDrawable backgroundColorDrawable = new ColorDrawable(backgroundColorWithAlpha);
        return backgroundColorDrawable;
    }

    private void showAndAnimateWebView() {
        webView.setAlpha(1.0f);
        webView.setVisibility(View.VISIBLE);

        if (shouldAnimate) {
            int animationResource;
            InAppLayout inAppLayout = getInAppLayout(insetPadding);
            switch (inAppLayout) {
                case TOP:
                    animationResource = R.anim.slide_down_custom;
                    break;
                case CENTER:
                case FULLSCREEN:
                    animationResource = R.anim.fade_in_custom;
                    break;
                case BOTTOM:
                    animationResource = R.anim.slide_up_custom;
                    break;
                default:
                    animationResource = R.anim.fade_in_custom;
            }
            try {
                Animation anim = AnimationUtils.loadAnimation(getContext(), animationResource);
                anim.setDuration(IterableConstants.ITERABLE_IN_APP_ANIMATION_DURATION);
                webView.startAnimation(anim);
            } catch (Exception e) {
                IterableLogger.e(TAG, "Failed to show inapp with animation");
            }
        }
    }

    private void hideWebView() {
        if (shouldAnimate) {
            int animationResource;
            InAppLayout inAppLayout = getInAppLayout(insetPadding);

            switch (inAppLayout) {
                case TOP:
                    animationResource = R.anim.top_exit;
                    break;
                case CENTER:
                case FULLSCREEN:
                    animationResource = R.anim.fade_out_custom;
                    break;
                case BOTTOM:
                    animationResource = R.anim.bottom_exit;
                    break;
                default:
                    animationResource = R.anim.fade_out_custom;
            }

            try {
                Animation anim = AnimationUtils.loadAnimation(getContext(),
                        animationResource);
                anim.setDuration(IterableConstants.ITERABLE_IN_APP_ANIMATION_DURATION);
                webView.startAnimation(anim);
            } catch (Exception e) {
                IterableLogger.e(TAG, "Failed to hide inapp with animation");
            }

        }

        hideInAppBackground();
        Runnable dismissWebViewRunnable = new Runnable() {
            @Override
            public void run() {
                if (getContext() != null && getDialog() != null && getDialog().getWindow() != null) {
                    dismissAllowingStateLoss();
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webView.postOnAnimationDelayed(dismissWebViewRunnable, 400);
        } else {
            webView.postDelayed(dismissWebViewRunnable, 400);
        }
    }

    private void processMessageRemoval() {
        IterableInAppMessage message = IterableApi.sharedInstance.getInAppManager().getMessageById(messageId);
        if (message == null) {
            IterableLogger.e(TAG, "Message with id " + messageId + " does not exist");
            return;
        }

        if (message.isMarkedForDeletion() && !message.isConsumed()) {
            IterableApi.sharedInstance.getInAppManager().removeMessage(message, null, null);
        }
    }

    @Override
    public void runResizeScript() {
        resize(webView.getContentHeight());
    }

    /**
     * Resizes the dialog window based upon the size of its webView HTML content
     * @param height
     */
    public void resize(final float height) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Since this is run asynchronously, notification might've been dismissed already
                    if (getContext() == null || notification == null || notification.getDialog() == null ||
                            notification.getDialog().getWindow() == null || !notification.getDialog().isShowing()) {
                        return;
                    }

                    DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
                    Window window = notification.getDialog().getWindow();
                    Rect insetPadding = notification.insetPadding;

                    WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                    Display display = wm.getDefaultDisplay();
                    Point size = new Point();

                    // Get the correct screen size based on api level
                    // https://stackoverflow.com/questions/35780980/getting-the-actual-screen-height-android
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        display.getRealSize(size);
                    } else {
                        display.getSize(size);
                    }

                    int webViewWidth = size.x;
                    int webViewHeight = size.y;

                    //Check if the dialog is full screen
                    if (insetPadding.bottom == 0 && insetPadding.top == 0) {
                        //Handle full screen
                        window.setLayout(webViewWidth, webViewHeight);
                        getDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    } else {
                        float relativeHeight = height * getResources().getDisplayMetrics().density;
                        RelativeLayout.LayoutParams webViewLayout = new RelativeLayout.LayoutParams(getResources().getDisplayMetrics().widthPixels, (int) relativeHeight);
                        webView.setLayoutParams(webViewLayout);
                    }
                } catch (IllegalArgumentException e) {
                    IterableLogger.e(TAG, "Exception while trying to resize an in-app message", e);
                }
            }
        });
    }

    /**
     * Returns the vertical position of the dialog for the given padding
     * @param padding
     * @return
     */
    int getVerticalLocation(Rect padding) {
        int gravity = Gravity.CENTER_VERTICAL;
        if (padding.top == 0 && padding.bottom < 0) {
            gravity = Gravity.TOP;
        } else if (padding.top < 0 && padding.bottom == 0) {
            gravity = Gravity.BOTTOM;
        }
        return gravity;
    }

    InAppLayout getInAppLayout(Rect padding) {
        if (padding.top == 0 && padding.bottom == 0) {
            return InAppLayout.FULLSCREEN;
        } else if (padding.top == 0 && padding.bottom < 0) {
            return InAppLayout.TOP;
        } else if (padding.top < 0 && padding.bottom == 0) {
            return InAppLayout.BOTTOM;
        } else {
            return InAppLayout.CENTER;
        }
    }
}

enum InAppLayout {
    TOP,
    BOTTOM,
    CENTER,
    FULLSCREEN
}
