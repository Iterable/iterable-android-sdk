package com.iterable.iterableapi.ui.inbox;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.iterable.iterableapi.IterableLogger;
import com.iterable.iterableapi.ui.R;

import static com.iterable.iterableapi.ui.inbox.IterableInboxFragment.INBOX_MODE;

public class IterableInboxActivity extends AppCompatActivity {
    static final String TAG = "IterableInboxActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IterableLogger.printInfo();
        setContentView(R.layout.inbox_activity);
        IterableInboxFragment inboxFragment;

        if (getIntent() != null) {
            Object inboxMode = getIntent().getSerializableExtra(INBOX_MODE);
            if ((inboxMode instanceof InboxMode)) {
                inboxFragment = IterableInboxFragment.newInstance((InboxMode) inboxMode);
            } else {
                inboxFragment = IterableInboxFragment.newInstance();
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
