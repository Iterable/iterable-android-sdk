package com.iterable.iterableapi.ui.inbox;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.iterable.iterableapi.IterableLogger;
import com.iterable.iterableapi.ui.R;

import static com.iterable.iterableapi.ui.inbox.IterableInboxFragment.INBOX_MODE;
import static com.iterable.iterableapi.ui.inbox.IterableInboxFragment.ITEM_LAYOUT_ID;

/**
 * An activity wrapping {@link IterableInboxFragment}
 * <p>
 * Supports optional extras:
 * {@link IterableInboxFragment#INBOX_MODE} - {@link InboxMode} value with the inbox mode
 * {@link IterableInboxFragment#ITEM_LAYOUT_ID} - Layout resource id for inbox items
 */
public class IterableInboxActivity extends AppCompatActivity {
    static final String TAG = "IterableInboxActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IterableLogger.printInfo();
        setContentView(R.layout.inbox_activity);
        IterableInboxFragment inboxFragment;

        if (getIntent() != null) {
            Object inboxModeExtra = getIntent().getSerializableExtra(INBOX_MODE);
            int itemLayoutId = getIntent().getIntExtra(ITEM_LAYOUT_ID, 0);
            InboxMode inboxMode = InboxMode.POPUP;
            if (inboxModeExtra instanceof InboxMode) {
                inboxMode = (InboxMode) inboxModeExtra;
            }
            inboxFragment = IterableInboxFragment.newInstance(inboxMode, itemLayoutId);
        } else {
            inboxFragment = IterableInboxFragment.newInstance();
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, inboxFragment)
                    .commitNow();
        }
    }

}
