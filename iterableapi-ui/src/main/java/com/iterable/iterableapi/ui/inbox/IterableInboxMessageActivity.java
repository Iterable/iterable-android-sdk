package com.iterable.iterableapi.ui.inbox;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.iterable.iterableapi.IterableLogger;
import com.iterable.iterableapi.ui.R;

public class IterableInboxMessageActivity extends AppCompatActivity {
    public static final String ARG_MESSAGE_ID = "messageId";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.iterable_inbox_message_activity);
        IterableLogger.printInfo();
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, IterableInboxMessageFragment.newInstance(getIntent().getStringExtra(ARG_MESSAGE_ID)))
                    .commitNow();
        }
    }
}
