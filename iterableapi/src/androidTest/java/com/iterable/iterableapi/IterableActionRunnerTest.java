package com.iterable.iterableapi;

import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.Intents;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.anyIntent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static org.hamcrest.CoreMatchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class IterableActionRunnerTest {

    @Before
    public void setUp() {
        IterableTestUtils.createIterableApi();
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testOpenUrlAction() throws Exception {
        intending(anyIntent()).respondWith(new Instrumentation.ActivityResult(0, null));
        JSONObject actionData = new JSONObject();
        actionData.put("type", "openUrl");
        actionData.put("data", "https://example.com");
        IterableAction action = new IterableAction(actionData);
        IterableActionRunner.executeAction(InstrumentationRegistry.getTargetContext(), action);

        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData("https://example.com")));
        Intents.assertNoUnverifiedIntents();
    }

    @Test
    public void testUrlHandlingOverride() throws Exception {
        IterableUrlHandler urlHandlerMock = mock(IterableUrlHandler.class);
        when(urlHandlerMock.handleIterableURL(any(Uri.class), any(IterableAction.class))).thenReturn(true);
        IterableApi.sharedInstance.setUrlHandler(urlHandlerMock);

        JSONObject actionData = new JSONObject();
        actionData.put("type", "openUrl");
        actionData.put("data", "https://example.com");
        IterableAction action = new IterableAction(actionData);
        IterableActionRunner.executeAction(InstrumentationRegistry.getTargetContext(), action);

        Intents.assertNoUnverifiedIntents();
        IterableApi.sharedInstance.setUrlHandler(null);
    }

    @Test
    public void testCustomAction() throws Exception {
        IterableCustomActionHandler customActionHandlerMock = mock(IterableCustomActionHandler.class);
        IterableApi.sharedInstance.setCustomActionHandler(customActionHandlerMock);

        JSONObject actionData = new JSONObject();
        actionData.put("type", "customActionName");
        IterableAction action = new IterableAction(actionData);
        IterableActionRunner.executeAction(InstrumentationRegistry.getTargetContext(), action);

        verify(customActionHandlerMock).handleIterableCustomAction(action);
        IterableApi.sharedInstance.setCustomActionHandler(null);
    }
}
