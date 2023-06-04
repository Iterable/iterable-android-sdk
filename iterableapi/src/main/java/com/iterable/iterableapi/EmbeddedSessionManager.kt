package com.iterable.iterableapi

import androidx.annotation.RestrictTo
import java.util.Date

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmbeddedSessionManager {

    private val TAG = "EmbeddedSessionManager"

    private var impressions: MutableMap<String, EmbeddedImpressionData> = mutableMapOf()

    var session: IterableEmbeddedSession = IterableEmbeddedSession(
        null,
        null,
        "0",
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
            "0",
            null
        )
    }

    fun endSession() {
        if (!isTracking()) {
            IterableLogger.e(TAG, "Embedded session ended without start")
            return
        }

        endAllImpressions()

        val sessionToTrack = IterableEmbeddedSession(
            session.start,
            Date(),
            "0",
            getImpressionList()
        )

        IterableApi.getInstance().trackEmbeddedSession(sessionToTrack)
        IterableLogger.d(TAG, "Embedded session ended!!")

        //reset session for next session start
        session = IterableEmbeddedSession(
            null,
            null,
            "0",
            null
        )
    }

    fun onMessageImpressionStarted(message: IterableEmbeddedMessage) {
        IterableLogger.printInfo()
        startImpression(message.metadata.id)
    }

    fun onMessageImpressionEnded(message: IterableEmbeddedMessage) {
        IterableLogger.printInfo()
        endImpression(message.metadata.id)
    }

    fun startImpression(messageId: String) {
        var impressionData: EmbeddedImpressionData? = impressions[messageId]

        if (impressionData == null) {
            impressionData = EmbeddedImpressionData(messageId)
            impressions[messageId] = impressionData
        }

        impressionData.startImpression()
    }

    fun endImpression(messageId: String) {
        val impressionData: EmbeddedImpressionData? = impressions[messageId]

        if (impressionData == null) {
            IterableLogger.e(TAG, "onMessageImpressionEnded: impressionData not found")
            return
        }

        if (impressionData.start == null) {
            IterableLogger.e(TAG, "onMessageImpressionEnded: impressionStarted is null")
            return
        }

        impressionData.endImpression()
    }

    private fun endAllImpressions() {
        for (impressionData in impressions.values) {
            impressionData.endImpression()
        }
    }

    private fun getImpressionList(): List<IterableEmbeddedImpression>? {
        val impressionList: MutableList<IterableEmbeddedImpression> = ArrayList()
        for (impressionData in impressions.values) {
            impressionList.add(
                IterableEmbeddedImpression(
                    impressionData.messageId,
                    impressionData.displayCount,
                    impressionData.duration
                )
            )
        }
        return impressionList
    }
}