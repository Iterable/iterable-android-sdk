package com.iterable.iterableapi.ui.inbox

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import android.view.View

import com.iterable.iterableapi.IterableInAppDeleteActionType
import com.iterable.iterableapi.ui.R

class IterableInboxTouchHelper(
    @NonNull context: Context,
    @NonNull private val adapter: IterableInboxAdapter
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
    
    private val icon: Drawable?
    private val background: ColorDrawable

    init {
        this.icon = ContextCompat.getDrawable(context, R.drawable.ic_delete_black_24dp)
        this.background = ColorDrawable(Color.RED)
    }

    override fun onMove(@NonNull recyclerView: RecyclerView, @NonNull viewHolder: RecyclerView.ViewHolder, @NonNull target: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onSwiped(@NonNull viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        adapter.deleteItem(position, IterableInAppDeleteActionType.INBOX_SWIPE)
    }

    override fun onChildDraw(@NonNull c: Canvas, @NonNull recyclerView: RecyclerView, @NonNull viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        val itemView = viewHolder.itemView
        background.setBounds(itemView.right + dX.toInt(),
                itemView.top, itemView.right, itemView.bottom)

        icon?.let { iconDrawable ->
            val iconTop = itemView.top + (itemView.height - iconDrawable.intrinsicHeight) / 2
            val iconBottom = iconTop + iconDrawable.intrinsicHeight

            val iconLeft = itemView.right - iconDrawable.intrinsicWidth * 2
            val iconRight = itemView.right - iconDrawable.intrinsicWidth
            iconDrawable.setBounds(iconLeft, iconTop, iconRight, iconBottom)

            background.setBounds(itemView.right + dX.toInt(),
                    itemView.top, itemView.right, itemView.bottom)

            background.draw(c)
            iconDrawable.draw(c)
        }
    }
}
