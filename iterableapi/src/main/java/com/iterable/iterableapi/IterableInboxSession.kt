package com.iterable.iterableapi

import androidx.annotation.RestrictTo

import java.util.Date
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IterableInboxSession {
    val sessionStartTime: Date?
    val sessionEndTime: Date?
    val startTotalMessageCount: Int
    val startUnreadMessageCount: Int
    val endTotalMessageCount: Int
    val endUnreadMessageCount: Int
    val impressions: List<Impression>?
    val sessionId: String

    constructor(sessionStartTime: Date?, sessionEndTime: Date?, startTotalMessageCount: Int, startUnreadMessageCount: Int, endTotalMessageCount: Int, endUnreadMessageCount: Int, impressions: List<Impression>?) {
        this.sessionStartTime = sessionStartTime
        this.sessionEndTime = sessionEndTime
        this.startTotalMessageCount = startTotalMessageCount
        this.startUnreadMessageCount = startUnreadMessageCount
        this.endTotalMessageCount = endTotalMessageCount
        this.endUnreadMessageCount = endUnreadMessageCount
        this.impressions = impressions
        this.sessionId = UUID.randomUUID().toString()
    }

    constructor() {
        this.sessionStartTime = null
        this.sessionEndTime = null
        this.startTotalMessageCount = 0
        this.startUnreadMessageCount = 0
        this.endTotalMessageCount = 0
        this.endUnreadMessageCount = 0
        this.impressions = null
        this.sessionId = UUID.randomUUID().toString()
    }

    class Impression(
        val messageId: String,
        val silentInbox: Boolean,
        val displayCount: Int,
        val duration: Float
    )
}