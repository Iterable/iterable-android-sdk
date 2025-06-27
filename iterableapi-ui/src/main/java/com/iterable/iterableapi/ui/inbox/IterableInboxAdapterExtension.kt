package com.iterable.iterableapi.ui.inbox

import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup

import com.iterable.iterableapi.IterableInAppMessage

/**
 * Inbox adapter extension interface
 *
 * @param <VH> The class for your ViewHolder extension. Use this to store references to views in
 *             your custom layout, similar to how you would use a RecyclerView.ViewHolder.
 */
interface IterableInboxAdapterExtension<VH> {
    /**
     * Return the item view type of the item for the given message.
     * See [RecyclerView.Adapter.getItemViewType]
     *
     * @param message Inbox message
     * @return Integer value identifying the type of the view needed to represent the item
     */
    fun getItemViewType(@NonNull message: IterableInAppMessage): Int

    /**
     * Return the layout resource id for a given view type
     *
     * @param viewType View type of an item to be displayed
     * @return Layout resource id for the view type. Must be a valid resource id.
     */
    @LayoutRes
    fun getLayoutForViewType(viewType: Int): Int

    /**
     * Create a view holder extension
     * This method is run after the default implementation at [IterableInboxAdapter.onCreateViewHolder]
     *
     * @param view A View inflated from the layout id specified in [getLayoutForViewType]
     * @param viewType View type of an item
     * @return A view holder extension object, or null. Use this to store references to views in
     *         your custom layout, similar to how you would use a RecyclerView.ViewHolder.
     */
    @Nullable
    fun createViewHolderExtension(@NonNull view: View, viewType: Int): VH?

    /**
     * Called by the adapter to display the data for the specified Inbox message. The method should
     * update the contents of the item view to reflect the contents of the Inbox message.
     * This method is run after the default implementation at [IterableInboxAdapter.onBindViewHolder]
     *
     * @param viewHolder The default view holder with references to standard fields: title, subtitle, etc.
     * @param holderExtension The holder extension object created in [createViewHolderExtension], or null
     * @param message Inbox message
     */
    fun onBindViewHolder(@NonNull viewHolder: IterableInboxAdapter.ViewHolder, @Nullable holderExtension: VH?, @NonNull message: IterableInAppMessage)
}
