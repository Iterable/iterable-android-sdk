package com.iterable.iterableapi

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.OrientationEventListener
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Dialog-based In-App notification for [androidx.activity.ComponentActivity] (Compose) support
 * 
 * This class provides the same functionality as [IterableInAppFragmentHTMLNotification]
 * but works with [androidx.activity.ComponentActivity] instead of requiring [androidx.fragment.app.FragmentActivity].
 */
class IterableInAppDialogNotification internal constructor(
    activity: Activity,
    private val htmlString: String?,
    private val callbackOnCancel: Boolean,
    private val messageId: String,
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
) : Dialog(activity), IterableWebView.HTMLNotificationCallbacks {

    private var webView: IterableWebView? = null
    private var loaded: Boolean = false
    private var orientationListener: OrientationEventListener? = null
    private var backPressedCallback: OnBackPressedCallback? = null

    companion object {
        private const val TAG = "IterableInAppDialog"
        private const val BACK_BUTTON = "itbl://backButton"
        private const val DELAY_THRESHOLD_MS = 500L

        @JvmStatic
        private var notification: IterableInAppDialogNotification? = null

        @JvmStatic
        private var clickCallback: IterableHelper.IterableUrlCallback? = null

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
            messageId: String,
            backgroundAlpha: Double,
            padding: Rect,
            animate: Boolean = false,
            inAppBgColor: IterableInAppMessage.InAppBgColor =
                IterableInAppMessage.InAppBgColor(null, 0.0),
        ): IterableInAppDialogNotification {
            clickCallback = urlCallback
            location = inAppLocation
            
            notification = IterableInAppDialogNotification(
                activity,
                htmlString,
                callbackOnCancel,
                messageId,
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
            
            return notification!!
        }

        /**
         * Returns the notification instance currently being shown
         *
         * @return notification instance
         */
        @JvmStatic
        fun getInstance(): IterableInAppDialogNotification? = notification
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
        
        setOnCancelListener {
            if (callbackOnCancel && clickCallback != null) {
                clickCallback?.execute(null)
            }
        }
        
        setupBackPressHandling()
        
        val contentView = createContentView()
        setContentView(contentView)
        
        setupOrientationListener()
        
        trackingService.trackInAppOpen(messageId, location)
        
        prepareToShowWebView()
    }

    override fun dismiss() {
        backPressedCallback?.remove()
        backPressedCallback = null
        
        orientationService.disableListener(orientationListener)
        orientationListener = null
        
        webViewService.cleanupWebView(webView)
        webView = null
        
        notification = null
        
        super.dismiss()
    }

    private fun setupBackPressHandling() {
        val activity = ownerActivity ?: context as? ComponentActivity
        
        if (activity is ComponentActivity) {
            backPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    trackingService.trackInAppClick(messageId, BACK_BUTTON, location)
                    trackingService.trackInAppClose(
                        messageId,
                        BACK_BUTTON,
                        IterableInAppCloseAction.BACK,
                        location
                    )
                    
                    processMessageRemoval()
                    
                    dismiss()
                }
            }
            
            activity.onBackPressedDispatcher.addCallback(activity, backPressedCallback!!)
            IterableLogger.d(TAG, "dialog notification back press handler registered")
        } else {
            IterableLogger.w(TAG, "Activity is not ComponentActivity, using legacy back press handling")
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    trackingService.trackInAppClick(messageId, BACK_BUTTON, location)
                    trackingService.trackInAppClose(
                        messageId,
                        BACK_BUTTON,
                        IterableInAppCloseAction.BACK,
                        location
                    )
                    
                    processMessageRemoval()
                    
                    dismiss()
                    true
                } else {
                    false
                }
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
            webView?.postDelayed({
                if (context != null && window != null) {
                    showInAppBackground()
                    showAndAnimateWebView()
                }
            }, DELAY_THRESHOLD_MS)
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
            animationService.showAndAnimateWebView(wv, shouldAnimate, context)
        }
    }

    override fun setLoaded(loaded: Boolean) {
        this.loaded = loaded
    }

    override fun runResizeScript() {
        webViewService.runResizeScript(webView)
    }

    override fun onUrlClicked(url: String?) {
        url?.let {
            trackingService.trackInAppClick(messageId, it, location)
            trackingService.trackInAppClose(
                messageId,
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
        dismiss()
    }

    private fun processMessageRemoval() {
        trackingService.removeMessage(messageId, location)
    }
}

