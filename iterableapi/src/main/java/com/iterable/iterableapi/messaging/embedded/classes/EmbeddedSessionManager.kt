package com.iterable.iterableapi.messaging.embedded.classes

import androidx.annotation.RestrictTo
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableLogger
import java.util.Date

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmbeddedSessionManager {

    private val TAG = "EmbeddedSessionManager"

    private var impressions: MutableMap<String, EmbeddedImpressionData> = mutableMapOf()

    var session: IterableEmbeddedSession = IterableEmbeddedSession(
        null,
        null,
        null
    )

    fun isTracking(): Boolean {
        return session.start != null
    }

    fun startSession() {
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

    fun endSession() {
        if (!isTracking()) {
            IterableLogger.e(TAG, "Embedded session ended without start")
            return
        }

        if(impressions.isNotEmpty()) {
            endAllImpressions()

            val sessionToTrack = IterableEmbeddedSession(
                session.start,
                Date(),
                getImpressionList()
            )

            IterableApi.getInstance().trackEmbeddedSession(sessionToTrack)

            //reset session for next session start
            session = IterableEmbeddedSession(
                null,
                null,
                null
            )

            impressions = mutableMapOf()
        }
    }

    fun startImpression(messageId: String, placementId: Long) {
        var impressionData: EmbeddedImpressionData? = impressions[messageId]

        if (impressionData == null) {
            impressionData = EmbeddedImpressionData(messageId, placementId)
            impressions[messageId] = impressionData
        }

        impressionData.start = Date()
    }

    fun pauseImpression(messageId: String) {
        val impressionData: EmbeddedImpressionData? = impressions[messageId]

        if (impressionData == null) {
            IterableLogger.e(TAG, "onMessageImpressionEnded: impressionData not found")
            return
        }

        if (impressionData.start == null) {
            IterableLogger.e(TAG, "onMessageImpressionEnded: impressionStarted is null")
            return
        }

        updateDisplayCountAndDuration(impressionData)
    }

    private fun endAllImpressions() {
        for (impressionData in impressions.values) {
            updateDisplayCountAndDuration(impressionData)
        }
    }

    private fun getImpressionList(): List<IterableEmbeddedImpression>? {
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

    private fun updateDisplayCountAndDuration(impressionData: EmbeddedImpressionData): EmbeddedImpressionData {
        if (impressionData.start != null) {
            impressionData.displayCount = impressionData.displayCount.plus(1)
            impressionData.duration =
                impressionData.duration.plus((Date().time - impressionData.start!!.time) / 1000.0)
                    .toFloat()
            impressionData.start = null
        }
        return impressionData
    }
}