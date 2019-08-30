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
import org.mockito.ArgumentCaptor;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasData;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    public void testLegacyApiInit() throws Exception {
        IterableTestUtils.legacyInitIterableApi();
        intending(anyIntent()).respondWith(new Instrumentation.ActivityResult(0, null));
        JSONObject actionData = new JSONObject();
        actionData.put("type", "openUrl");
        actionData.put("data", "https://example.com");
        IterableAction action = IterableAction.from(actionData);
        IterableActionRunner.executeAction(getApplicationContext(), action, IterableActionSource.PUSH);

        // It should not attempt to open the URL unless it is initialized with a new method
        Intents.assertNoUnverifiedIntents();
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
        IterableUrlHandler urlHandlerMock = mock(IterableUrlHandler.class);
        when(urlHandlerMock.handleIterableURL(any(Uri.class), any(IterableActionContext.class))).thenReturn(true);
        IterableTestUtils.initIterableApi(new IterableConfig.Builder().setUrlHandler(urlHandlerMock).build());

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
        IterableCustomActionHandler customActionHandlerMock = mock(IterableCustomActionHandler.class);
        IterableTestUtils.initIterableApi(new IterableConfig.Builder().setCustomActionHandler(customActionHandlerMock).build());

        JSONObject actionData = new JSONObject();
        actionData.put("type", "customActionName");
        IterableAction action = IterableAction.from(actionData);
        IterableActionRunner.executeAction(getApplicationContext(), action, IterableActionSource.PUSH);

        ArgumentCaptor<IterableActionContext> contextCaptor = ArgumentCaptor.forClass(IterableActionContext.class);
        verify(customActionHandlerMock).handleIterableCustomAction(eq(action), contextCaptor.capture());
        assertEquals(IterableActionSource.PUSH, contextCaptor.getValue().source);
        IterableTestUtils.initIterableApi(null);
    }
}
