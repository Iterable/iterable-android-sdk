package com.iterable.iterableapi

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object InAppServices {
    val layout: InAppLayoutService = InAppLayoutService()
    val animation: InAppAnimationService = InAppAnimationService()
    val tracking: InAppTrackingService by lazy { InAppTrackingService(IterableApi.sharedInstance) }
    val webView: InAppWebViewService = InAppWebViewService()
    val orientation: InAppOrientationService = InAppOrientationService()
    val displayMode: InAppDisplayModeService = InAppDisplayModeService()
}

