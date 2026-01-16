package com.iterable.iterableapi;

import android.app.Application;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.RemoteInput;
import androidx.test.core.app.ApplicationProvider;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static androidx.core.app.RemoteInput.RESULTS_CLIP_LABEL;
import static com.iterable.iterableapi.IterableTestUtils.stubAnyRequestReturningStatusCode;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

public class IterablePushActionReceiverTest extends BaseTest {

    private MockWebServer server;
    private IterableActionRunner.IterableActionRunnerImpl actionRunnerMock;

    @Before
    public void setUp() throws Exception {
        IterableApi.sharedInstance = new IterableApi();
        IterableTestUtils.createIterableApi();
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        actionRunnerMock = mock(IterableActionRunner.IterableActionRunnerImpl.class);
        IterableActionRunner.instance = actionRunnerMock;
    }

    @After
    public void tearDown() throws Exception {
        IterableActionRunner.instance = new IterableActionRunner.IterableActionRunnerImpl();

        server.shutdown();
        server = null;
    }

    @Test
    public void testPushOpenWithNonInitializedSDK() throws Exception {
        stubAnyRequestReturningStatusCode(server, 200, "{}");
        IterableTestUtils.resetIterableApi();
        IterablePushActionReceiver iterablePushActionReceiver = new IterablePushActionReceiver();
        Intent intent = new Intent(IterableConstants.ACTION_PUSH_ACTION);
        intent.putExtra(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, "silentButton");
        intent.putExtra(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_silent_action.json"));

        // This must not crash
        iterablePushActionReceiver.onReceive(ApplicationProvider.getApplicationContext(), intent);
    }

    @Test
    public void testTrackPushOpenWithCustomAction() throws Exception {
        Application application = ApplicationProvider.getApplicationContext();
        final JSONObject responseData = new JSONObject("{\"key\":\"value\"}");
        stubAnyRequestReturningStatusCode(server, 200, responseData);

        IterablePushActionReceiver iterablePushActionReceiver = new IterablePushActionReceiver();
        Intent intent = new Intent(IterableConstants.ACTION_PUSH_ACTION);
        intent.putExtra(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, IterableConstants.ITERABLE_ACTION_DEFAULT);
        intent.putExtra(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_custom_action.json"));

        iterablePushActionReceiver.onReceive(application, intent);

        // Verify that IterableActionRunner was called with the proper action
        ArgumentCaptor<IterableAction> capturedAction = ArgumentCaptor.forClass(IterableAction.class);
        verify(actionRunnerMock).executeAction(any(Context.class), capturedAction.capture(), eq(IterableActionSource.PUSH));
        assertEquals("customAction", capturedAction.getValue().getType());

        // Verify that the main app activity was launched
        Intent activityIntent = shadowOf(application).peekNextStartedActivity();
        assertNotNull(activityIntent);
        assertEquals(Intent.ACTION_MAIN, activityIntent.getAction());

        // Verify trackPushOpen HTTP request
        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_PUSH_OPEN, recordedRequest.getPath());
        JSONObject jsonBody = new JSONObject(recordedRequest.getBody().readUtf8());
        assertEquals(1234, jsonBody.getInt(IterableConstants.KEY_CAMPAIGN_ID));
        assertEquals(4321, jsonBody.getInt(IterableConstants.KEY_TEMPLATE_ID));
        assertEquals("123456789abcdef", jsonBody.getString(IterableConstants.KEY_MESSAGE_ID));
        JSONObject dataFields = jsonBody.optJSONObject(IterableConstants.KEY_DATA_FIELDS);
        assertNotNull(dataFields);
        assertEquals(IterableConstants.ITERABLE_ACTION_DEFAULT, dataFields.getString(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER));
    }

    @Test
    public void testPushActionWithSilentAction() throws Exception {
        Application application = ApplicationProvider.getApplicationContext();
        stubAnyRequestReturningStatusCode(server, 200, "{}");
        IterablePushActionReceiver iterablePushActionReceiver = new IterablePushActionReceiver();
        Intent intent = new Intent(IterableConstants.ACTION_PUSH_ACTION);
        intent.putExtra(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, "silentButton");
        intent.putExtra(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_silent_action.json"));

        iterablePushActionReceiver.onReceive(application, intent);

        // Verify that the main app activity was NOT launched
        Intent activityIntent = shadowOf(application).peekNextStartedActivity();
        assertNull(activityIntent);
    }

    @Test
    public void testPushActionWithTextInput() throws Exception {
        IterablePushActionReceiver iterablePushActionReceiver = new IterablePushActionReceiver();
        Intent intent = new Intent(IterableConstants.ACTION_PUSH_ACTION);
        intent.putExtra(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, "textInputButton");
        intent.putExtra(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_action_buttons.json"));

        // Inject input text
        Bundle resultsBundle = new Bundle();
        Intent clipDataIntent = new Intent();
        resultsBundle.putString(IterableConstants.USER_INPUT, "input text");
        clipDataIntent.putExtra(RemoteInput.EXTRA_RESULTS_DATA, resultsBundle);
        intent.setClipData(ClipData.newIntent(RESULTS_CLIP_LABEL, clipDataIntent));

        iterablePushActionReceiver.onReceive(ApplicationProvider.getApplicationContext(), intent);

        // Verify that IterableActionRunner was called with the proper action
        ArgumentCaptor<IterableAction> actionCaptor = ArgumentCaptor.forClass(IterableAction.class);
        verify(actionRunnerMock).executeAction(any(Context.class), actionCaptor.capture(), eq(IterableActionSource.PUSH));
        IterableAction capturedAction = actionCaptor.getValue();
        assertEquals("handleTextInput", capturedAction.getType());
        assertEquals("input text", capturedAction.userInput);
    }

    @Test
    public void testLegacyDeepLinkPayload() throws Exception {
        stubAnyRequestReturningStatusCode(server, 200, "{}");
        IterablePushActionReceiver iterablePushActionReceiver = new IterablePushActionReceiver();
        Intent intent = new Intent(IterableConstants.ACTION_PUSH_ACTION);
        intent.putExtras(IterableTestUtils.getBundleFromJsonResource("push_payload_legacy_deep_link.json"));
        intent.putExtra(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, IterableConstants.ITERABLE_ACTION_DEFAULT);

        iterablePushActionReceiver.onReceive(ApplicationProvider.getApplicationContext(), intent);

        // Verify that IterableActionRunner was called with openUrl action
        ArgumentCaptor<IterableAction> capturedAction = ArgumentCaptor.forClass(IterableAction.class);
        verify(actionRunnerMock).executeAction(any(Context.class), capturedAction.capture(), eq(IterableActionSource.PUSH));
        assertEquals("openUrl", capturedAction.getValue().getType());
        assertEquals("https://example.com", capturedAction.getValue().getData());
    }

    @Test
    public void testBackgroundCustomActionWithNonInitializedSDK() throws Exception {
        // Reset to simulate SDK not being initialized
        IterableTestUtils.resetIterableApi();

        // Verify context is initially null
        assertNull(IterableApi.sharedInstance._applicationContext);

        IterablePushActionReceiver iterablePushActionReceiver = new IterablePushActionReceiver();
        Intent intent = new Intent(IterableConstants.ACTION_PUSH_ACTION);
        intent.putExtra(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, "remindMeButton");
        intent.putExtra(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_background_custom_action.json"));

        // Receive push action when SDK is not initialized
        iterablePushActionReceiver.onReceive(ApplicationProvider.getApplicationContext(), intent);

        // Verify that context was stored even without SDK initialization
        assertNotNull(IterableApi.sharedInstance._applicationContext);

        // Verify that the main app activity was NOT launched (openApp=false)
        Application application = ApplicationProvider.getApplicationContext();
        Intent activityIntent = shadowOf(application).peekNextStartedActivity();
        assertNull(activityIntent);
    }

    @Test
    public void testBackgroundCustomActionProcessedAfterSDKInit() throws Exception {
        // Reset to simulate SDK not being initialized
        IterableTestUtils.resetIterableApi();

        IterablePushActionReceiver iterablePushActionReceiver = new IterablePushActionReceiver();
        Intent intent = new Intent(IterableConstants.ACTION_PUSH_ACTION);
        intent.putExtra(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, "remindMeButton");
        intent.putExtra(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_background_custom_action.json"));

        // Receive push action when SDK is not initialized (action won't be handled)
        iterablePushActionReceiver.onReceive(ApplicationProvider.getApplicationContext(), intent);

        // Now initialize SDK with a custom action handler
        stubAnyRequestReturningStatusCode(server, 200, "{}");
        final boolean[] handlerCalled = {false};
        IterableTestUtils.createIterableApiNew(builder ->
            builder.setCustomActionHandler((action, actionContext) -> {
                handlerCalled[0] = true;
                assertEquals("snoozeReminder", action.getType());
                return true;
            })
        );

        // Verify that the custom action handler was called during initialization
        // (processPendingAction is called in initialize())
        assertEquals(true, handlerCalled[0]);
    }


}
