package com.iterable.iterableapi.testapp;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import com.iterable.iterableapi.ui.R;
import com.iterable.iterableapi.ui.inbox.InboxActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class InboxUITest {
    @Rule
    public ActivityTestRule rule = new ActivityTestRule<>(InboxActivity.class);

    @Test
    public void basicTest() {
        onView(withId(R.id.list)).perform(click());
        assertNotNull(rule.getActivity());
    }
}
