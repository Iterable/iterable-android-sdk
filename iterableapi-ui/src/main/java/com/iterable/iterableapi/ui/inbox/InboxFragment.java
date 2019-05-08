package com.iterable.iterableapi.ui.inbox;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.iterable.iterableapi.IterableApi;
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
        view.setAdapter(new InboxRecyclerViewAdapter(IterableApi.getInstance().getInAppManager().getInboxMessages(), InboxFragment.this));
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
}
