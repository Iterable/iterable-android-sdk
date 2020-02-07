package com.iterable.iterableapi.ui.inbox;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.iterable.iterableapi.IterableInAppMessage;

/**
 * Inbox adapter extension interface
 *
 * @param <VH> The class for your ViewHolder extension. Use this to store references to views in
 *             your custom layout, similar to how you would use a RecyclerView.ViewHolder.
 */
public interface IterableInboxAdapterExtension<VH> {
    /**
     * Return the item view type of the item for the given message.
     * See {@link RecyclerView.Adapter#getItemViewType(int)}
     *
     * @param message Inbox message
     * @return Integer value identifying the type of the view needed to represent the item
     */
    int getItemViewType(@NonNull IterableInAppMessage message);

    /**
     * Return the layout resource id for a given view type
     *
     * @param viewType View type of an item to be displayed
     * @return Layout resource id for the view type. Must be a valid resource id.
     */
    @LayoutRes int getLayoutForViewType(int viewType);

    /**
     * Create a view holder extension
     * This method is run after the default implementation at {@link IterableInboxAdapter#onCreateViewHolder(ViewGroup, int)}
     *
     * @param view A View inflated from the layout id specified in {@link #getLayoutForViewType(int)}
     * @param viewType View type of an item
     * @return A view holder extension object, or null. Use this to store references to views in
     *         your custom layout, similar to how you would use a RecyclerView.ViewHolder.
     */
    @Nullable
    VH createViewHolderExtension(@NonNull View view, int viewType);

    /**
     * Called by the adapter to display the data for the specified Inbox message. The method should
     * update the contents of the item view to reflect the contents of the Inbox message.
     * This method is run after the default implementation at {@link IterableInboxAdapter#onBindViewHolder(IterableInboxAdapter.ViewHolder, int)}
     *
     * @param viewHolder The default view holder with references to standard fields: title, subtitle, etc.
     * @param holderExtension The holder extension object created in {@link #createViewHolderExtension(View, int)}, or null
     * @param message Inbox message
     */
    void onBindViewHolder(@NonNull IterableInboxAdapter.ViewHolder viewHolder, @Nullable VH holderExtension, @NonNull IterableInAppMessage message);
}
