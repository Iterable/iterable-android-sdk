package com.iterable.iterableapi;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import com.iterable.iterableapi.testapp.PathBasedQueueDispatcher;
import com.iterable.iterableapi.ui.inbox.InboxActivity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.mockito.Mockito.mock;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private MockWebServer server;
    private PathBasedQueueDispatcher dispatcher;
    private IterableInAppHandler inAppHandler;
    private IterableCustomActionHandler customActionHandler;
    private IterableUrlHandler urlHandler;
    @Rule
    public ActivityTestRule rule = new ActivityTestRule<>(InboxActivity.class, false, false);

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        dispatcher = new PathBasedQueueDispatcher();
        server.setDispatcher(dispatcher);
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        IterableApi.sharedInstance = new IterableApi();
        IterableTestUtils.createIterableApiNew(new IterableTestUtils.ConfigBuilderExtender() {
            @Override
            public IterableConfig.Builder run(IterableConfig.Builder builder) {
                return builder;
            }
        });
        IterableApi.getInstance().getInAppManager().syncInApp();
    }

    @Test
    public void checkInboxFragmentCount() throws IOException {
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(IterableTestUtils.getResourceString("inapp_payload_inbox_multiple.json")));
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();

        rule.launchActivity(null);
        inAppManager.syncInApp();

        onView(withId(com.iterable.iterableapi.ui.R.id.list)).check(matches(isDisplayed()));
        //onView(allOf(withId(com.iterable.iterableapi.ui.R.id.list), withText("Coffee Promotion"))); //Validation is missing
        onView(withId(com.iterable.iterableapi.ui.R.id.list)).check(ViewAssertions.matches (Matchers.withListSize (3)));
    }

    static class Matchers{
        public static Matcher<View> withListSize (final int size) {
            return new TypeSafeMatcher<View>() {
                @Override public boolean matchesSafely (final View view) {
                    return ((RecyclerView) view).getAdapter().getItemCount() == size;
                }

                @Override public void describeTo (final Description description) {
                    description.appendText ("ListView should have " + size + " items");
                }
            };
        }
    }
}