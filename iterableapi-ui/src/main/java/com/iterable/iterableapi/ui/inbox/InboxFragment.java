package com.iterable.iterableapi.ui.inbox;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.iterable.iterableapi.IterableApi;
import com.iterable.iterableapi.IterableInAppDeleteActionType;
import com.iterable.iterableapi.IterableInAppLocation;
import com.iterable.iterableapi.IterableInAppManager;
import com.iterable.iterableapi.IterableInAppMessage;
import com.iterable.iterableapi.IterableInboxSession;
import com.iterable.iterableapi.IterableLogger;
import com.iterable.iterableapi.ui.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InboxFragment extends Fragment implements IterableInAppManager.Listener, InboxRecyclerViewAdapter.OnListInteractionListener {

    private static final String TAG = "InboxFragment";

    private final SessionManager sessionManager = new SessionManager();

    public static InboxFragment newInstance() {
        return new InboxFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerView view = (RecyclerView) inflater.inflate(R.layout.fragment_inbox_list, container, false);
        view.setLayoutManager(new LinearLayoutManager(getContext()));
        InboxRecyclerViewAdapter adapter = new InboxRecyclerViewAdapter(IterableApi.getInstance().getInAppManager().getInboxMessages(), InboxFragment.this);
        view.setAdapter(adapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new InboxRecyclerViewTouchHelper(getContext(), adapter));
        itemTouchHelper.attachToRecyclerView(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateList();
        IterableApi.getInstance().getInAppManager().addListener(this);
        sessionManager.onAppDidEnterForeground();
    }

    @Override
    public void onPause() {
        IterableApi.getInstance().getInAppManager().removeListener(this);
        sessionManager.onAppDidEnterBackground();
        super.onPause();
    }

    private void updateList() {
        RecyclerView recyclerView = (RecyclerView) getView();
        InboxRecyclerViewAdapter adapter = (InboxRecyclerViewAdapter) recyclerView.getAdapter();
        adapter.setValues(IterableApi.getInstance().getInAppManager().getInboxMessages());
    }

    @Override
    public void onInboxUpdated() {
        updateList();
    }

    @Override
    public void onListItemTapped(IterableInAppMessage message) {
        IterableApi.getInstance().getInAppManager().setRead(message, true);
        startActivity(new Intent(getContext(), InboxMessageActivity.class).putExtra(InboxMessageActivity.ARG_MESSAGE_ID, message.getMessageId()));
    }

    @Override
    public void onListItemDeleted(IterableInAppMessage message, IterableInAppDeleteActionType source) {
        IterableApi.getInstance().getInAppManager().removeMessage(message, source, IterableInAppLocation.INBOX);
    }

    @Override
    public void onListItemImpressionStarted(IterableInAppMessage message) {
        sessionManager.onMessageImpressionStarted(message);
    }

    @Override
    public void onListItemImpressionEnded(IterableInAppMessage message) {
        sessionManager.onMessageImpressionEnded(message);
    }

    private static class SessionManager {
        IterableInboxSession session = new IterableInboxSession();
        Map<String, ImpressionData> impressions = new HashMap<>();

        private void onAppDidEnterForeground() {
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
        }

        private void onAppDidEnterBackground() {
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
            session = new IterableInboxSession();
            impressions = new HashMap<>();
        }

        private void onMessageImpressionStarted(IterableInAppMessage message) {
            String messageId = message.getMessageId();
            ImpressionData impressionData = impressions.get(messageId);
            if (impressionData == null) {
                impressionData = new ImpressionData(messageId, message.isSilentInboxMessage());
                impressions.put(messageId, impressionData);
            }
            impressionData.startImpression();
        }

        private void onMessageImpressionEnded(IterableInAppMessage message) {
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

    private static class ImpressionData {
        final String messageId;
        final boolean silentInbox;
        int displayCount = 0;
        float duration = 0.0f;

        Date impressionStarted = null;

        private ImpressionData(String messageId, boolean silentInbox) {
            this.messageId = messageId;
            this.silentInbox = silentInbox;
        }

        private void startImpression() {
            this.impressionStarted = new Date();
        }

        private void endImpression() {
            if (this.impressionStarted != null) {
                this.displayCount += 1;
                this.duration += (float)(new Date().getTime() - this.impressionStarted.getTime()) / 1000;
                this.impressionStarted = null;
            }
        }
    }
}
