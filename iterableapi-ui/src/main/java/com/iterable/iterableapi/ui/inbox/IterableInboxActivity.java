package com.iterable.iterableapi.ui.inbox;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
    private static final String TAG = "IterableInboxActivity";
    public static final String ACTIVITY_TITLE = "activityTitle";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IterableLogger.printInfo();
        setContentView(R.layout.iterable_inbox_activity);
        IterableInboxFragment inboxFragment;

        Intent intent = getIntent();
        if (intent != null) {
            Object inboxModeExtra = intent.getSerializableExtra(INBOX_MODE);
            int itemLayoutId = intent.getIntExtra(ITEM_LAYOUT_ID, 0);
            InboxMode inboxMode = InboxMode.POPUP;
            if (inboxModeExtra instanceof InboxMode) {
                inboxMode = (InboxMode) inboxModeExtra;
            }
            inboxFragment = IterableInboxFragment.newInstance(inboxMode, itemLayoutId);

            if (intent.getStringExtra(ACTIVITY_TITLE) != null) {
                setTitle(intent.getStringExtra(ACTIVITY_TITLE));
            }
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
