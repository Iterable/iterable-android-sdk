package com.iterable.iterableapi.ui.inbox;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.iterable.iterableapi.IterableInAppDeleteActionType;
import com.iterable.iterableapi.IterableInAppMessage;
import com.iterable.iterableapi.ui.BitmapLoader;
import com.iterable.iterableapi.ui.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class IterableInboxAdapter extends RecyclerView.Adapter<IterableInboxAdapter.ViewHolder> {

    private static final String TAG = "IterableInboxAdapter";

    private final @NonNull OnListInteractionListener listener;
    private final @NonNull IterableInboxAdapterExtension extension;
    private final @NonNull IterableInboxComparator comparator;
    private final @NonNull IterableInboxFilter filter;
    private final @NonNull IterableInboxDateMapper dateMapper;

    private List<InboxRow> inboxItems;

    IterableInboxAdapter(@NonNull List<IterableInAppMessage> values, @NonNull OnListInteractionListener listener, @NonNull IterableInboxAdapterExtension extension, @NonNull IterableInboxComparator comparator, @NonNull IterableInboxFilter filter, @NonNull IterableInboxDateMapper dateMapper) {
        this.listener = listener;
        this.extension = extension;
        this.comparator = comparator;
        this.filter = filter;
        this.inboxItems = inboxRowListFromInboxMessages(values);
        this.dateMapper = dateMapper;
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            IterableInAppMessage inboxMessage = (IterableInAppMessage) v.getTag();
            listener.onListItemTapped(inboxMessage);
        }
    };

    @Override
    public int getItemViewType(int position) {
        return extension.getItemViewType(inboxItems.get(position).message);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(extension.getLayoutForViewType(viewType), parent, false);
        return new ViewHolder(view, extension.createViewHolderExtension(view, viewType));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InboxRow inboxRow = inboxItems.get(position);
        IterableInAppMessage.InboxMetadata inboxMetadata = inboxRow.inboxMetadata;

        if (holder.title != null) {
            holder.title.setText(inboxMetadata.title);
        }

        if (holder.subtitle != null) {
            holder.subtitle.setText(inboxMetadata.subtitle);
        }

        if (holder.icon != null) {
            BitmapLoader.loadBitmap(holder.icon, Uri.parse(inboxMetadata.icon));
        }

        if (holder.unreadIndicator != null) {
            if (inboxRow.isRead) {
                holder.unreadIndicator.setVisibility(View.INVISIBLE);
            } else {
                holder.unreadIndicator.setVisibility(View.VISIBLE);
            }
        }

        if (holder.date != null) {
            holder.date.setText(dateMapper.mapMessageToDateString(inboxRow.message));
        }

        holder.itemView.setTag(inboxRow.message);
        holder.itemView.setOnClickListener(onClickListener);
        extension.onBindViewHolder(holder, holder.extension, inboxRow.message);
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

    public void setInboxItems(@NonNull List<IterableInAppMessage> newValues) {
        List<InboxRow> newRowValues = inboxRowListFromInboxMessages(newValues);
        InAppMessageDiffCallback diffCallback = new InAppMessageDiffCallback(inboxItems, newRowValues);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        inboxItems.clear();
        inboxItems.addAll(newRowValues);
        diffResult.dispatchUpdatesTo(this);
    }

    public void deleteItem(int position, @NonNull IterableInAppDeleteActionType source) {
        IterableInAppMessage deletedItem = inboxItems.get(position).message;
        inboxItems.remove(position);
        listener.onListItemDeleted(deletedItem, source);
        notifyItemRemoved(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final @Nullable TextView title;
        public final @Nullable TextView subtitle;
        public final @Nullable TextView date;
        public final @Nullable ImageView icon;
        public final @Nullable ImageView unreadIndicator;
        private Object extension;

        private ViewHolder(View itemView, Object extension) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);
            icon = itemView.findViewById(R.id.imageView);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            date = itemView.findViewById(R.id.date);
            this.extension = extension;
        }
    }

    interface OnListInteractionListener {
        void onListItemTapped(@NonNull IterableInAppMessage message);
        void onListItemDeleted(@NonNull IterableInAppMessage message, IterableInAppDeleteActionType source);
        void onListItemImpressionStarted(@NonNull IterableInAppMessage message);
        void onListItemImpressionEnded(@NonNull IterableInAppMessage message);
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
            if (filter.filter(message)) {
                inboxRows.add(new InboxRow(message));
            }
        }
        Collections.sort(inboxRows, (o1, o2) -> comparator.compare(o1.message, o2.message));
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