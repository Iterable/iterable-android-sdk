package com.iterable.iterableapi

internal object InAppServices {
    val layout: InAppLayoutService = InAppLayoutService()
    val animation: InAppAnimationService = InAppAnimationService()
    val tracking: InAppTrackingService = InAppTrackingService(IterableApi.sharedInstance)
    val webView: InAppWebViewService = InAppWebViewService()
    val orientation: InAppOrientationService = InAppOrientationService()
}

