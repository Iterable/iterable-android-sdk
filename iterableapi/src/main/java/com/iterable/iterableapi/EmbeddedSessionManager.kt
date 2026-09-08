package com.iterable.iterableapi

import java.util.Date

public class EmbeddedSessionManager {

    private val TAG = "EmbeddedSessionManager"

    // Callers reach this class from arbitrary threads (see issue #1052), so every access to
    // impressions, session, and the impression fields happens under this lock.
    private val lock = Any()

    private var impressions: MutableMap<String, EmbeddedImpressionData> = mutableMapOf()

    var session: IterableEmbeddedSession = IterableEmbeddedSession(
        null,
        null,
        null
    )
        get() = synchronized(lock) { field }
        set(value) = synchronized(lock) { field = value }

    fun isTracking(): Boolean {
        return synchronized(lock) { session.start != null }
    }

    fun startSession() {
        synchronized(lock) {
            if (isTracking()) {
                IterableLogger.e(TAG, "Embedded session started twice")
                return
            }

            session = IterableEmbeddedSession(
                Date(),
                null,
                null
            )
        }
    }

    fun endSession() {
        val sessionToTrack = synchronized(lock) {
            if (!isTracking()) {
                IterableLogger.e(TAG, "Embedded session ended without start")
                return
            }

            if (impressions.isEmpty()) {
                return
            }

            endAllImpressionsLocked()

            val tracked = IterableEmbeddedSession(
                session.start,
                Date(),
                getImpressionListLocked()
            )

            //reset session for next session start
            session = IterableEmbeddedSession(
                null,
                null,
                null
            )

            impressions = mutableMapOf()

            tracked
        }

        // Tracking calls into IterableApi, so it runs after the lock is released.
        IterableApi.getInstance().trackEmbeddedSession(sessionToTrack)
    }

    fun startImpression(messageId: String, placementId: Long) {
        synchronized(lock) {
            var impressionData: EmbeddedImpressionData? = impressions[messageId]

            if (impressionData == null) {
                impressionData = EmbeddedImpressionData(messageId, placementId)
                impressions[messageId] = impressionData
            }

            impressionData.start = Date()
        }
    }

    fun pauseImpression(messageId: String) {
        synchronized(lock) {
            val impressionData: EmbeddedImpressionData? = impressions[messageId]

            if (impressionData == null) {
                IterableLogger.e(TAG, "onMessageImpressionEnded: impressionData not found")
                return
            }

            if (impressionData.start == null) {
                IterableLogger.e(TAG, "onMessageImpressionEnded: impressionStarted is null")
                return
            }

            updateDisplayCountAndDurationLocked(impressionData)
        }
    }

    // The Locked suffix marks helpers that read or write impressions without taking the lock
    // themselves: every caller must already hold it.
    private fun endAllImpressionsLocked() {
        for (impressionData in impressions.values) {
            updateDisplayCountAndDurationLocked(impressionData)
        }
    }

    private fun getImpressionListLocked(): List<IterableEmbeddedImpression>? {
        val impressionList: MutableList<IterableEmbeddedImpression> = ArrayList()
        for (impressionData in impressions.values) {
            impressionList.add(
                IterableEmbeddedImpression(
                    impressionData.messageId,
                    impressionData.placementId,
                    impressionData.displayCount,
                    impressionData.duration
                )
            )
        }
        return impressionList
    }

    private fun updateDisplayCountAndDurationLocked(impressionData: EmbeddedImpressionData): EmbeddedImpressionData {
        val start = impressionData.start
        if (start != null) {
            impressionData.displayCount = impressionData.displayCount.plus(1)
            impressionData.duration =
                impressionData.duration.plus((Date().time - start.time) / 1000.0)
                    .toFloat()
            impressionData.start = null
        }
        return impressionData
    }
}
