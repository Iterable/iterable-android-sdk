package com.iterable.iterableapi.ui.inbox

import android.content.Intent
import android.graphics.Insets
import android.os.Build
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView

import com.iterable.iterableapi.InboxSessionManager
import com.iterable.iterableapi.IterableActivityMonitor
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableConstants
import com.iterable.iterableapi.IterableInAppDeleteActionType
import com.iterable.iterableapi.IterableInAppLocation
import com.iterable.iterableapi.IterableInAppManager
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.IterableLogger
import com.iterable.iterableapi.ui.R

import java.text.DateFormat

/**
 * The main class for Inbox UI. Renders the list of Inbox messages and handles touch interaction:
 * tap on an item opens the in-app message, swipe left deletes it.
 *
 * To customize the UI, either create the fragment with [newInstance],
 * or subclass [IterableInboxFragment] to use [setAdapterExtension],
 * [setComparator] and [setFilter].
 */
class IterableInboxFragment : Fragment(), IterableInAppManager.Listener, IterableInboxAdapter.OnListInteractionListener {

    companion object {
        private const val TAG = "IterableInboxFragment"
        const val INBOX_MODE = "inboxMode"
        const val ITEM_LAYOUT_ID = "itemLayoutId"

        /**
         * Create an Inbox fragment with default parameters
         *
         * @return [IterableInboxFragment] instance
         */
        @NonNull
        @JvmStatic
        fun newInstance(): IterableInboxFragment {
            return IterableInboxFragment()
        }

        /**
         * Create an Inbox fragment with custom parameters for inbox mode and item layout id
         * To customize beyond these parameters, subclass [IterableInboxFragment].
         * (see class description)
         *
         * @param inboxMode    Inbox mode
         * @param itemLayoutId Layout resource id for inbox items. Pass 0 to use the default layout.
         * @return [IterableInboxFragment] instance
         */
        @NonNull
        @JvmStatic
        fun newInstance(@NonNull inboxMode: InboxMode, @LayoutRes itemLayoutId: Int): IterableInboxFragment {
            return newInstance(inboxMode, itemLayoutId, null, null)
        }

        @NonNull
        @JvmStatic
        fun newInstance(
            @NonNull inboxMode: InboxMode,
            @LayoutRes itemLayoutId: Int,
            @Nullable noMessagesTitle: String?,
            @Nullable noMessagesBody: String?
        ): IterableInboxFragment {
            val inboxFragment = IterableInboxFragment()
            val bundle = Bundle()
            bundle.putSerializable(INBOX_MODE, inboxMode)
            bundle.putInt(ITEM_LAYOUT_ID, itemLayoutId)
            bundle.putString(IterableConstants.NO_MESSAGES_TITLE, noMessagesTitle)
            bundle.putString(IterableConstants.NO_MESSAGES_BODY, noMessagesBody)
            inboxFragment.arguments = bundle
            return inboxFragment
        }
    }

    private var inboxMode = InboxMode.POPUP
    @LayoutRes
    private var itemLayoutId = R.layout.iterable_inbox_item
    private var noMessagesTitle: String? = null
    private var noMessagesBody: String? = null
    
    lateinit var noMessagesTitleTextView: TextView
    lateinit var noMessagesBodyTextView: TextView
    lateinit var recyclerView: RecyclerView

    private val sessionManager = InboxSessionManager()
    private var adapterExtension: IterableInboxAdapterExtension<*> = DefaultAdapterExtension()
    private var comparator: IterableInboxComparator = DefaultInboxComparator()
    private var filter: IterableInboxFilter = DefaultInboxFilter()
    private var dateMapper: IterableInboxDateMapper = DefaultInboxDateMapper()


    /**
     * Set the inbox mode to display inbox messages either in a new activity or as an overlay
     *
     * @param inboxMode Inbox mode
     */
    protected fun setInboxMode(@NonNull inboxMode: InboxMode) {
        this.inboxMode = inboxMode
    }

    /**
     * Set an adapter extension to customize the way inbox items are rendered.
     * See [IterableInboxAdapterExtension] for details.
     *
     * @param adapterExtension Custom adapter extension implemented by the app
     */
    protected fun setAdapterExtension(@NonNull adapterExtension: IterableInboxAdapterExtension<*>) {
        this.adapterExtension = adapterExtension
    }

    /**
     * Set a comparator to define message order in the inbox UI.
     *
     * @param comparator A [java.util.Comparator] implementation for [IterableInAppMessage]
     */
    protected fun setComparator(@NonNull comparator: IterableInboxComparator) {
        this.comparator = comparator
    }

    /**
     * Set a custom filter method to only show specific messages in the Inbox UI.
     *
     * @param filter Filter class that returns true or false to keep or exclude a message
     */
    protected fun setFilter(@NonNull filter: IterableInboxFilter) {
        this.filter = filter
    }

    /**
     * Set a custom date mapper to define how the date is rendered in an inbox cell
     *
     * @param dateMapper Date mapper class that takes an inbox message returns a string for the creation date
     */
    protected fun setDateMapper(@NonNull dateMapper: IterableInboxDateMapper) {
        this.dateMapper = dateMapper
    }

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IterableActivityMonitor.getInstance().addCallback(appStateCallback)
    }

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View? {
        IterableLogger.printInfo()
        val arguments = arguments
        if (arguments != null) {
            if (arguments.get(INBOX_MODE) is InboxMode) {
                inboxMode = arguments.get(INBOX_MODE) as InboxMode
            }
            if (arguments.getInt(ITEM_LAYOUT_ID, 0) != 0) {
                itemLayoutId = arguments.getInt(ITEM_LAYOUT_ID)
            }
            if (arguments.getString(IterableConstants.NO_MESSAGES_TITLE) != null) {
                noMessagesTitle = arguments.getString(IterableConstants.NO_MESSAGES_TITLE)
            }
            if (arguments.getString(IterableConstants.NO_MESSAGES_BODY) != null) {
                noMessagesBody = arguments.getString(IterableConstants.NO_MESSAGES_BODY)
            }
        }

        val relativeLayout = inflater.inflate(R.layout.iterable_inbox_fragment, container, false) as RelativeLayout
        recyclerView = relativeLayout.findViewById(R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val adapter = IterableInboxAdapter(
            IterableApi.getInstance().inAppManager.inboxMessages,
            this@IterableInboxFragment,
            adapterExtension,
            comparator,
            filter,
            dateMapper
        )
        recyclerView.adapter = adapter
        noMessagesTitleTextView = relativeLayout.findViewById(R.id.emptyInboxTitle)
        noMessagesBodyTextView = relativeLayout.findViewById(R.id.emptyInboxMessage)
        noMessagesTitleTextView.text = noMessagesTitle
        noMessagesBodyTextView.text = noMessagesBody
        val itemTouchHelper = ItemTouchHelper(IterableInboxTouchHelper(context, adapter))
        itemTouchHelper.attachToRecyclerView(recyclerView)
        return relativeLayout
    }

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use ViewCompat to handle insets dynamically
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For API 30 and above: Use WindowInsetsCompat to handle insets
                val systemBarsInsets = insets.systemGestureInsets.toPlatformInsets()
                v.setPadding(
                    0,
                    systemBarsInsets.top,  // Padding for status bar and cutout
                    0,
                    systemBarsInsets.bottom // Padding for navigation bar
                )
            } else {
                // For older Android versions: Use legacy methods
                v.setPadding(
                    0,
                    insets.systemWindowInsetTop,  // Padding for status bar and cutout
                    0,
                    insets.systemWindowInsetBottom // Padding for navigation bar
                )
            }
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        updateList()
        IterableApi.getInstance().inAppManager.addListener(this)
        sessionManager.startSession()
    }

    override fun onPause() {
        IterableApi.getInstance().inAppManager.removeListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        IterableActivityMonitor.getInstance().removeCallback(appStateCallback)
        if (this.activity != null && !this.activity!!.isChangingConfigurations) {
            sessionManager.endSession()
        }
    }

    private val appStateCallback = object : IterableActivityMonitor.AppStateCallback {
        override fun onSwitchToForeground() {
        }

        override fun onSwitchToBackground() {
            sessionManager.endSession()
        }
    }

    private fun updateList() {
        val adapter = recyclerView.adapter as IterableInboxAdapter
        adapter.setInboxItems(IterableApi.getInstance().inAppManager.inboxMessages)
        handleEmptyInbox(adapter)
    }

    private fun handleEmptyInbox(adapter: IterableInboxAdapter) {
        if (adapter.itemCount == 0) {
            noMessagesTitleTextView.visibility = View.VISIBLE
            noMessagesBodyTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.INVISIBLE
        } else {
            noMessagesTitleTextView.visibility = View.INVISIBLE
            noMessagesBodyTextView.visibility = View.INVISIBLE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onInboxUpdated() {
        updateList()
    }

    override fun onListItemTapped(@NonNull message: IterableInAppMessage) {
        IterableApi.getInstance().inAppManager.setRead(message, true, null, null)

        if (inboxMode == InboxMode.ACTIVITY) {
            startActivity(Intent(context, IterableInboxMessageActivity::class.java).putExtra(IterableInboxMessageActivity.ARG_MESSAGE_ID, message.messageId))
        } else {
            IterableApi.getInstance().inAppManager.showMessage(message, IterableInAppLocation.INBOX)
        }
    }

    override fun onListItemDeleted(@NonNull message: IterableInAppMessage, @NonNull source: IterableInAppDeleteActionType) {
        IterableApi.getInstance().inAppManager.removeMessage(message, source, IterableInAppLocation.INBOX, null, null)
    }

    override fun onListItemImpressionStarted(@NonNull message: IterableInAppMessage) {
        sessionManager.onMessageImpressionStarted(message)
    }

    override fun onListItemImpressionEnded(@NonNull message: IterableInAppMessage) {
        sessionManager.onMessageImpressionEnded(message)
    }

    /**
     * Default implementation of the adapter extension. Does nothing other than returning
     * the value of [IterableInboxFragment.itemLayoutId] for the view layout
     */
    private inner class DefaultAdapterExtension : IterableInboxAdapterExtension<Any?> {
        override fun getItemViewType(@NonNull message: IterableInAppMessage): Int {
            return 0
        }

        override fun getLayoutForViewType(viewType: Int): Int {
            return itemLayoutId
        }

        @Nullable
        override fun createViewHolderExtension(@NonNull view: View, viewType: Int): Any? {
            return null
        }

        override fun onBindViewHolder(@NonNull viewHolder: IterableInboxAdapter.ViewHolder, @Nullable holderExtension: Any?, @NonNull message: IterableInAppMessage) {

        }
    }

    /**
     * Default implementation of the comparator: descending by creation date
     */
    private class DefaultInboxComparator : IterableInboxComparator {
        override fun compare(@NonNull message1: IterableInAppMessage, @NonNull message2: IterableInAppMessage): Int {
            return -message1.createdAt.compareTo(message2.createdAt)
        }
    }

    /**
     * Default implementation of the filter. Accepts all inbox messages.
     */
    private class DefaultInboxFilter : IterableInboxFilter {
        override fun filter(@NonNull message: IterableInAppMessage): Boolean {
            return true
        }
    }

    /**
     * Default implementation of the date mapper.
     */
    private class DefaultInboxDateMapper : IterableInboxDateMapper {
        @Nullable
        override fun mapMessageToDateString(@NonNull message: IterableInAppMessage): CharSequence {
            return if (message.createdAt != null) {
                val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                formatter.format(message.createdAt)
            } else {
                ""
            }
        }
    }
}
