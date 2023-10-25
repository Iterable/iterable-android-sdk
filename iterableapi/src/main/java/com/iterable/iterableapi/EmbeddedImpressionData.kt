package com.iterable.iterableapi

import androidx.annotation.RestrictTo
import java.util.Date

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class EmbeddedImpressionData(
    val messageId: String,
    val placementId: String,
    var displayCount: Int = 0,
    var duration: Float = 0.0f,
    var start: Date? = null
) {
    constructor(
        messageId: String,
        placementId: String
    ) : this(messageId, placementId,0, 0.0f, null)
}