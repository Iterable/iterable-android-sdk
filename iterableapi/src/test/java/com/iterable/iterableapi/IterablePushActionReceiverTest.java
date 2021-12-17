package com.iterable.iterableapi;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.RemoteInput;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.RuntimeEnvironment;

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
        iterablePushActionReceiver.onReceive(RuntimeEnvironment.application, intent);
    }

    @Test
    public void testTrackPushOpenWithCustomAction() throws Exception {
        final JSONObject responseData = new JSONObject("{\"key\":\"value\"}");
        stubAnyRequestReturningStatusCode(server, 200, responseData);

        IterablePushActionReceiver iterablePushActionReceiver = new IterablePushActionReceiver();
        Intent intent = new Intent(IterableConstants.ACTION_PUSH_ACTION);
        intent.putExtra(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, IterableConstants.ITERABLE_ACTION_DEFAULT);
        intent.putExtra(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_custom_action.json"));

        iterablePushActionReceiver.onReceive(RuntimeEnvironment.application, intent);

        // Verify that IterableActionRunner was called with the proper action
        ArgumentCaptor<IterableAction> capturedAction = ArgumentCaptor.forClass(IterableAction.class);
        verify(actionRunnerMock).executeAction(any(Context.class), capturedAction.capture(), eq(IterableActionSource.PUSH), eq(new String[0]));
        assertEquals("customAction", capturedAction.getValue().getType());

        // Verify that the main app activity was launched
        Intent activityIntent = shadowOf(RuntimeEnvironment.application).peekNextStartedActivity();
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
        stubAnyRequestReturningStatusCode(server, 200, "{}");
        IterablePushActionReceiver iterablePushActionReceiver = new IterablePushActionReceiver();
        Intent intent = new Intent(IterableConstants.ACTION_PUSH_ACTION);
        intent.putExtra(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, "silentButton");
        intent.putExtra(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getResourceString("push_payload_silent_action.json"));

        iterablePushActionReceiver.onReceive(RuntimeEnvironment.application, intent);

        // Verify that the main app activity was NOT launched
        Intent activityIntent = shadowOf(RuntimeEnvironment.application).peekNextStartedActivity();
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

        iterablePushActionReceiver.onReceive(RuntimeEnvironment.application, intent);

        // Verify that IterableActionRunner was called with the proper action
        ArgumentCaptor<IterableAction> actionCaptor = ArgumentCaptor.forClass(IterableAction.class);
        verify(actionRunnerMock).executeAction(any(Context.class), actionCaptor.capture(), eq(IterableActionSource.PUSH), eq(new String[0]));
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

        iterablePushActionReceiver.onReceive(RuntimeEnvironment.application, intent);

        // Verify that IterableActionRunner was called with openUrl action
        ArgumentCaptor<IterableAction> capturedAction = ArgumentCaptor.forClass(IterableAction.class);
        verify(actionRunnerMock).executeAction(any(Context.class), capturedAction.capture(), eq(IterableActionSource.PUSH), eq(new String[0]));
        assertEquals("openUrl", capturedAction.getValue().getType());
        assertEquals("https://example.com", capturedAction.getValue().getData());
    }


}
