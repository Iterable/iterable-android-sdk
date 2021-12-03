package com.iterable.iterableapi;

import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InboxSessionManager {
    private static final String TAG = "InboxSessionManager";

    IterableInboxSession session = new IterableInboxSession();
    Map<String, ImpressionData> impressions = new HashMap<>();

    public boolean isTracking() {
        return session.sessionStartTime != null;
    }

    public void startSession() {
        if (isTracking()) {
            IterableLogger.e(TAG, "Inbox session started twice");
            return;
        }

        session = new IterableInboxSession(
                new Date(),
                null,
                IterableApi.getInstance().getInAppManager().getInboxMessages().size(),
                IterableApi.getInstance().getInAppManager().getUnreadInboxMessagesCount(),
                0,
                0,
                null);

        IterableApi.getInstance().setInboxSessionId(session.sessionId);
    }

    public void startSession(List<IterableInboxSession.Impression> visibleRows) {
        startSession();

        updateVisibleRows(visibleRows);
    }

    public void endSession() {
        if (!isTracking()) {
            IterableLogger.e(TAG, "Inbox Session ended without start");
            return;
        }

        endAllImpressions();

        IterableInboxSession sessionToTrack = new IterableInboxSession(
                session.sessionStartTime,
                new Date(),
                session.startTotalMessageCount,
                session.startUnreadMessageCount,
                IterableApi.getInstance().getInAppManager().getInboxMessages().size(),
                IterableApi.getInstance().getInAppManager().getUnreadInboxMessagesCount(),
                getImpressionList());

        IterableApi.getInstance().trackInboxSession(sessionToTrack);
        IterableApi.getInstance().clearInboxSessionId();

        session = new IterableInboxSession();
        impressions = new HashMap<>();
    }

    public void updateVisibleRows(List<IterableInboxSession.Impression> visibleRows) {
        IterableLogger.printInfo();

        // this code is basically doing the equivalent of a diff, but manually
        // sorry, i couldn't find a better/quicker way under the time constraint
        Set<String> previousImpressions = impressions.keySet();
        HashSet<String> visibleMessageIds = new HashSet();

        for (IterableInboxSession.Impression row : visibleRows) {
            visibleMessageIds.add(row.messageId);
        }

        Set<String> impressionsToStart = visibleMessageIds;
        impressionsToStart.removeAll(previousImpressions);

        Set<String> impressionsToEnd = previousImpressions;
        impressionsToEnd.removeAll(visibleMessageIds);

        for (String messageId : impressionsToStart) {
            onMessageImpressionStarted(IterableApi.getInstance().getInAppManager().getMessageById(messageId));
        }

        for (String messageId : impressionsToEnd) {
            endImpression(messageId);
        }
    }

    public void onMessageImpressionStarted(IterableInAppMessage message) {
        IterableLogger.printInfo();

        String messageId = message.getMessageId();
        startImpression(messageId, message.isSilentInboxMessage());
    }

    public void onMessageImpressionEnded(IterableInAppMessage message) {
        IterableLogger.printInfo();

        String messageId = message.getMessageId();
        endImpression(messageId);
    }

    private void startImpression(String messageId, boolean silentInbox) {
        ImpressionData impressionData = impressions.get(messageId);

        if (impressionData == null) {
            impressionData = new ImpressionData(messageId, silentInbox);
            impressions.put(messageId, impressionData);
        }

        impressionData.startImpression();
    }

    private void endImpression(String messageId) {
        ImpressionData impressionData = impressions.get(messageId);

        if (impressionData == null) {
            IterableLogger.e(TAG, "onMessageImpressionEnded: impressionData not found");
            return;
        }

        if (impressionData.impressionStarted == null) {
            IterableLogger.e(TAG, "onMessageImpressionEnded: impressionStarted is null");
            return;
        }

        impressionData.endImpression();
    }

    private void endAllImpressions() {
        for (ImpressionData impressionData : impressions.values()) {
            impressionData.endImpression();
        }
    }

    private List<IterableInboxSession.Impression> getImpressionList() {
        List<IterableInboxSession.Impression> impressionList = new ArrayList<>();
        for (ImpressionData impressionData : impressions.values()) {
            impressionList.add(new IterableInboxSession.Impression(
                    impressionData.messageId,
                    impressionData.silentInbox,
                    impressionData.displayCount,
                    impressionData.duration
            ));
        }
        return impressionList;
    }
}
