package com.iterable.iterableapi.ui.inbox;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
        values = newValues;
        notifyDataSetChanged();
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
    }

    private String formatDate(Date date) {
        if (date != null) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            return formatter.format(date);
        } else {
            return "";
        }
    }

}
