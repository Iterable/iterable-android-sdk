package com.iterable.iterableapi.ui.inbox;

import android.content.Context;
import android.content.Intent;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BundleCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.iterable.iterableapi.InboxSessionManager;
import com.iterable.iterableapi.IterableActivityMonitor;
import com.iterable.iterableapi.IterableApi;
import com.iterable.iterableapi.IterableConstants;
import com.iterable.iterableapi.IterableInAppDeleteActionType;
import com.iterable.iterableapi.IterableInAppLocation;
import com.iterable.iterableapi.IterableInAppManager;
import com.iterable.iterableapi.IterableInAppMessage;
import com.iterable.iterableapi.IterableLogger;
import com.iterable.iterableapi.ui.R;

import java.text.DateFormat;

/**
 * The main class for Inbox UI. Renders the list of Inbox messages and handles touch interaction:
 * tap on an item opens the in-app message, swipe left deletes it.
 * <p>
 * To customize the UI, create the fragment with one of the {@code newInstance(...)} overloads
 * (use {@link InboxToolbarOption} to opt into the built-in toolbar), or subclass
 * {@link IterableInboxFragment} to use {@link #setAdapterExtension(IterableInboxAdapterExtension)},
 * {@link #setComparator(IterableInboxComparator)} and {@link #setFilter(IterableInboxFilter)}.
 * Implement {@link IterableInboxToolbarBackListener} on the host to handle toolbar back clicks.
 * <p>
 * The host activity must use a {@code Theme.AppCompat} (or {@code Theme.MaterialComponents} /
 * {@code Theme.Material3}) descendant when the opt-in toolbar is enabled.
 */
public class IterableInboxFragment extends Fragment implements IterableInAppManager.Listener, IterableInboxAdapter.OnListInteractionListener {
    private static final String TAG = "IterableInboxFragment";
    public static final String INBOX_MODE = "inboxMode";
    public static final String ITEM_LAYOUT_ID = "itemLayoutId";
    public static final String TOOLBAR_OPTION = "toolbarOption";
    public static final String TOOLBAR_TITLE = "toolbarTitle";

    private InboxMode inboxMode = InboxMode.POPUP;
    private @LayoutRes int itemLayoutId = R.layout.iterable_inbox_item;
    private String noMessagesTitle;
    private String noMessagesBody;
    private InboxToolbarOption toolbarOption = InboxToolbarOption.None.INSTANCE;
    private @Nullable String toolbarTitle;
    private @Nullable IterableInboxToolbarBackListener toolbarBackListener;
    TextView noMessagesTitleTextView;
    TextView noMessagesBodyTextView;
    RecyclerView recyclerView;

    private final InboxSessionManager sessionManager = new InboxSessionManager();
    private IterableInboxAdapterExtension adapterExtension = new DefaultAdapterExtension();
    private IterableInboxComparator comparator = new DefaultInboxComparator();
    private IterableInboxFilter filter = new DefaultInboxFilter();
    private IterableInboxDateMapper dateMapper = new DefaultInboxDateMapper();


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
        return newInstance(inboxMode, itemLayoutId, null, null);
    }

    @NonNull public static IterableInboxFragment newInstance(@NonNull InboxMode inboxMode, @LayoutRes int itemLayoutId, @Nullable String noMessagesTitle, @Nullable String noMessagesBody) {
        IterableInboxFragment inboxFragment = new IterableInboxFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(INBOX_MODE, inboxMode);
        bundle.putInt(ITEM_LAYOUT_ID, itemLayoutId);
        bundle.putString(IterableConstants.NO_MESSAGES_TITLE, noMessagesTitle);
        bundle.putString(IterableConstants.NO_MESSAGES_BODY, noMessagesBody);
        inboxFragment.setArguments(bundle);

        return inboxFragment;
    }

    /**
     * Create an Inbox fragment with toolbar customization; all other parameters use their defaults.
     *
     * @param toolbarOption Toolbar variant
     * @param toolbarTitle  Title shown in the toolbar, or null for the default "Inbox" string
     * @return {@link IterableInboxFragment} instance
     */
    @NonNull public static IterableInboxFragment newInstance(
        @NonNull InboxToolbarOption toolbarOption,
        @Nullable String toolbarTitle
    ) {
        return newInstance(InboxMode.POPUP, 0, null, null, toolbarOption, toolbarTitle);
    }

    @NonNull public static IterableInboxFragment newInstance(
        @NonNull InboxMode inboxMode,
        @LayoutRes int itemLayoutId,
        @Nullable String noMessagesTitle,
        @Nullable String noMessagesBody,
        @NonNull InboxToolbarOption toolbarOption,
        @Nullable String toolbarTitle
    ) {
        IterableInboxFragment inboxFragment = new IterableInboxFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(INBOX_MODE, inboxMode);
        bundle.putInt(ITEM_LAYOUT_ID, itemLayoutId);
        bundle.putString(IterableConstants.NO_MESSAGES_TITLE, noMessagesTitle);
        bundle.putString(IterableConstants.NO_MESSAGES_BODY, noMessagesBody);
        bundle.putSerializable(TOOLBAR_OPTION, toolbarOption);
        bundle.putString(TOOLBAR_TITLE, toolbarTitle);
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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Fragment parent = getParentFragment();
        if (parent instanceof IterableInboxToolbarBackListener) {
            toolbarBackListener = (IterableInboxToolbarBackListener) parent;
        } else if (context instanceof IterableInboxToolbarBackListener) {
            toolbarBackListener = (IterableInboxToolbarBackListener) context;
        }
    }

    @Override
    public void onDetach() {
        toolbarBackListener = null;
        super.onDetach();
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
                noMessagesTitle = arguments.getString(IterableConstants.NO_MESSAGES_TITLE);
            }
            if (arguments.getString(IterableConstants.NO_MESSAGES_BODY) != null) {
                noMessagesBody = arguments.getString(IterableConstants.NO_MESSAGES_BODY);
            }
            InboxToolbarOption toolbarOptionArg = BundleCompat.getSerializable(arguments, TOOLBAR_OPTION, InboxToolbarOption.class);
            if (toolbarOptionArg != null) {
                toolbarOption = toolbarOptionArg;
            }
            if (arguments.getString(TOOLBAR_TITLE) != null) {
                toolbarTitle = arguments.getString(TOOLBAR_TITLE);
            }
        }

        RelativeLayout relativeLayout = (RelativeLayout) inflater.inflate(R.layout.iterable_inbox_fragment, container, false);

        IterableInboxToolbarView toolbar = relativeLayout.findViewById(R.id.iterable_inbox_toolbar);
        toolbar.apply(toolbarOption, toolbarTitle);
        // Prefer the host listener if one was discovered in onAttach; otherwise delegate
        // to the fragment's host activity so we never depend on the view's Context chain
        // to find a ComponentActivity.
        if (toolbarBackListener != null) {
            toolbar.setOnBackClickListener(v -> toolbarBackListener.onInboxToolbarBackClick());
        } else {
            toolbar.setOnBackClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
            );
        }

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
        return relativeLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Use ViewCompat to handle insets dynamically
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For API 30 and above: Use WindowInsetsCompat to handle insets
                Insets systemBarsInsets = insets.getSystemGestureInsets().toPlatformInsets();
                v.setPadding(
                        0,
                        systemBarsInsets.top,  // Padding for status bar and cutout
                        0,
                        systemBarsInsets.bottom // Padding for navigation bar
                );
            } else {
                // For older Android versions: Use legacy methods
                v.setPadding(
                        0,
                        insets.getSystemWindowInsetTop(),  // Padding for status bar and cutout
                        0,
                        insets.getSystemWindowInsetBottom() // Padding for navigation bar
                );
            }
            return insets;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateList();
        IterableApi.getInstance().getInAppManager().addListener(this);

        sessionManager.startSession();
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
            sessionManager.endSession();
        }
    }

    private final IterableActivityMonitor.AppStateCallback appStateCallback = new IterableActivityMonitor.AppStateCallback() {
        @Override
        public void onSwitchToForeground() {
        }

        @Override
        public void onSwitchToBackground() {
            sessionManager.endSession();
        }
    };

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
        IterableApi.getInstance().getInAppManager().setRead(message, true, null, null);

        if (inboxMode == InboxMode.ACTIVITY) {
            startActivity(new Intent(getContext(), IterableInboxMessageActivity.class).putExtra(IterableInboxMessageActivity.ARG_MESSAGE_ID, message.getMessageId()));
        } else {
            IterableApi.getInstance().getInAppManager().showMessage(message, IterableInAppLocation.INBOX);
        }
    }

    @Override
    public void onListItemDeleted(@NonNull IterableInAppMessage message, @NonNull IterableInAppDeleteActionType source) {
        IterableApi.getInstance().getInAppManager().removeMessage(message, source, IterableInAppLocation.INBOX, null, null);
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
}
