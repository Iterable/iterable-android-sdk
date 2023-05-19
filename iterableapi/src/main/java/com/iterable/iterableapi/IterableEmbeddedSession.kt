package com.iterable.iterableapi

import androidx.annotation.RestrictTo
import java.util.Date
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class IterableEmbeddedSession(
    val sessionStartTime: Date?,
    val sessionEndTime: Date?,
    val impressions: List<IterableEmbeddedImpression>?,
    val sessionId: String = UUID.randomUUID().toString()
)

class IterableEmbeddedImpression(
    val messageId: String,
    val displayCount: Int,
    val duration: Float
)