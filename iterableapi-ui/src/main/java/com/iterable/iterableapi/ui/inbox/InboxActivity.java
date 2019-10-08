package com.iterable.iterableapi.ui.inbox;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.iterable.iterableapi.IterableLogger;
import com.iterable.iterableapi.ui.R;

public class InboxActivity extends AppCompatActivity {

    private InboxMode mode = InboxMode.POPUP;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inbox_activity);
        IterableLogger.printInfo();


        //Setting necessary parameters
        InboxFragment inboxFragment = new InboxFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("InboxMode", mode);
        inboxFragment.setArguments(bundle);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, inboxFragment)
                    .commitNow();
        }
    }

    public InboxMode getMode() {
        return mode;
    }

    public void setMode(InboxMode mode) {
        this.mode = mode;
    }

}
