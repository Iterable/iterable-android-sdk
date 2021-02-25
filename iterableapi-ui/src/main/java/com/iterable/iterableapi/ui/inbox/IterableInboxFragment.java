package com.iterable.iterableapi.ui.inbox;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.iterable.iterableapi.IterableActivityMonitor;
import com.iterable.iterableapi.IterableApi;
import com.iterable.iterableapi.IterableConstants;
import com.iterable.iterableapi.IterableInAppDeleteActionType;
import com.iterable.iterableapi.IterableInAppLocation;
import com.iterable.iterableapi.IterableInAppManager;
import com.iterable.iterableapi.IterableInAppMessage;
import com.iterable.iterableapi.IterableInboxSession;
import com.iterable.iterableapi.IterableLogger;
import com.iterable.iterableapi.ui.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The main class for Inbox UI. Renders the list of Inbox messages and handles touch interaction:
 * tap on an item opens the in-app message, swipe left deletes it.
 * <p>
 * To customize the UI, either create the fragment with {@link #newInstance(InboxMode, int)},
 * or subclass {@link IterableInboxFragment} to use {@link #setAdapterExtension(IterableInboxAdapterExtension)},
 * {@link #setComparator(IterableInboxComparator)} and {@link #setFilter(IterableInboxFilter)}.
 */
public class IterableInboxFragment extends Fragment implements IterableInAppManager.Listener, IterableInboxAdapter.OnListInteractionListener {
    private static final String TAG = "IterableInboxFragment";
    public static final String INBOX_MODE = "inboxMode";
    public static final String ITEM_LAYOUT_ID = "itemLayoutId";

    private InboxMode inboxMode = InboxMode.POPUP;
    private @LayoutRes int itemLayoutId = R.layout.iterable_inbox_item;

    private final SessionManager sessionManager = new SessionManager();
    private IterableInboxAdapterExtension adapterExtension = new DefaultAdapterExtension();
    private IterableInboxComparator comparator = new DefaultInboxComparator();
    private IterableInboxFilter filter = new DefaultInboxFilter();
    private IterableInboxDateMapper dateMapper = new DefaultInboxDateMapper();
    private boolean sessionStarted = false;
    private String noMessagesTitle;
    private String noMessagesBody;

    TextView noMessagesTitleTextView;
    TextView noMessagesBodyTextView;
    RecyclerView recyclerView;
    /**
     * Create an Inbox fragment with default parameters
     *
     * @return {@link IterableInboxFragment} instance
     */
    @NonNull public static IterableInboxFragment newInstance() {
        return new IterableInboxFragment();
    }

    /**
     * Create an Inbox fragment with custom parameters for inbox mode and item layout id
     * To customize beyond these parameters, subclass {@link IterableInboxFragment}.
     * (see class description)
     *
     * @param inboxMode    Inbox mode
     * @param itemLayoutId Layout resource id for inbox items. Pass 0 to use the default layout.
     * @return {@link IterableInboxFragment} instance
     */
    @NonNull public static IterableInboxFragment newInstance(@NonNull InboxMode inboxMode, @LayoutRes int itemLayoutId) {
        IterableInboxFragment inboxFragment = new IterableInboxFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(INBOX_MODE, inboxMode);
        bundle.putInt(ITEM_LAYOUT_ID, itemLayoutId);
        inboxFragment.setArguments(bundle);

        return inboxFragment;
    }

    /**
     * Set the inbox mode to display inbox messages either in a new activity or as an overlay
     *
     * @param inboxMode Inbox mode
     */
    protected void setInboxMode(@NonNull InboxMode inboxMode) {
        this.inboxMode = inboxMode;
    }

    /**
     * Set an adapter extension to customize the way inbox items are rendered.
     * See {@link IterableInboxAdapterExtension} for details.
     *
     * @param adapterExtension Custom adapter extension implemented by the app
     */
    protected void setAdapterExtension(@NonNull IterableInboxAdapterExtension adapterExtension) {
        if (adapterExtension != null) {
            this.adapterExtension = adapterExtension;
        }
    }

    /**
     * Set a comparator to define message order in the inbox UI.
     *
     * @param comparator A{@link java.util.Comparator} implementation for {@link IterableInAppMessage}
     */
    protected void setComparator(@NonNull IterableInboxComparator comparator) {
        if (comparator != null) {
            this.comparator = comparator;
        }
    }

    /**
     * Set a custom filter method to only show specific messages in the Inbox UI.
     *
     * @param filter Filter class that returns true or false to keep or exclude a message
     */
    protected void setFilter(@NonNull IterableInboxFilter filter) {
        if (filter != null) {
            this.filter = filter;
        }
    }

    /**
     * Set a custom date mapper to define how the date is rendered in an inbox cell
     *
     * @param dateMapper Date mapper class that takes an inbox message returns a string for the creation date
     */
    protected void setDateMapper(@NonNull IterableInboxDateMapper dateMapper) {
        if (dateMapper != null) {
            this.dateMapper = dateMapper;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IterableActivityMonitor.getInstance().addCallback(appStateCallback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        IterableLogger.printInfo();
        Bundle arguments = getArguments();
        if (arguments != null) {
            if (arguments.get(INBOX_MODE) instanceof InboxMode) {
                inboxMode = (InboxMode) arguments.get(INBOX_MODE);
            }
            if (arguments.getInt(ITEM_LAYOUT_ID, 0) != 0) {
                itemLayoutId = arguments.getInt(ITEM_LAYOUT_ID);
            }
            if (arguments.getString(IterableConstants.NO_MESSAGES_TITLE) != null) {
                this.noMessagesTitle = arguments.getString(IterableConstants.NO_MESSAGES_TITLE);
            }
            if (arguments.getString(IterableConstants.NO_MESSAGES_BODY) != null) {
                this.noMessagesBody = arguments.getString(IterableConstants.NO_MESSAGES_BODY);
            }
        }

        RelativeLayout relativeLayout = (RelativeLayout) inflater.inflate(R.layout.iterable_inbox_fragment, container, false);
        recyclerView = relativeLayout.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        IterableInboxAdapter adapter = new IterableInboxAdapter(IterableApi.getInstance().getInAppManager().getInboxMessages(), IterableInboxFragment.this, adapterExtension, comparator, filter, dateMapper);
        recyclerView.setAdapter(adapter);
        noMessagesTitleTextView = relativeLayout.findViewById(R.id.emptyInboxTitle);
        noMessagesBodyTextView = relativeLayout.findViewById(R.id.emptyInboxMessage);
        noMessagesTitleTextView.setText(noMessagesTitle);
        noMessagesBodyTextView.setText(noMessagesBody);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new IterableInboxTouchHelper(getContext(), adapter));
        itemTouchHelper.attachToRecyclerView(recyclerView);
        return relativeLayout.getRootView();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateList();
        IterableApi.getInstance().getInAppManager().addListener(this);
        startSession();
    }

    @Override
    public void onPause() {
        IterableApi.getInstance().getInAppManager().removeListener(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        IterableActivityMonitor.getInstance().removeCallback(appStateCallback);
        if (this.getActivity() != null && !this.getActivity().isChangingConfigurations()) {
            stopSession();
        }
    }

    private final IterableActivityMonitor.AppStateCallback appStateCallback = new IterableActivityMonitor.AppStateCallback() {
        @Override
        public void onSwitchToForeground() {
        }

        @Override
        public void onSwitchToBackground() {
            stopSession();
        }
    };

    private void startSession() {
        if (!sessionStarted) {
            sessionStarted = true;
            sessionManager.onAppDidEnterForeground();
        }
    }

    private void stopSession() {
        if (sessionStarted) {
            sessionStarted = false;
            sessionManager.onAppDidEnterBackground();
        }
    }

    private void updateList() {
        IterableInboxAdapter adapter = (IterableInboxAdapter) recyclerView.getAdapter();
        adapter.setInboxItems(IterableApi.getInstance().getInAppManager().getInboxMessages());
        handleEmptyInbox(adapter);
    }

    private void handleEmptyInbox(IterableInboxAdapter adapter) {
        if (adapter.getItemCount() == 0) {
            noMessagesTitleTextView.setVisibility(View.VISIBLE);
            noMessagesBodyTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
        } else {
            noMessagesTitleTextView.setVisibility(View.INVISIBLE);
            noMessagesBodyTextView.setVisibility(View.INVISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onInboxUpdated() {
        updateList();
    }

    @Override
    public void onListItemTapped(@NonNull IterableInAppMessage message) {
        IterableApi.getInstance().getInAppManager().setRead(message, true);

        if (inboxMode == InboxMode.ACTIVITY) {
            startActivity(new Intent(getContext(), IterableInboxMessageActivity.class).putExtra(IterableInboxMessageActivity.ARG_MESSAGE_ID, message.getMessageId()));
        } else {
            IterableApi.getInstance().getInAppManager().showMessage(message, IterableInAppLocation.INBOX);
        }
    }

    @Override
    public void onListItemDeleted(@NonNull IterableInAppMessage message, @NonNull IterableInAppDeleteActionType source) {
        IterableApi.getInstance().getInAppManager().removeMessage(message, source, IterableInAppLocation.INBOX);
    }

    @Override
    public void onListItemImpressionStarted(@NonNull IterableInAppMessage message) {
        sessionManager.onMessageImpressionStarted(message);
    }

    @Override
    public void onListItemImpressionEnded(@NonNull IterableInAppMessage message) {
        sessionManager.onMessageImpressionEnded(message);
    }

    /**
     * Default implementation of the adapter extension. Does nothing other than returning
     * the value of {@link IterableInboxFragment#itemLayoutId} for the view layout
     */
    private class DefaultAdapterExtension implements IterableInboxAdapterExtension<Object> {
        @Override
        public int getItemViewType(@NonNull IterableInAppMessage message) {
            return 0;
        }

        @Override
        public int getLayoutForViewType(int viewType) {
            return itemLayoutId;
        }

        @Nullable
        @Override
        public Object createViewHolderExtension(@NonNull View view, int viewType) {
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull IterableInboxAdapter.ViewHolder viewHolder, @Nullable Object holderExtension, @NonNull IterableInAppMessage message) {

        }
    }

    /**
     * Default implementation of the comparator: descending by creation date
     */
    private static class DefaultInboxComparator implements IterableInboxComparator {
        @Override
        public int compare(@NonNull IterableInAppMessage message1, @NonNull IterableInAppMessage message2) {
            return -message1.getCreatedAt().compareTo(message2.getCreatedAt());
        }
    }

    /**
     * Default implementation of the filter. Accepts all inbox messages.
     */
    private static class DefaultInboxFilter implements IterableInboxFilter {
        @Override
        public boolean filter(@NonNull IterableInAppMessage message) {
            return true;
        }
    }

    /**
     * Default implementation of the date mapper.
     */
    private static class DefaultInboxDateMapper implements IterableInboxDateMapper {
        @Nullable
        @Override
        public CharSequence mapMessageToDateString(@NonNull IterableInAppMessage message) {
            if (message.getCreatedAt() != null) {
                DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
                return formatter.format(message.getCreatedAt());
            } else {
                return "";
            }
        }
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
            IterableApi.getInstance().setInboxSessionId(session.sessionId);
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
            IterableApi.getInstance().clearInboxSessionId();
            session = new IterableInboxSession();
            impressions = new HashMap<>();
        }

        private void onMessageImpressionStarted(IterableInAppMessage message) {
            IterableLogger.printInfo();
            String messageId = message.getMessageId();
            ImpressionData impressionData = impressions.get(messageId);
            if (impressionData == null) {
                impressionData = new ImpressionData(messageId, message.isSilentInboxMessage());
                impressions.put(messageId, impressionData);
            }
            impressionData.startImpression();
        }

        private void onMessageImpressionEnded(IterableInAppMessage message) {
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
                this.duration += (float) (new Date().getTime() - this.impressionStarted.getTime()) / 1000;
                this.impressionStarted = null;
            }
        }
    }
}
