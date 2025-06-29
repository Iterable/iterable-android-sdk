package com.iterable.iterableapi

import androidx.annotation.RestrictTo
import java.util.Date
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class IterableEmbeddedSession(
    val start: Date?,
    val end: Date?,
    val impressions: List<IterableEmbeddedImpression>?,
    val id: String
) {
    constructor(
        start: Date?,
        end: Date?,
        impressions: List<IterableEmbeddedImpression>?
    ) : this(start, end, impressions, UUID.randomUUID().toString())
    
    fun getStart(): Date? = start
    fun getEnd(): Date? = end
    fun getId(): String? = id
    fun getImpressions(): List<IterableEmbeddedImpression>? = impressions
}

class IterableEmbeddedImpression(
    val messageId: String,
    val placementId: Long,
    val displayCount: Int,
    val duration: Float
) {
    fun getMessageId(): String = messageId
    fun getPlacementId(): Long = placementId
    fun getDisplayCount(): Int = displayCount
    fun getDuration(): Float = duration
}