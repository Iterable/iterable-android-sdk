package com.iterable.iterableapi;

import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.iterable.iterableapi.testapp.PathBasedQueueDispatcher;
import com.iterable.iterableapi.ui.inbox.InboxMode;
import com.iterable.iterableapi.ui.inbox.IterableInboxActivity;
import com.iterable.iterableapi.ui.inbox.IterableInboxMessageActivity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
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
import static com.iterable.iterableapi.ui.inbox.IterableInboxFragment.INBOX_MODE;
import static org.hamcrest.Matchers.allOf;


@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private MockWebServer server;

    private PathBasedQueueDispatcher dispatcher;
    private IterableInAppHandler inAppHandler;
    private IterableCustomActionHandler customActionHandler;
    private IterableUrlHandler urlHandler;

    @Rule
    public IntentsTestRule<IterableInboxActivity> rule =
            new IntentsTestRule<>(IterableInboxActivity.class, false, false);
    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        dispatcher = new PathBasedQueueDispatcher();
        server.setDispatcher(dispatcher);
        // This is done so we don't hold a reference to the previous in-app notification.
        // We should handle in-app dismissal when the parent activity is destroyed though.
        IterableInAppFragmentHTMLNotification.notification = null;
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
        intended(hasComponent(IterableInboxMessageActivity.class.getName()));
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
        intended(allOf(hasComponent(IterableInboxMessageActivity.class.getName())), times(0));
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
        intended(allOf(hasComponent(IterableInboxMessageActivity.class.getName())), times(0));
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
        intended(allOf(hasComponent(IterableInboxMessageActivity.class.getName())), times(0));
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
        intended(allOf(hasComponent(IterableInboxMessageActivity.class.getName())), times(0));
        onView(withId(R.id.webView)).inRoot(isDialog()).check(matches(isDisplayed()));
        onWebView().withElement(findElement(Locator.XPATH, "//*[contains(text(),'Ok, got it')]"));
    }

    @Test
    public void testSwipeToDeleteInApp() throws Exception {
        //TODO: Add check to see if removeMessage method was triggered using spy objects. Somehow it(the commented code below) is failing in Travis environment. Mostly because the espresso continues before the background tasks are completed.
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("editable_get_messages_response.json"));
        getTwoValidOneExpiredMessage(payload);
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));
        //IterableInAppManager spyInAppManager = Mockito.spy(IterableApi.getInstance().getInAppManager());
        //IterableApi api = new IterableApi(spyInAppManager);
        //IterableApi.sharedInstance = api;
        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        clearMessageFromStorage();
        inAppManager.syncInApp();
        rule.launchActivity(null);
        //onView(withText("Tips and tricks 2")).check(matches(isDisplayed()));

        //IterableInAppMessage messageToDelete = IterableApi.getInstance().getInAppManager().getInboxMessages().get(0);
        onView(withText("Tips and tricks 2")).perform(ViewActions.swipeLeft());
        waitFor(100);
        //Mockito.verify(spyInAppManager).removeMessage(messageToDelete, IterableInAppDeleteActionType.INBOX_SWIPE, IterableInAppLocation.INBOX);
        onView(withText("Tips and tricks 2")).check(doesNotExist());

        rule.finishActivity();
        IterableApi.getInstance().getInAppManager().syncInApp();
        rule.launchActivity(null);
        onView(withText("Tips and tricks 2")).check(doesNotExist());
    }

    @Test
    public void testNoMessagesTitleAndText() throws Exception {
        Intent intent = new Intent();
        String noMessageTitle = "OOPSY";
        String noMessageBody = "No messages for you";
        intent.putExtra(IterableConstants.NO_MESSAGES_TITLE,noMessageTitle);
        intent.putExtra(IterableConstants.NO_MESSAGES_BODY,noMessageBody);
        rule.launchActivity(intent);
        onView(withText(noMessageTitle)).check(matches(isDisplayed()));
        onView(withText(noMessageBody)).check(matches(isDisplayed()));
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

    static void waitFor(int ms) {
        final CountDownLatch signal = new CountDownLatch(1);

        try {
            signal.await(ms, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }
}