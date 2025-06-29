package com.iterable.iterableapi.ui.inbox

import android.net.Uri
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.util.ObjectsCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.iterable.iterableapi.IterableInAppDeleteActionType
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.ui.BitmapLoader
import com.iterable.iterableapi.ui.R

import java.util.ArrayList
import java.util.Date

class IterableInboxAdapter(
    values: List<IterableInAppMessage>,
    @NonNull private val listener: OnListInteractionListener,
    @NonNull private val extension: IterableInboxAdapterExtension<*>,
    @NonNull private val comparator: IterableInboxComparator,
    @NonNull private val filter: IterableInboxFilter,
    @NonNull private val dateMapper: IterableInboxDateMapper
) : RecyclerView.Adapter<IterableInboxAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "IterableInboxAdapter"
    }

    private var inboxItems: MutableList<InboxRow> = inboxRowListFromInboxMessages(values).toMutableList()

    private val onClickListener = View.OnClickListener { v ->
        val inboxMessage = v.tag as IterableInAppMessage
        listener.onListItemTapped(inboxMessage)
    }

    override fun getItemViewType(position: Int): Int {
        return extension.getItemViewType(inboxItems[position].message)
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(extension.getLayoutForViewType(viewType), parent, false)
        return ViewHolder(view, extension.createViewHolderExtension(view, viewType))
    }

    override fun onBindViewHolder(@NonNull holder: ViewHolder, position: Int) {
        val inboxRow = inboxItems[position]
        val inboxMetadata = inboxRow.inboxMetadata

        holder.title?.setText(inboxMetadata.title)
        holder.subtitle?.setText(inboxMetadata.subtitle)
        holder.icon?.let { BitmapLoader.loadBitmap(it, Uri.parse(inboxMetadata.icon)) }

        holder.unreadIndicator?.visibility = if (inboxRow.isRead) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }

        holder.date?.setText(dateMapper.mapMessageToDateString(inboxRow.message))

        holder.itemView.tag = inboxRow.message
        holder.itemView.setOnClickListener(onClickListener)
        extension.onBindViewHolder(holder, holder.extension, inboxRow.message)
    }

    override fun getItemCount(): Int {
        return inboxItems.size
    }

    override fun onViewAttachedToWindow(@NonNull holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        val message = holder.itemView.tag as IterableInAppMessage
        listener.onListItemImpressionStarted(message)
    }

    override fun onViewDetachedFromWindow(@NonNull holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        val message = holder.itemView.tag as IterableInAppMessage
        listener.onListItemImpressionEnded(message)
    }

    fun setInboxItems(@NonNull newValues: List<IterableInAppMessage>) {
        val newRowValues = inboxRowListFromInboxMessages(newValues)
        val diffCallback = InAppMessageDiffCallback(inboxItems, newRowValues)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        inboxItems.clear()
        inboxItems.addAll(newRowValues)
        diffResult.dispatchUpdatesTo(this)
    }

    fun deleteItem(position: Int, @NonNull source: IterableInAppDeleteActionType) {
        val deletedItem = inboxItems[position].message
        inboxItems.removeAt(position)
        listener.onListItemDeleted(deletedItem, source)
        notifyItemRemoved(position)
    }

    class ViewHolder(
        itemView: View,
        val extension: Any?
    ) : RecyclerView.ViewHolder(itemView) {
        @Nullable val title: TextView? = itemView.findViewById(R.id.title)
        @Nullable val subtitle: TextView? = itemView.findViewById(R.id.subtitle)
        @Nullable val icon: ImageView? = itemView.findViewById(R.id.imageView)
        @Nullable val unreadIndicator: ImageView? = itemView.findViewById(R.id.unreadIndicator)
        @Nullable val date: TextView? = itemView.findViewById(R.id.date)
    }

    interface OnListInteractionListener {
        fun onListItemTapped(@NonNull message: IterableInAppMessage)
        fun onListItemDeleted(@NonNull message: IterableInAppMessage, source: IterableInAppDeleteActionType)
        fun onListItemImpressionStarted(@NonNull message: IterableInAppMessage)
        fun onListItemImpressionEnded(@NonNull message: IterableInAppMessage)
    }

    /**
     * Since [IterableInAppMessage] is a mutable object, we transform it into a separate
     * immutable object to be able to run DiffUtil on it. Otherwise, we wouldn't be able to figure
     * out if an inbox message was changed.
     */
    private class InboxRow(inboxMessage: IterableInAppMessage) {
        val message: IterableInAppMessage = inboxMessage
        val inboxMetadata: IterableInAppMessage.InboxMetadata = inboxMessage.inboxMetadata
        val isRead: Boolean = inboxMessage.isRead()
        val createdAt: Date = inboxMessage.createdAt

        override fun equals(other: Any?): Boolean {
            if (other === this) {
                return true
            }
            if (other !is InboxRow) {
                return false
            }
            return message === other.message &&
                    ObjectsCompat.equals(inboxMetadata, other.inboxMetadata) &&
                    ObjectsCompat.equals(isRead, other.isRead) &&
                    ObjectsCompat.equals(createdAt, other.createdAt)
        }

        override fun hashCode(): Int {
            return ObjectsCompat.hash(message, inboxMetadata, isRead, createdAt)
        }
    }

    private fun inboxRowListFromInboxMessages(messages: List<IterableInAppMessage>): List<InboxRow> {
        val inboxRows = ArrayList<InboxRow>(messages.size)
        for (message in messages) {
            if (filter.filter(message)) {
                inboxRows.add(InboxRow(message))
            }
        }
        inboxRows.sortWith { o1, o2 ->
            comparator.compare(o1.message, o2.message)
        }
        return inboxRows
    }

    private class InAppMessageDiffCallback(
        private val oldList: List<InboxRow>,
        private val newList: List<InboxRow>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.message.messageId == newItem.message.messageId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem == newItem
        }
    }

}