package com.iterable.iterableapi.ui.inbox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.iterable.iterableapi.ui.R;

public class InboxRecyclerViewTouchHelper extends ItemTouchHelper.SimpleCallback {
    private final Drawable icon;
    private final InboxRecyclerViewAdapter adapter;
    private final ColorDrawable background;

    public InboxRecyclerViewTouchHelper(Context context, InboxRecyclerViewAdapter adapter) {
        super(0,ItemTouchHelper.LEFT);
        this.adapter = adapter;
        this.icon = ContextCompat.getDrawable(context, R.drawable.ic_delete_black_24dp);
        this.background = new ColorDrawable(Color.RED);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        adapter.deleteItem(position);
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        View itemView = viewHolder.itemView;
        background.setBounds(itemView.getRight() + ((int) dX),
                itemView.getTop(), itemView.getRight(), itemView.getBottom());

        int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
        int iconBottom = iconTop + icon.getIntrinsicHeight();

        int iconLeft = itemView.getRight() - icon.getIntrinsicWidth() * 2;
        int iconRight = itemView.getRight() - icon.getIntrinsicWidth() ;
        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

        background.setBounds(itemView.getRight() + ((int) dX),
                itemView.getTop(), itemView.getRight(), itemView.getBottom());

        background.draw(c);
        icon.draw(c);
    }
}
