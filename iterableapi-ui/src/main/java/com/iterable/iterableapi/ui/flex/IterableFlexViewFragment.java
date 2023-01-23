package com.iterable.iterableapi.ui.flex;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.iterable.iterableapi.IterableApi;
import com.iterable.iterableapi.IterableConstants;
import com.iterable.iterableapi.IterableLogger;
import com.iterable.iterableapi.ui.R;
import com.iterable.iterableapi.ui.inbox.InboxMode;
import com.iterable.iterableapi.ui.inbox.IterableInboxAdapter;
import com.iterable.iterableapi.ui.inbox.IterableInboxFragment;
import com.iterable.iterableapi.ui.inbox.IterableInboxTouchHelper;

public class IterableFlexViewFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //IterableLogger.printInfo();

        RelativeLayout relativeLayout = (RelativeLayout) inflater.inflate(R.layout.iterable_flex_view_fragment, container, false);

//        noMessagesTitleTextView = relativeLayout.findViewById(R.id.emptyInboxTitle);
//        noMessagesBodyTextView = relativeLayout.findViewById(R.id.emptyInboxMessage);
//        noMessagesTitleTextView.setText(noMessagesTitle);
//        noMessagesBodyTextView.setText(noMessagesBody);

//        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new IterableInboxTouchHelper(getContext(), adapter));
//        itemTouchHelper.attachToRecyclerView(recyclerView);

        return relativeLayout;
    }
}
