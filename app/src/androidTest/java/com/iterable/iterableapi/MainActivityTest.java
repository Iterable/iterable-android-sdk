package com.iterable.iterableapi;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.iterable.iterableapi.testapp.PathBasedQueueDispatcher;
import com.iterable.iterableapi.ui.inbox.InboxActivity;
import com.iterable.iterableapi.ui.inbox.InboxMessageActivity;
import com.iterable.iterableapi.ui.inbox.InboxMode;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.VerificationModes.times;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static com.iterable.iterableapi.ui.inbox.InboxActivity.INBOX_MODE;
import static org.hamcrest.Matchers.allOf;


@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private static String MainActTEXT = "Hello";
    private MockWebServer server;

    private PathBasedQueueDispatcher dispatcher;
    private IterableInAppHandler inAppHandler;
    private IterableCustomActionHandler customActionHandler;
    private IterableUrlHandler urlHandler;

    @Rule
    public IntentsTestRule<InboxActivity> rule =
            new IntentsTestRule<>(InboxActivity.class, false, false);
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
    public void checkIfExpiredMessageAreNotShown() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("editable_get_messages_response.json"));
        getTwoValidOneExpiredMessage(payload);

        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        clearMessageFromStorage();
        IterableApi.getInstance().getInAppManager().syncInApp();

        rule.launchActivity(null);
        onView(withId(com.iterable.iterableapi.ui.R.id.list)).check(matches (Matchers.withListSize (2)));
    }

    @Test
    public void testNonInboxMessagesToNotShowUpOnInboxFragment() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("editable_get_messages_response.json"));
        getTwoValidOneExpiredMessage(payload);
        JSONArray jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
        jsonArray.getJSONObject(0).put(IterableConstants.ITERABLE_IN_APP_SAVE_TO_INBOX,false);
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        clearMessageFromStorage();
        IterableApi.getInstance().getInAppManager().syncInApp();

        rule.launchActivity(null);
        onView(withId(com.iterable.iterableapi.ui.R.id.list)).check(matches (Matchers.withListSize (1)));
    }

    @Test
    public void checkIfMessageShownAsActivity() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("editable_get_messages_response.json"));
        getTwoValidOneExpiredMessage(payload);
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        clearMessageFromStorage();
        inAppManager.syncInApp();

        Intent intent = new Intent();
        intent.putExtra(INBOX_MODE, InboxMode.ACTIVITY);
        rule.launchActivity(intent);

        onView(withText("Tips and tricks 2")).perform(click());
        intended(hasComponent(InboxMessageActivity.class.getName()));
    }

    @Test
    public void checkIfMessageShownAsPopUpForImproperIntent() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("editable_get_messages_response.json"));
        getTwoValidOneExpiredMessage(payload);
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        clearMessageFromStorage();
        inAppManager.syncInApp();

        Intent intent = new Intent();
        intent.putExtra(INBOX_MODE, "Hello");
        rule.launchActivity(intent);

        onView(withText("Tips and tricks 2")).perform(click());
        intended(allOf(hasComponent(InboxMessageActivity.class.getName())), times(0));
        onView(withId(R.id.webView)).inRoot(isDialog()).check(matches(isDisplayed()));
        onWebView().withElement(findElement(Locator.XPATH, "//*[contains(text(),'Ok, got it')]"));
    }

    @Test
    public void checkIfMessageShownAsPopUpForUnexpectedIntent() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("editable_get_messages_response.json"));
        getTwoValidOneExpiredMessage(payload);
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        clearMessageFromStorage();
        inAppManager.syncInApp();

        Intent intent = new Intent();
        intent.putExtra("HEllO", "Hello");
        rule.launchActivity(intent);

        onView(withText("Tips and tricks 2")).perform(click());
        intended(allOf(hasComponent(InboxMessageActivity.class.getName())), times(0));
        onView(withId(R.id.webView)).inRoot(isDialog()).check(matches(isDisplayed()));
        onWebView().withElement(findElement(Locator.XPATH, "//*[contains(text(),'Ok, got it')]"));
    }

    @Test
    public void checkIfMessageShownAsPopUpForNoIntent() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("editable_get_messages_response.json"));
        getTwoValidOneExpiredMessage(payload);
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        clearMessageFromStorage();
        inAppManager.syncInApp();
        rule.launchActivity(null);

        onView(withText("Tips and tricks 2")).perform(click());
        intended(allOf(hasComponent(InboxMessageActivity.class.getName())), times(0));
        onView(withId(R.id.webView)).inRoot(isDialog()).check(matches(isDisplayed()));
        onWebView().withElement(findElement(Locator.XPATH, "//*[contains(text(),'Ok, got it')]"));
    }

    @Test
    public void checkIfMessageShownAsPopUp() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("editable_get_messages_response.json"));
        getTwoValidOneExpiredMessage(payload);
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        clearMessageFromStorage();
        inAppManager.syncInApp();

        Intent intent = new Intent();
        intent.putExtra(INBOX_MODE, InboxMode.POPUP);

        rule.launchActivity(intent);

        onView(withText("Tips and tricks 2")).perform(click());
        intended(allOf(hasComponent(InboxMessageActivity.class.getName())), times(0));
        onView(withId(R.id.webView)).inRoot(isDialog()).check(matches(isDisplayed()));
        onWebView().withElement(findElement(Locator.XPATH, "//*[contains(text(),'Ok, got it')]"));
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

    private void getTwoValidOneExpiredMessage(JSONObject payload) throws JSONException {
        JSONArray jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
        JSONObject validMessage1 = jsonArray.getJSONObject(0).put(IterableConstants.ITERABLE_IN_APP_EXPIRES_AT, System.currentTimeMillis() + 60 * 1000).put(IterableConstants.ITERABLE_IN_APP_CREATED_AT, System.currentTimeMillis() - 60 * 1000);
        JSONObject validMessage2 = jsonArray.getJSONObject(1).put(IterableConstants.ITERABLE_IN_APP_EXPIRES_AT, System.currentTimeMillis() + 60 * 1000).put(IterableConstants.ITERABLE_IN_APP_CREATED_AT, System.currentTimeMillis() - 60 * 1000);
        JSONObject expiredMessage = jsonArray.getJSONObject(2).put(IterableConstants.ITERABLE_IN_APP_EXPIRES_AT, System.currentTimeMillis() - 60 * 1000).put(IterableConstants.ITERABLE_IN_APP_CREATED_AT, System.currentTimeMillis() - 60 * 1000);

        jsonArray.remove(0);
        jsonArray.put(validMessage1);
        jsonArray.put(validMessage2);
        jsonArray.put(expiredMessage);
    }

    private void clearMessageFromStorage() {
        IterableApi.getInstance().getInAppManager().removeMessage("MKPNMZVQ1TbujjdNuguRug7M24dT4LFiterable");
        IterableApi.getInstance().getInAppManager().removeMessage("ERPNnMVABFGuoodQi5uBuPdWawku4GIiterable");
        IterableApi.getInstance().getInAppManager().removeMessage("JjPkeZKK0fEunnBXhqu5uk1LmnGhnKhiterable");
    }

    private void sleepFor(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}