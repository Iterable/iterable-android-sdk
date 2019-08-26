package com.iterable.iterableapi.ui.inbox;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.iterable.iterableapi.IterableInAppDeleteSource;
import com.iterable.iterableapi.IterableInAppMessage;
import com.iterable.iterableapi.ui.BitmapLoader;
import com.iterable.iterableapi.ui.R;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class InboxRecyclerViewAdapter extends RecyclerView.Adapter<InboxRecyclerViewAdapter.ViewHolder> {

    private List<IterableInAppMessage> values;
    private OnListInteractionListener listener;

    public InboxRecyclerViewAdapter(List<IterableInAppMessage> values, OnListInteractionListener listener) {
        this.values = values;
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
        IterableInAppMessage inboxMessage = values.get(position);
        IterableInAppMessage.InboxMetadata inboxMetadata = inboxMessage.getInboxMetadata();
        holder.title.setText(inboxMetadata.title);
        holder.subtitle.setText(inboxMetadata.subtitle);
        BitmapLoader.loadBitmap(holder.icon, Uri.parse(inboxMetadata.icon));
        if (inboxMessage.isRead()) {
            holder.unreadIndicator.setVisibility(View.INVISIBLE);
        } else {
            holder.unreadIndicator.setVisibility(View.VISIBLE);
        }
        holder.date.setText(formatDate(inboxMessage.getCreatedAt()));
        holder.itemView.setTag(inboxMessage);
        holder.itemView.setOnClickListener(onClickListener);
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    public void setValues(List<IterableInAppMessage> newValues) {
        InAppMessageDiffCallback diffCallback = new InAppMessageDiffCallback(values, newValues);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        values.clear();
        values.addAll(newValues);
        diffResult.dispatchUpdatesTo(this);
    }

    public void deleteItem(int position, IterableInAppDeleteSource source) {
        IterableInAppMessage deletedItem = values.get(position);
        values.remove(position);
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
        void onListItemDeleted(IterableInAppMessage message, IterableInAppDeleteSource source);
    }

    private String formatDate(Date date) {
        if (date != null) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            return formatter.format(date);
        } else {
            return "";
        }
    }

    private static class InAppMessageDiffCallback extends DiffUtil.Callback {

        private final List<IterableInAppMessage> oldList;
        private final List<IterableInAppMessage> newList;

        private InAppMessageDiffCallback(List<IterableInAppMessage> oldList, List<IterableInAppMessage> newList) {
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
            IterableInAppMessage oldItem = oldList.get(oldItemPosition);
            IterableInAppMessage newItem = newList.get(newItemPosition);
            return oldItem.getMessageId().equals(newItem.getMessageId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            IterableInAppMessage oldItem = oldList.get(oldItemPosition);
            IterableInAppMessage newItem = newList.get(newItemPosition);
            return oldItem.equals(newItem);
        }
    }

}
