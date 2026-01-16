package com.iterable.iterableapi

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.RelativeLayout

internal class InAppWebViewService {

    fun createConfiguredWebView(
        context: Context,
        callbacks: IterableWebView.HTMLNotificationCallbacks,
        htmlContent: String
    ): IterableWebView {
        val webView = IterableWebView(context)
        webView.id = R.id.webView
        webView.createWithHtml(callbacks, htmlContent)

        IterableLogger.d(TAG, "Created and configured WebView with HTML content")
        return webView
    }

    fun createWebViewLayoutParams(isFullScreen: Boolean): FrameLayout.LayoutParams {
        return if (isFullScreen) {
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        } else {
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    fun createCenteredWebViewParams(): RelativeLayout.LayoutParams {
        val params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.CENTER_IN_PARENT)
        return params
    }

    fun createContainerLayoutParams(layout: InAppLayoutService.InAppLayout): FrameLayout.LayoutParams {
        val params = when (layout) {
            InAppLayoutService.InAppLayout.TOP -> {
                val p = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                p.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                p
            }
            InAppLayoutService.InAppLayout.BOTTOM -> {
                val p = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                p.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                p
            }
            InAppLayoutService.InAppLayout.CENTER -> {
                val p = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                p.gravity = Gravity.CENTER
                p
            }
            InAppLayoutService.InAppLayout.FULLSCREEN -> {
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        }

        return params
    }

    fun cleanupWebView(webView: IterableWebView?) {
        if (webView != null) {
            try {
                webView.destroy()
                IterableLogger.d(TAG, "WebView cleaned up and destroyed")
            } catch (e: Exception) {
                IterableLogger.w(TAG, "Error cleaning up WebView", e)
            }
        }
    }

    fun runResizeScript(webView: IterableWebView?) {
        if (webView != null) {
            try {
                webView.evaluateJavascript("window.resize()", null)
                IterableLogger.d(TAG, "Triggered WebView resize script")
            } catch (e: Exception) {
                IterableLogger.w(TAG, "Error running resize script", e)
            }
        }
    }

    companion object {
        private const val TAG = "InAppWebViewService"
    }
}

