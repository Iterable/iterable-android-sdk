package com.iterable.iterableapi.ui.inbox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.View;

import com.iterable.iterableapi.messaging.inapp.enums.IterableInAppDeleteActionType;
import com.iterable.iterableapi.ui.R;

public class IterableInboxTouchHelper extends ItemTouchHelper.SimpleCallback {
    private final Drawable icon;
    private final IterableInboxAdapter adapter;
    private final ColorDrawable background;

    public IterableInboxTouchHelper(@NonNull Context context, @NonNull IterableInboxAdapter adapter) {
        super(0, ItemTouchHelper.LEFT);
        this.adapter = adapter;
        this.icon = ContextCompat.getDrawable(context, R.drawable.ic_delete_black_24dp);
        this.background = new ColorDrawable(Color.RED);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        adapter.deleteItem(position, IterableInAppDeleteActionType.INBOX_SWIPE);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        View itemView = viewHolder.itemView;
        background.setBounds(itemView.getRight() + ((int) dX),
                itemView.getTop(), itemView.getRight(), itemView.getBottom());

        int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
        int iconBottom = iconTop + icon.getIntrinsicHeight();

        int iconLeft = itemView.getRight() - icon.getIntrinsicWidth() * 2;
        int iconRight = itemView.getRight() - icon.getIntrinsicWidth();
        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

        background.setBounds(itemView.getRight() + ((int) dX),
                itemView.getTop(), itemView.getRight(), itemView.getBottom());

        background.draw(c);
        icon.draw(c);
    }
}
