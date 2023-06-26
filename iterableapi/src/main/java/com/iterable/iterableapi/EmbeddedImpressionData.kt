package com.iterable.iterableapi

import androidx.annotation.RestrictTo
import java.util.Date

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class EmbeddedImpressionData(
    val messageId: String,
    var displayCount: Int = 0,
    var duration: Double = 0.00,
    var start: Date? = null
) {
    constructor(
        messageId: String
    ) : this(messageId, 0, 0.00, null)
}