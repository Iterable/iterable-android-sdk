package com.iterable.iterableapi

import androidx.annotation.RestrictTo
import java.util.Date
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class IterableEmbeddedSession(
    val start: Date? = null,
    val end: Date? = null,
    val impressions: List<IterableEmbeddedImpression>? = null,
    val id: String = UUID.randomUUID().toString()
) {
    // For java compatibility
    constructor(
        start: Date?,
        end: Date?,
        impressions: List<IterableEmbeddedImpression>?
    ) : this(start, end, impressions, UUID.randomUUID().toString())
}

class IterableEmbeddedImpression(
    val messageId: String,
    val placementId: Long,
    val displayCount: Int,
    val duration: Float
)