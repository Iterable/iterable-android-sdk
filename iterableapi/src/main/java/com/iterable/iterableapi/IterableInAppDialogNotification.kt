package com.iterable.iterableapi

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.annotation.RestrictTo
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference

/**
 * Dialog-based In-App notification for [androidx.activity.ComponentActivity] (Compose) support
 *
 * This class provides the same functionality as [IterableInAppFragmentHTMLNotification]
 * but works with [androidx.activity.ComponentActivity] instead of requiring [androidx.fragment.app.FragmentActivity].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IterableInAppDialogNotification internal constructor(
    hostActivity: Activity,
    private val htmlString: String?,
    private val callbackOnCancel: Boolean,
    private val message: IterableInAppMessage,
    private val backgroundAlpha: Double,
    private val insetPadding: Rect,
    private val shouldAnimate: Boolean,
    private val inAppBackgroundAlpha: Double,
    private val inAppBackgroundColor: String?,
    private val layoutService: InAppLayoutService = InAppServices.layout,
    private val animationService: InAppAnimationService = InAppServices.animation,
    private val trackingService: InAppTrackingService = InAppServices.tracking,
    private val webViewService: InAppWebViewService = InAppServices.webView,
    private val orientationService: InAppOrientationService = InAppServices.orientation
) : Dialog(hostActivity), IterableWebView.HTMLNotificationCallbacks {

    private var webView: IterableWebView? = null
    private var loaded: Boolean = false
    private var orientationListener: OrientationEventListener? = null
    private var inAppOpenTracked: Boolean = false
    private var dismissed: Boolean = false

    private var prepareToShowRunnable: Runnable? = null
    private var dismissRunnable: Runnable? = null

    // Mirrors IterableInAppFragmentHTMLNotification's debounced resize plumbing.
    private var resizeHandler: Handler? = null
    private var pendingResizeRunnable: Runnable? = null
    private var lastContentHeight: Float = -1f

    // Dismisses the dialog and clears its singleton if the host activity is torn down
    // before the in-app does it itself (e.g. user backs out of the activity, finish(),
    // rotation while the dialog is up).
    private val hostLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            processMessageRemoval()
            dismiss()
        }
    }

    companion object {
        private const val TAG = "IterableInAppDialog"
        private const val BACK_BUTTON = "itbl://backButton"
        private const val DELAY_THRESHOLD_MS = 500L
        private const val DISMISS_DELAY_MS = 400L
        private const val RESIZE_DEBOUNCE_DELAY_MS = 200L
        private const val RESIZE_HEIGHT_EPSILON = 1.0f

        // WeakReference so a stale singleton can never pin its host Activity in memory.
        // The lifecycle observer attached in createInstance is the primary teardown path
        // (clears this field on host destroy); the WeakReference is belt-and-suspenders
        // against any path that might bypass dismiss() — lint flags any Activity-typed
        // class held in a static field as a leak vector even when we believe it's
        // unreachable.
        @Volatile
        @JvmStatic
        private var notificationRef: WeakReference<IterableInAppDialogNotification>? = null

        @Volatile
        @JvmStatic
        private var clickCallback: IterableHelper.IterableUrlCallback? = null

        @Volatile
        @JvmStatic
        private var location: IterableInAppLocation? = null

        @JvmStatic
        @JvmOverloads
        fun createInstance(
            activity: Activity,
            htmlString: String,
            callbackOnCancel: Boolean,
            urlCallback: IterableHelper.IterableUrlCallback,
            inAppLocation: IterableInAppLocation,
            message: IterableInAppMessage,
            backgroundAlpha: Double,
            padding: Rect,
            animate: Boolean = false,
            inAppBgColor: IterableInAppMessage.InAppBgColor =
                IterableInAppMessage.InAppBgColor(null, 0.0),
        ): IterableInAppDialogNotification? {
            val existing = notificationRef?.get()
            if (existing != null) {
                IterableLogger.w(
                    TAG,
                    "createInstance called while another dialog is showing; " +
                        "returning existing instance without overwriting callbacks"
                )
                return existing
            }

            val lifecycle = (activity as? LifecycleOwner)?.lifecycle
            if (lifecycle == null) {
                IterableLogger.w(
                    TAG,
                    "Host activity is not a LifecycleOwner; refusing to show dialog in-app. " +
                        "Use a ComponentActivity or FragmentActivity host."
                )
                return null
            }
            if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
                IterableLogger.w(
                    TAG,
                    "Host activity is already destroyed; refusing to show dialog in-app"
                )
                return null
            }

            val newInstance = IterableInAppDialogNotification(
                activity,
                htmlString,
                callbackOnCancel,
                message,
                backgroundAlpha,
                padding,
                animate,
                inAppBgColor.bgAlpha,
                inAppBgColor.bgHexColor,
                InAppServices.layout,
                InAppServices.animation,
                InAppServices.tracking,
                InAppServices.webView,
                InAppServices.orientation
            )

            lifecycle.addObserver(newInstance.hostLifecycleObserver)

            clickCallback = urlCallback
            location = inAppLocation
            notificationRef = WeakReference(newInstance)

            return newInstance
        }

        /**
         * Returns the notification instance currently being shown
         *
         * @return notification instance
         */
        @JvmStatic
        fun getInstance(): IterableInAppDialogNotification? = notificationRef?.get()
    }

    override fun onStart() {
        super.onStart()

        window?.let { layoutService.setWindowToFullScreen(it) }
        
        val layout = layoutService.getInAppLayout(insetPadding)
        if (layout != InAppLayoutService.InAppLayout.FULLSCREEN) {
            window?.let { layoutService.applyWindowGravity(it, insetPadding, "onStart") }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val layout = layoutService.getInAppLayout(insetPadding)
        window?.let { layoutService.configureWindowFlags(it, layout) }

        if (layout != InAppLayoutService.InAppLayout.FULLSCREEN) {
            window?.let { layoutService.applyWindowGravity(it, insetPadding, "onCreate") }
        }

        // TODO: tap-outside cancel doesn't fire trackInAppClose / processMessageRemoval —
        // same gap exists in the fragment version, we should fix it together
        setOnCancelListener {
            if (callbackOnCancel && clickCallback != null) {
                clickCallback?.execute(null)
            }
        }

        setupBackPressHandling()

        val contentView = createContentView()
        setContentView(contentView)

        setupOrientationListener()

        if (!inAppOpenTracked) {
            trackingService.trackInAppOpen(message, location)
            inAppOpenTracked = true
        }

        prepareToShowWebView()
    }

    override fun dismiss() {
        if (dismissed) {
            return
        }
        dismissed = true

        prepareToShowRunnable?.let { webView?.removeCallbacks(it) }
        dismissRunnable?.let { webView?.removeCallbacks(it) }
        pendingResizeRunnable?.let { resizeHandler?.removeCallbacks(it) }
        prepareToShowRunnable = null
        dismissRunnable = null
        pendingResizeRunnable = null
        resizeHandler = null

        (context as? LifecycleOwner)?.lifecycle?.removeObserver(hostLifecycleObserver)

        orientationService.disableListener(orientationListener)
        orientationListener = null

        webViewService.cleanupWebView(webView)
        webView = null

        // Always clear statics. Unlike DialogFragment, Dialog is not recreated
        // after configuration changes, so a stale reference would permanently
        // block isShowingInApp() and prevent future in-app messages.
        notificationRef = null
        clickCallback = null
        location = null

        super.dismiss()
    }

    private fun setupBackPressHandling() {
        setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                trackingService.trackInAppClick(message, BACK_BUTTON, location)
                trackingService.trackInAppClose(
                    message,
                    BACK_BUTTON,
                    IterableInAppCloseAction.BACK,
                    location
                )

                processMessageRemoval()
                hideWebView()
                true
            } else {
                false
            }
        }
    }

    private fun createContentView(): View {
        val context = context
        
        webView = webViewService.createConfiguredWebView(
            context,
            this@IterableInAppDialogNotification,
            htmlString ?: ""
        )
        
        val frameLayout = FrameLayout(context)
        val layout = layoutService.getInAppLayout(insetPadding)
        val isFullScreen = layout == InAppLayoutService.InAppLayout.FULLSCREEN
        
        if (isFullScreen) {
            val params = webViewService.createWebViewLayoutParams(true)
            frameLayout.addView(webView, params)
        } else {
            val webViewContainer = RelativeLayout(context)
            
            val containerParams = webViewService.createContainerLayoutParams(layout)
            
            val webViewParams = webViewService.createCenteredWebViewParams()
            
            webViewContainer.addView(webView, webViewParams)
            frameLayout.addView(webViewContainer, containerParams)
            
            ViewCompat.setOnApplyWindowInsetsListener(frameLayout) { v, insets ->
                val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(0, sysBars.top, 0, sysBars.bottom)
                insets
            }
        }
        
        return frameLayout
    }

    private fun setupOrientationListener() {
        orientationListener = orientationService.createOrientationListener(context) {
            if (loaded && webView != null) {
                webViewService.runResizeScript(webView)
            }
        }
        orientationService.enableListener(orientationListener)
    }

    private fun prepareToShowWebView() {
        try {
            webView?.let { animationService.prepareViewForDisplay(it) }
            val runnable = Runnable {
                prepareToShowRunnable = null
                if (!dismissed && window != null) {
                    showInAppBackground()
                    showAndAnimateWebView()
                }
            }
            prepareToShowRunnable = runnable
            webView?.postDelayed(runnable, DELAY_THRESHOLD_MS)
        } catch (e: NullPointerException) {
            IterableLogger.e(TAG, "View not present. Failed to hide before resizing inapp", e)
        }
    }

    private fun showInAppBackground() {
        window?.let { w ->
            animationService.showInAppBackground(
                w,
                inAppBackgroundColor,
                inAppBackgroundAlpha,
                shouldAnimate
            )
        }
    }

    private fun showAndAnimateWebView() {
        webView?.let { wv ->
            val layout = layoutService.getInAppLayout(insetPadding)
            animationService.showAndAnimateWebView(wv, shouldAnimate, context, layout)
        }
    }

    override fun setLoaded(loaded: Boolean) {
        this.loaded = loaded
    }

    override fun runResizeScript() {
        val handler = resizeHandler ?: Handler(Looper.getMainLooper()).also { resizeHandler = it }
        pendingResizeRunnable?.let { handler.removeCallbacks(it) }

        val runnable = Runnable { performResizeWithValidation() }
        pendingResizeRunnable = runnable
        handler.postDelayed(runnable, RESIZE_DEBOUNCE_DELAY_MS)
    }

    private fun performResizeWithValidation() {
        val wv = webView
        if (wv == null) {
            IterableLogger.w(TAG, "WebView is null, skipping resize")
            return
        }

        val currentHeight = wv.contentHeight.toFloat()
        if (currentHeight <= 0f) {
            IterableLogger.w(TAG, "Invalid content height: ${currentHeight}dp, skipping resize")
            return
        }

        if (Math.abs(currentHeight - lastContentHeight) < RESIZE_HEIGHT_EPSILON) {
            IterableLogger.d(TAG, "Content height unchanged (${currentHeight}dp), skipping resize")
            return
        }

        lastContentHeight = currentHeight
        IterableLogger.d(TAG, "Resizing in-app to height: ${currentHeight}dp")
        resize(currentHeight)
    }

    /**
     * Resizes the dialog window/WebView to fit the rendered HTML content. Mirrors
     * IterableInAppFragmentHTMLNotification.resize(float).
     */
    private fun resize(height: Float) {
        (context as? Activity)?.runOnUiThread {
            try {
                if (dismissed || !isShowing) {
                    return@runOnUiThread
                }
                val win = window ?: return@runOnUiThread
                val wv = webView ?: return@runOnUiThread

                if (insetPadding.top == 0 && insetPadding.bottom == 0) {
                    // Fullscreen — just keep the window MATCH_PARENT, the WebView is
                    // already configured with MATCH_PARENT in createContentView().
                    win.setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT
                    )
                    return@runOnUiThread
                }

                val displayMetrics = context.resources.displayMetrics
                val newWebViewWidth = displayMetrics.widthPixels
                val newWebViewHeight = (height * displayMetrics.density).toInt()

                val webViewParams =
                    RelativeLayout.LayoutParams(newWebViewWidth, newWebViewHeight)

                when (layoutService.getVerticalLocation(insetPadding)) {
                    Gravity.CENTER_VERTICAL -> {
                        webViewParams.addRule(RelativeLayout.CENTER_IN_PARENT)
                    }
                    Gravity.TOP -> {
                        webViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                        webViewParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                    }
                    Gravity.BOTTOM -> {
                        webViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                        webViewParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                    }
                }

                // Make the dialog window fill the screen so the gravity-positioned
                // WebView lands in the right spot relative to the device viewport.
                win.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )

                wv.layoutParams = webViewParams
                wv.requestLayout()
                (wv.parent as? ViewGroup)?.requestLayout()

                IterableLogger.d(
                    TAG,
                    "Applied explicit size and positioning to WebView: " +
                        "${newWebViewWidth}x${newWebViewHeight}px"
                )
            } catch (e: IllegalArgumentException) {
                IterableLogger.e(TAG, "Exception while trying to resize an in-app message", e)
            }
        }
    }

    override fun onUrlClicked(url: String?) {
        url?.let {
            trackingService.trackInAppClick(message, it, location)
            trackingService.trackInAppClose(
                message,
                it,
                IterableInAppCloseAction.LINK,
                location
            )
            
            clickCallback?.execute(Uri.parse(it))
        }
        
        processMessageRemoval()
        hideWebView()

    }

    private fun hideWebView() {
        val wv = webView
        val win = window
        val layout = layoutService.getInAppLayout(insetPadding)

        if (shouldAnimate && wv != null) {
            animationService.hideAndAnimateWebView(wv, true, context, layout)

            if (win != null) {
                animationService.hideInAppBackground(
                    win,
                    inAppBackgroundColor,
                    inAppBackgroundAlpha,
                    true
                )
            }

            val runnable = Runnable {
                dismissRunnable = null
                dismiss()
            }
            dismissRunnable = runnable
            wv.postDelayed(runnable, DISMISS_DELAY_MS)
        } else {
            dismiss()
        }
    }

    private fun processMessageRemoval() {
        trackingService.removeMessage(message)
    }
}

