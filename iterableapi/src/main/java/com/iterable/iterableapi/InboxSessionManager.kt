package com.iterable.iterableapi

import androidx.annotation.RestrictTo

import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.HashSet

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InboxSessionManager {
    companion object {
        private const val TAG = "InboxSessionManager"
    }

    internal var session = IterableInboxSession()
    private var impressions: MutableMap<String, ImpressionData> = HashMap()
    private var previousImpressions: MutableSet<String> = HashSet()

    fun isTracking(): Boolean {
        return session.sessionStartTime != null
    }

    fun startSession() {
        if (isTracking()) {
            IterableLogger.e(TAG, "Inbox session started twice")
            return
        }

        session = IterableInboxSession(
            Date(),
            null,
            IterableApi.getInstance().inAppManager?.inboxMessages?.size ?: 0,
            IterableApi.getInstance().inAppManager?.unreadInboxMessagesCount ?: 0,
            0,
            0,
            null
        )

        IterableApi.getInstance().setInboxSessionId(session.sessionId)
    }

    fun startSession(visibleRows: List<IterableInboxSession.Impression>) {
        startSession()
        updateVisibleRows(visibleRows)
    }

    fun endSession() {
        if (!isTracking()) {
            IterableLogger.e(TAG, "Inbox Session ended without start")
            return
        }

        //end all impressions that were started,where impressionStarted is non-null
        endAllImpressions()

        val sessionToTrack = IterableInboxSession(
            session.sessionStartTime,
            Date(),
            session.startTotalMessageCount,
            session.startUnreadMessageCount,
            IterableApi.getInstance().inAppManager?.inboxMessages?.size ?: 0,
            IterableApi.getInstance().inAppManager?.unreadInboxMessagesCount ?: 0,
            getImpressionList()
        )

        IterableApi.getInstance().trackInboxSession(sessionToTrack)
        IterableApi.getInstance().clearInboxSessionId()

        session = IterableInboxSession()
        impressions = HashMap()

        //previous impressions need to be reset to empty for the next session
        previousImpressions = HashSet()
    }

    fun updateVisibleRows(visibleRows: List<IterableInboxSession.Impression>) {
        IterableLogger.printInfo()

        // this code is basically doing the equivalent of a diff, but manually
        // sorry, i couldn't find a better/quicker way under the time constraint
        val visibleMessageIds = HashSet<String>()

        //add visible ids to hash set
        for (row in visibleRows) {
            visibleMessageIds.add(row.messageId)
        }

        //lists impressions to start
        //removes all visible rows that have impressions that were started
        val impressionsToStart = HashSet(visibleMessageIds)
        impressionsToStart.removeAll(previousImpressions)

        //list impressions to end
        //removes all visible rows that are still going
        val impressionsToEnd = HashSet(previousImpressions)
        impressionsToEnd.removeAll(visibleMessageIds)

        //set previous impressions for next iteration to the current visible messages
        previousImpressions = HashSet(visibleMessageIds)
        previousImpressions.removeAll(impressionsToEnd)

        //start all impressions designated to start
        for (messageId in impressionsToStart) {
            val message = IterableApi.getInstance().inAppManager?.getMessageById(messageId)
            if (message != null) {
                onMessageImpressionStarted(message)
            }
        }

        //end all impressions designated to end
        for (messageId in impressionsToEnd) {
            endImpression(messageId)
        }
    }

    fun onMessageImpressionStarted(message: IterableInAppMessage) {
        IterableLogger.printInfo()

        val messageId = message.messageId
        startImpression(messageId, message.isSilentInboxMessage())
    }

    fun onMessageImpressionEnded(message: IterableInAppMessage) {
        IterableLogger.printInfo()

        val messageId = message.messageId
        endImpression(messageId)
    }

    private fun startImpression(messageId: String, silentInbox: Boolean) {
        var impressionData = impressions[messageId]

        if (impressionData == null) {
            impressionData = ImpressionData(messageId, silentInbox)
            impressions[messageId] = impressionData
        }

        impressionData.startImpression()
    }

    private fun endImpression(messageId: String) {
        val impressionData = impressions[messageId]

        if (impressionData == null) {
            IterableLogger.e(TAG, "onMessageImpressionEnded: impressionData not found")
            return
        }

        if (impressionData.impressionStarted == null) {
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

    private fun getImpressionList(): List<IterableInboxSession.Impression> {
        val impressionList = ArrayList<IterableInboxSession.Impression>()
        for (impressionData in impressions.values) {
            impressionList.add(IterableInboxSession.Impression(
                impressionData.messageId,
                impressionData.silentInbox,
                impressionData.displayCount,
                impressionData.duration
            ))
        }
        return impressionList
    }
}
