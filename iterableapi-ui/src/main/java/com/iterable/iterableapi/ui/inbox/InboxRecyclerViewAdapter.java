package com.iterable.iterableapi.ui.inbox;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.util.ObjectsCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.iterable.iterableapi.IterableInAppDeleteActionType;
import com.iterable.iterableapi.IterableInAppMessage;
import com.iterable.iterableapi.ui.BitmapLoader;
import com.iterable.iterableapi.ui.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InboxRecyclerViewAdapter extends RecyclerView.Adapter<InboxRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "InboxRecyclerViewAdapter";

    private List<InboxRow> inboxItems;
    private OnListInteractionListener listener;

    public InboxRecyclerViewAdapter(List<IterableInAppMessage> values, OnListInteractionListener listener) {
        this.inboxItems = inboxRowListFromInboxMessages(values);
        this.listener = listener;
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            IterableInAppMessage inboxMessage = (IterableInAppMessage) v.getTag();
            listener.onListItemTapped(inboxMessage);
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_inbox_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InboxRow inboxRow = inboxItems.get(position);
        IterableInAppMessage.InboxMetadata inboxMetadata = inboxRow.inboxMetadata;
        holder.title.setText(inboxMetadata.title);
        holder.subtitle.setText(inboxMetadata.subtitle);
        BitmapLoader.loadBitmap(holder.icon, Uri.parse(inboxMetadata.icon));
        if (inboxRow.isRead) {
            holder.unreadIndicator.setVisibility(View.INVISIBLE);
        } else {
            holder.unreadIndicator.setVisibility(View.VISIBLE);
        }
        holder.date.setText(formatDate(inboxRow.createdAt));
        holder.itemView.setTag(inboxRow.message);
        holder.itemView.setOnClickListener(onClickListener);
    }

    @Override
    public int getItemCount() {
        return inboxItems.size();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        IterableInAppMessage message = (IterableInAppMessage) holder.itemView.getTag();
        listener.onListItemImpressionStarted(message);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        IterableInAppMessage message = (IterableInAppMessage) holder.itemView.getTag();
        listener.onListItemImpressionEnded(message);
    }

    public void setInboxItems(List<IterableInAppMessage> newValues) {
        List<InboxRow> newRowValues = inboxRowListFromInboxMessages(newValues);
        InAppMessageDiffCallback diffCallback = new InAppMessageDiffCallback(inboxItems, newRowValues);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        inboxItems.clear();
        inboxItems.addAll(newRowValues);
        diffResult.dispatchUpdatesTo(this);
    }

    public void deleteItem(int position, IterableInAppDeleteActionType source) {
        IterableInAppMessage deletedItem = inboxItems.get(position).message;
        inboxItems.remove(position);
        listener.onListItemDeleted(deletedItem, source);
        notifyItemRemoved(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView subtitle;
        private final TextView date;
        private final ImageView icon;
        private final ImageView unreadIndicator;

        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);
            icon = itemView.findViewById(R.id.imageView);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            date = itemView.findViewById(R.id.date);
        }
    }

    interface OnListInteractionListener {
        void onListItemTapped(IterableInAppMessage message);
        void onListItemDeleted(IterableInAppMessage message, IterableInAppDeleteActionType source);
        void onListItemImpressionStarted(IterableInAppMessage message);
        void onListItemImpressionEnded(IterableInAppMessage message);
    }

    private String formatDate(Date date) {
        if (date != null) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            return formatter.format(date);
        } else {
            return "";
        }
    }

    /**
     * Since {@link IterableInAppMessage} is a mutable object, we transform it into a separate
     * immutable object to be able to run DiffUtil on it. Otherwise, we wouldn't be able to figure
     * out if an inbox message was changed.
     */
    private static class InboxRow {
        private final IterableInAppMessage message;
        private final IterableInAppMessage.InboxMetadata inboxMetadata;
        private final boolean isRead;
        private final Date createdAt;

        private InboxRow(IterableInAppMessage inboxMessage) {
            this.message = inboxMessage;
            this.inboxMetadata = inboxMessage.getInboxMetadata();
            this.isRead = inboxMessage.isRead();
            this.createdAt = inboxMessage.getCreatedAt();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof InboxRow)) {
                return false;
            }
            InboxRow inboxRow = (InboxRow) obj;
            return message == inboxRow.message &&
                    ObjectsCompat.equals(inboxMetadata, inboxRow.inboxMetadata) &&
                    ObjectsCompat.equals(isRead, inboxRow.isRead) &&
                    ObjectsCompat.equals(createdAt, inboxRow.createdAt);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(message, inboxMetadata, isRead, createdAt);
        }
    }

    private List<InboxRow> inboxRowListFromInboxMessages(List<IterableInAppMessage> messages) {
        ArrayList<InboxRow> inboxRows = new ArrayList<>(messages.size());
        for (IterableInAppMessage message : messages) {
            inboxRows.add(new InboxRow(message));
        }
        return inboxRows;
    }

    private static class InAppMessageDiffCallback extends DiffUtil.Callback {

        private final List<InboxRow> oldList;
        private final List<InboxRow> newList;

        private InAppMessageDiffCallback(List<InboxRow> oldList, List<InboxRow> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            InboxRow oldItem = oldList.get(oldItemPosition);
            InboxRow newItem = newList.get(newItemPosition);
            return oldItem.message.getMessageId().equals(newItem.message.getMessageId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            InboxRow oldItem = oldList.get(oldItemPosition);
            InboxRow newItem = newList.get(newItemPosition);
            return oldItem.equals(newItem);
        }
    }

}
