package com.iterable.iterableapi.ui.inbox;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.iterable.iterableapi.IterableConstants;
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
 * <p>
 * To customize the toolbar header, use {@link #setInboxCustomization(IterableInboxCustomization)}
 * or set customization via intent extras:
 * {@link #EXTRA_TITLE_CENTER_ALIGNED} - boolean to center-align the title
 * {@link #EXTRA_SHOW_CLOSE_BUTTON} - boolean to show a close button
 */
public class IterableInboxActivity extends AppCompatActivity {
    private static final String TAG = "IterableInboxActivity";
    public static final String ACTIVITY_TITLE = "activityTitle";
    public static final String EXTRA_TITLE_CENTER_ALIGNED = "iterableTitleCenterAligned";
    public static final String EXTRA_SHOW_CLOSE_BUTTON = "iterableShowCloseButton";

    @Nullable
    private static IterableInboxCustomization pendingCustomization;

    /**
     * Set toolbar customization to be applied when the activity is created.
     * This must be called before starting the activity. The customization is consumed
     * once applied and will not persist across activity recreations.
     *
     * @param customization The customization to apply
     */
    public static void setInboxCustomization(@Nullable IterableInboxCustomization customization) {
        pendingCustomization = customization;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IterableLogger.printInfo();
        setContentView(R.layout.iterable_inbox_activity);

        Toolbar toolbar = findViewById(R.id.iterable_inbox_toolbar);
        setSupportActionBar(toolbar);

        IterableInboxFragment inboxFragment;

        Intent intent = getIntent();
        String titleText = null;
        boolean titleCenterAligned = false;
        boolean showCloseButton = false;

        if (intent != null) {
            Object inboxModeExtra = intent.getSerializableExtra(INBOX_MODE);
            int itemLayoutId = intent.getIntExtra(ITEM_LAYOUT_ID, 0);
            InboxMode inboxMode = InboxMode.POPUP;
            if (inboxModeExtra instanceof InboxMode) {
                inboxMode = (InboxMode) inboxModeExtra;
            }
            String noMessageTitle = null;
            String noMessageBody = null;
            Bundle extraBundle = getIntent().getExtras();
            if (extraBundle != null) {
                noMessageTitle = extraBundle.getString(IterableConstants.NO_MESSAGES_TITLE, null);
                noMessageBody = extraBundle.getString(IterableConstants.NO_MESSAGES_BODY, null);
            }
            inboxFragment = IterableInboxFragment.newInstance(inboxMode, itemLayoutId, noMessageTitle, noMessageBody);

            if (intent.getStringExtra(ACTIVITY_TITLE) != null) {
                titleText = intent.getStringExtra(ACTIVITY_TITLE);
            }

            titleCenterAligned = intent.getBooleanExtra(EXTRA_TITLE_CENTER_ALIGNED, false);
            showCloseButton = intent.getBooleanExtra(EXTRA_SHOW_CLOSE_BUTTON, false);
        } else {
            inboxFragment = IterableInboxFragment.newInstance();
        }

        // Apply programmatic customization (takes precedence over intent extras)
        IterableInboxCustomization customization = pendingCustomization;
        pendingCustomization = null;

        if (customization != null) {
            if (customization.getTitle() != null) {
                titleText = customization.getTitle();
            }
            titleCenterAligned = customization.isTitleCenterAligned();
            showCloseButton = customization.isShowCloseButton();
        }

        // Configure toolbar
        configureToolbar(toolbar, titleText, titleCenterAligned, showCloseButton, customization);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, inboxFragment)
                    .commitNow();
        }
    }

    private void configureToolbar(Toolbar toolbar, @Nullable String titleText, boolean titleCenterAligned, boolean showCloseButton, @Nullable IterableInboxCustomization customization) {
        if (titleCenterAligned) {
            // Use the custom centered TextView instead of the default title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
            TextView centeredTitle = findViewById(R.id.iterable_inbox_toolbar_title);
            centeredTitle.setVisibility(android.view.View.VISIBLE);
            centeredTitle.setText(titleText != null ? titleText : "Inbox");

            // Center the TextView in the toolbar
            Toolbar.LayoutParams params = new Toolbar.LayoutParams(
                    Toolbar.LayoutParams.WRAP_CONTENT,
                    Toolbar.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
            );
            centeredTitle.setLayoutParams(params);
        } else if (titleText != null) {
            setTitle(titleText);
        }

        if (showCloseButton) {
            toolbar.setNavigationIcon(R.drawable.ic_close_black_24dp);
            toolbar.setNavigationContentDescription("Close");
            toolbar.setNavigationOnClickListener(v -> {
                if (customization != null && customization.getCloseButtonListener() != null) {
                    customization.getCloseButtonListener().onClick(v);
                } else {
                    finish();
                }
            });
        }
    }
}
