package com.iterable.iterableapi.ui.inbox;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.iterable.iterableapi.IterableLogger;
import com.iterable.iterableapi.ui.R;

public class InboxActivity extends AppCompatActivity {

    static final String TAG = "InboxActivity";
    public static String INBOX_MODE = "inboxMode";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        IterableLogger.printInfo();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inbox_activity);
        InboxFragment inboxFragment;

        if (getIntent() != null) {
            if (((getIntent().getSerializableExtra(INBOX_MODE) instanceof InboxMode))) {
                inboxFragment = InboxFragment.newInstance((InboxMode) getIntent().getSerializableExtra(INBOX_MODE));
            } else {
                inboxFragment = InboxFragment.newInstance();
            }
        } else {
            inboxFragment = InboxFragment.newInstance();
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, inboxFragment)
                    .commitNow();
        }
    }


}
