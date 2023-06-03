package com.iterable.iterableapi

import androidx.annotation.RestrictTo
import java.util.Date

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmbeddedSessionManager {

    private val TAG = "EmbeddedSessionManager"

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

        val sessionToTrack = IterableEmbeddedSession(
            session.start,
            Date(),
            "0",
            null
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

    }

    fun endImpression(messageId: String) {

    }
}