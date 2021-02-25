package com.iterable.iterableapi.testapp;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import com.iterable.iterableapi.IterableApi;
import com.iterable.iterableapi.ui.R;
import com.iterable.iterableapi.ui.inbox.IterableInboxActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class InboxUITest {
    @Rule
    public ActivityTestRule<IterableInboxActivity> rule = new ActivityTestRule<IterableInboxActivity>(IterableInboxActivity.class) {
        @Override
        protected void beforeActivityLaunched() {
            super.beforeActivityLaunched();
            IterableApi.initialize(getApplicationContext(), "apiKey");
        }
    };

    @Test
    public void basicTest() {
        assertNotNull(rule.getActivity());
    }
}
