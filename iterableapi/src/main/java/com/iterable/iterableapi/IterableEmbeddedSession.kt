package com.iterable.iterableapi

import androidx.annotation.RestrictTo
import java.util.Date
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class IterableEmbeddedSession(
    val sessionStartTime: Date,
    val sessionEndTime: Date,
    val placementId: String?,
    val impressions: List<IterableEmbeddedImpression>,
    val embeddedSessionId: String
) {
    constructor(
        sessionStartTime: Date,
        sessionEndTime: Date,
        placementId: String,
        impressions: List<IterableEmbeddedImpression>
    ) : this(sessionStartTime, sessionEndTime, placementId, impressions, UUID.randomUUID().toString())
}

class IterableEmbeddedImpression(
    val messageId: String,
    val displayCount: Int,
    val duration: Float
)