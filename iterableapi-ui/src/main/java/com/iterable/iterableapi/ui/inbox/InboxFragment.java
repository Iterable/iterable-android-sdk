package com.iterable.iterableapi.ui.inbox;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.iterable.iterableapi.IterableApi;
import com.iterable.iterableapi.IterableInAppDeleteSource;
import com.iterable.iterableapi.IterableInAppLocation;
import com.iterable.iterableapi.IterableInAppManager;
import com.iterable.iterableapi.IterableInAppMessage;
import com.iterable.iterableapi.ui.R;

public class InboxFragment extends Fragment implements IterableInAppManager.Listener, InboxRecyclerViewAdapter.OnListInteractionListener {

    public static InboxFragment newInstance() {
        return new InboxFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerView view = (RecyclerView) inflater.inflate(R.layout.fragment_inbox_list, container, false);
        view.setLayoutManager(new LinearLayoutManager(getContext()));
        InboxRecyclerViewAdapter adapter = new InboxRecyclerViewAdapter(IterableApi.getInstance().getInAppManager().getInboxMessages(), InboxFragment.this);
        view.setAdapter(adapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new InboxRecyclerViewTouchHelper(getContext(), adapter));
        itemTouchHelper.attachToRecyclerView(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateList();
        IterableApi.getInstance().getInAppManager().addListener(this);
    }

    @Override
    public void onPause() {
        IterableApi.getInstance().getInAppManager().removeListener(this);
        super.onPause();
    }

    private void updateList() {
        RecyclerView recyclerView = (RecyclerView) getView();
        InboxRecyclerViewAdapter adapter = (InboxRecyclerViewAdapter) recyclerView.getAdapter();
        adapter.setValues(IterableApi.getInstance().getInAppManager().getInboxMessages());
    }

    @Override
    public void onInboxUpdated() {
        updateList();
    }

    @Override
    public void onListItemTapped(IterableInAppMessage message) {
        IterableApi.getInstance().getInAppManager().setRead(message, true);
        startActivity(new Intent(getContext(), InboxMessageActivity.class).putExtra(InboxMessageActivity.ARG_MESSAGE_ID, message.getMessageId()));
    }

    @Override
    public void onListItemDeleted(IterableInAppMessage message, IterableInAppDeleteSource source) {
        IterableApi.getInstance().getInAppManager().removeMessage(message, source, IterableInAppLocation.INBOX);
    }
}
