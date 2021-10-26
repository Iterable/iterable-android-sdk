package com.iterable.iterableapi;

import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iterable.iterableapi.IterableInboxSession.Impression;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InboxSessionManager {
    private static final String TAG = "InboxSessionManager";

    IterableInboxSession session = new IterableInboxSession();
    Map<String, ImpressionData> impressions = new HashMap<>();

    public void startSession() {
        if (session.sessionStartTime != null) {
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

    public void endSession() {
        if (session.sessionStartTime == null) {
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

    public void onMessageImpressionStarted(IterableInAppMessage message) {
        IterableLogger.printInfo();
        String messageId = message.getMessageId();
        ImpressionData impressionData = impressions.get(messageId);
        if (impressionData == null) {
            impressionData = new ImpressionData(messageId, message.isSilentInboxMessage());
            impressions.put(messageId, impressionData);
        }
        impressionData.startImpression();
    }

    public void onMessageImpressionEnded(IterableInAppMessage message) {
        IterableLogger.printInfo();
        String messageId = message.getMessageId();
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
