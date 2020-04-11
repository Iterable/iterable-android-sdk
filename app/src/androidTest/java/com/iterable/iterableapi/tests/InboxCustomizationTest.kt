package com.iterable.iterableapi.tests

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions.displayed
import br.com.concretesolutions.kappuccino.custom.recyclerView.RecyclerViewInteractions.recyclerView
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.testapp.R
import com.iterable.iterableapi.ui.inbox.IterableInboxAdapter
import com.iterable.iterableapi.ui.inbox.IterableInboxAdapterExtension
import com.iterable.iterableapi.ui.inbox.IterableInboxFragment
import com.iterable.iterableapi.util.DataManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InboxCustomizationTest {
    @Before
    fun setUp() {
        DataManager.initializeIterableApi(getApplicationContext())
    }

    class CustomDateFormatInboxFragment : IterableInboxFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setDateMapper { message ->
                "customDate"
            }
        }
    }

    @Test
    fun testCustomDateFormat() {
        DataManager.loadData("simple-inbox-messages.json")
        launchFragmentInContainer<CustomDateFormatInboxFragment>()
        displayed { id(com.iterable.iterableapi.ui.R.id.list) }
        recyclerView(com.iterable.iterableapi.ui.R.id.list) {
            sizeIs(4)
            atPosition(1) {
                displayed {
                    text("customDate")
                }
            }
        }
    }

    class InboxWithAdditionalFieldsFragment : IterableInboxFragment(),
            IterableInboxAdapterExtension<InboxWithAdditionalFieldsFragment.ViewHolder> {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setAdapterExtension(this)
        }

        override fun createViewHolderExtension(view: View, viewType: Int): ViewHolder? {
            return ViewHolder(view)
        }

        override fun getItemViewType(message: IterableInAppMessage): Int {
            return 0
        }

        override fun getLayoutForViewType(viewType: Int): Int {
            return R.layout.additional_fields_cell
        }

        override fun onBindViewHolder(
                viewHolder: IterableInboxAdapter.ViewHolder,
                holderExtension: ViewHolder?,
                message: IterableInAppMessage
        ) {
            holderExtension?.discountText?.text = message.customPayload.optString("discount")
        }

        class ViewHolder(view: View) {
            var discountText: TextView? = null

            init {
                this.discountText = view.findViewById(R.id.discountText)
            }
        }
    }

    @Test
    fun testInboxWithAdditionalFields() {
        DataManager.loadData("inbox-with-additional-fields-messages.json")
        launchFragmentInContainer<InboxWithAdditionalFieldsFragment>()
        displayed { id(com.iterable.iterableapi.ui.R.id.list) }
        recyclerView(com.iterable.iterableapi.ui.R.id.list) {
            sizeIs(4)
            atPosition(1) {
                displayed {
                    id(R.id.discountText)
                    text("30%")
                }
            }
        }
    }

    class FilterByMessageTypeInboxFragment : IterableInboxFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setFilter { message ->
                message.customPayload.optString("messageType") in setOf("transactional", "promotional")
            }
        }
    }

    @Test
    fun testInboxFiltering() {
        DataManager.loadData("filter-by-message-type-messages.json")
        launchFragmentInContainer<FilterByMessageTypeInboxFragment>()
        displayed { id(com.iterable.iterableapi.ui.R.id.list) }
        recyclerView(com.iterable.iterableapi.ui.R.id.list) {
            sizeIs(2)
            atPosition(0) {
                displayed {
                    text("This is a transactional message")
                }
            }
        }
    }

    class SortByDateAscendingInboxFragment : IterableInboxFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setComparator { message1, message2 ->
                message1.createdAt.compareTo(message2.createdAt)
            }
        }
    }

    @Test
    fun testInboxSorting() {
        DataManager.loadData("simple-inbox-messages.json")
        launchFragmentInContainer<SortByDateAscendingInboxFragment>()
        displayed { id(com.iterable.iterableapi.ui.R.id.list) }
        recyclerView(com.iterable.iterableapi.ui.R.id.list) {
            sizeIs(4)
            atPosition(0) {
                displayed { text("Buy mocha") }
            }
            atPosition(1) {
                displayed { text("Buy black coffee") }
            }
        }
    }
}