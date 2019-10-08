package com.iterable.iterableapi;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import com.iterable.iterableapi.testapp.PathBasedQueueDispatcher;
import com.iterable.iterableapi.ui.inbox.InboxActivity;
import com.iterable.iterableapi.ui.inbox.InboxMessageActivity;

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
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;


@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private static String MainActTEXT = "Hello";
    private MockWebServer server;
    private PathBasedQueueDispatcher dispatcher;
    private IterableInAppHandler inAppHandler;
    private IterableCustomActionHandler customActionHandler;
    private IterableUrlHandler urlHandler;
//    @Rule
//    public ActivityTestRule rule = new ActivityTestRule<>(InboxActivity.class, false, false);

    @Rule
    public ActivityTestRule<InboxActivity> rule =
            new ActivityTestRule<>(InboxActivity.class, false, true);

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
    public void testNonInboxMessageValidation() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("editable_get_messages_response.json"));
        getTwoValidOneExpiredMessage(payload);

        //Change the 'save toinbox' to false for one the parameter
        JSONArray jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
        JSONObject validMessage1 = jsonArray.getJSONObject(0).put(IterableConstants.ITERABLE_IN_APP_SAVE_TO_INBOX,false);

        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        clearMessageFromStorage();
        IterableApi.getInstance().getInAppManager().syncInApp();

        rule.launchActivity(null);
        onView(withId(com.iterable.iterableapi.ui.R.id.list)).check(matches (Matchers.withListSize (1)));
    }


//    @Test
//    public void checkInboxFragmentCount() throws IOException {
//
//        String resourceString = getValidResourceString();
//
//        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(resourceString));
//        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
//        clearMessageFromStorage();
//
//        inAppManager.syncInApp();
//        rule.launchActivity(null);
//        //onView(withId(com.iterable.iterableapi.ui.R.id.list)).check(matches(isDisplayed()));
//        onView(withId(com.iterable.iterableapi.ui.R.id.list)).check(matches (Matchers.withListSize (3)));
//    }

//TODO: Try to programmaticallly change inbox mode from pop up to activity
    @Test
    public void checkIfMessageShownInActivity() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("editable_get_messages_response.json"));
        getTwoValidOneExpiredMessage(payload);
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        clearMessageFromStorage();
        inAppManager.syncInApp();
        rule.launchActivity(null);

        onView(withText("Tips and tricks 2")).perform(click());

        //Check for title text for activity
        onView(withText("AndroidSDK")).check(doesNotExist());
        //intended(hasComponent(InboxMessageActivity.class.getName()));
        //intending(hasComponent(InboxMessageActivity.class.getName()));
    }

    //TODO: Try to programmaticallly change inbox mode from pop up to activity
    @Test
    public void checkIfMessageShownAsPopUp() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("editable_get_messages_response.json"));
        getTwoValidOneExpiredMessage(payload);
        dispatcher.enqueueResponse("/inApp/getMessages", new MockResponse().setBody(payload.toString()));

        IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
        clearMessageFromStorage();
        inAppManager.syncInApp();
        rule.launchActivity(null);

        onView(withText("Tips and tricks 2")).perform(click());

        //Check for title text is not displayed
        //onView(withText("AndroidSDK")).check(matches(isDisplayed()));
//        intended(allOf(hasComponent(InboxMessageActivity.class.getName())));
        intended(allOf(hasComponent(InboxMessageActivity.class.getName())));
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

//    private String getValidResourceString() throws IOException {
//        String resourceString = IterableTestUtils.getResourceString("editable_get_messages_response.json");
//
//        long yesterday = getEpochforPastDay();
//        long tomorrow = getEpochforNextDay();
//
//        resourceString = resourceString.replace("\"***EPOCHTIME_MESSAGE1_CREATED***\"", ""+yesterday);
//        resourceString = resourceString.replace("\"***EPOCHTIME_MESSAGE1_EXPIRES***\"", ""+tomorrow);
//
//        resourceString = resourceString.replace("\"***EPOCHTIME_MESSAGE2_CREATED***\"", ""+yesterday);
//        resourceString = resourceString.replace("\"***EPOCHTIME_MESSAGE2_EXPIRES***\"", ""+tomorrow);
//
//        resourceString = resourceString.replace("\"***EPOCHTIME_MESSAGE3_CREATED***\"", ""+yesterday);
//        resourceString = resourceString.replace("\"***EPOCHTIME_MESSAGE3_EXPIRES***\"", ""+tomorrow);
//        return resourceString;
//    }



//    private long getEpochForCurrentTime() {
//        Date today = Calendar.getInstance().getTime();
//        return getValidEpochTime(today);
//    }
//
//    private long getEpochforPastDay() {
//        Calendar c = Calendar.getInstance();
//        c.setTime(Calendar.getInstance().getTime());
//        c.add(Calendar.DATE, -1);
//        return getValidEpochTime(c.getTime());
//    }
//
//    private long getEpochforNextDay() {
//        Calendar c = Calendar.getInstance();
//        c.setTime(Calendar.getInstance().getTime());
//        c.add(Calendar.DATE, 1);
//        return getValidEpochTime(c.getTime());
//    }
//
//    private long getValidEpochTime(Date date) {
//        //Date today = Calendar.getInstance().getTime();
//        SimpleDateFormat df = new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS zzz");
//        String currentTime = df.format(date);
//
//        try{
//            Date formattedDate = df.parse(currentTime);
//            long epochTime = formattedDate.getTime();
//            return epochTime;
//        }catch (Exception e){
//            return 0000000000000;
//        }
//    }




}