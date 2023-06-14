package com.iterable.iterableapi

import androidx.annotation.RestrictTo
import java.util.Date
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class IterableEmbeddedSession(
    val start: Date?,
    val end: Date?,
    val placementId: String?,
    val impressions: List<IterableEmbeddedImpression>?,
    val id: String
) {
    constructor(
        start: Date?,
        end: Date?,
        placementId: String,
        impressions: List<IterableEmbeddedImpression>?
    ) : this(start, end, placementId, impressions, UUID.randomUUID().toString())
}

class IterableEmbeddedImpression(
    val messageId: String,
    val displayCount: Int,
    val duration: Float
)