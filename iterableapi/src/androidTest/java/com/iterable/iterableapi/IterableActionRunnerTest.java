package com.iterable.iterableapi;

import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasData;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class IterableActionRunnerTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testOpenUrlAction() throws Exception {
        IterableTestUtils.initIterableApi(null);
        intending(anyIntent()).respondWith(new Instrumentation.ActivityResult(0, null));
        JSONObject actionData = new JSONObject();
        actionData.put("type", "openUrl");
        actionData.put("data", "https://example.com");
        IterableAction action = IterableAction.from(actionData);
        IterableActionRunner.executeAction(getApplicationContext(), action, IterableActionSource.PUSH);

        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData("https://example.com")));
        Intents.assertNoUnverifiedIntents();
    }

    @Test
    public void testUrlHandlingOverride() throws Exception {
        // Use a simple implementation instead of mock for API 36+ compatibility
        IterableUrlHandler urlHandler = new IterableUrlHandler() {
            @Override
            public boolean handleIterableURL(Uri uri, IterableActionContext context) {
                return true;
            }
        };
        IterableTestUtils.initIterableApi(new IterableConfig.Builder().setUrlHandler(urlHandler).build());

        JSONObject actionData = new JSONObject();
        actionData.put("type", "openUrl");
        actionData.put("data", "https://example.com");
        IterableAction action = IterableAction.from(actionData);
        IterableActionRunner.executeAction(getApplicationContext(), action, IterableActionSource.PUSH);

        Intents.assertNoUnverifiedIntents();
        IterableTestUtils.initIterableApi(null);
    }

    @Test
    public void testCustomAction() throws Exception {
        // Track if the custom action handler was called (for API 36+ compatibility)
        final boolean[] handlerCalled = {false};
        final IterableAction[] capturedAction = {null};
        final IterableActionContext[] capturedContext = {null};

        IterableCustomActionHandler customActionHandler = (action, actionContext) -> {
            handlerCalled[0] = true;
            capturedAction[0] = action;
            capturedContext[0] = actionContext;
            return false;
        };

        IterableTestUtils.initIterableApi(new IterableConfig.Builder().setCustomActionHandler(customActionHandler).build());

        JSONObject actionData = new JSONObject();
        actionData.put("type", "customActionName");
        IterableAction action = IterableAction.from(actionData);
        IterableActionRunner.executeAction(getApplicationContext(), action, IterableActionSource.PUSH);

        // Verify the handler was called with correct parameters
        assertTrue("Custom action handler should have been called", handlerCalled[0]);
        assertNotNull("Action should not be null", capturedAction[0]);
        assertEquals("Action type should match", "customActionName", capturedAction[0].getType());
        assertNotNull("Context should not be null", capturedContext[0]);
        assertEquals("Source should be PUSH", IterableActionSource.PUSH, capturedContext[0].source);
        IterableTestUtils.initIterableApi(null);
    }
}
