package com.iterable.iterableapi;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the functionality of IterableHelper callback interfaces
 */
public class IterableHelperUnitTest {
    @Test
    public void actionHandlerCallback() throws Exception {
        final String resultString = "testString";

        IterableHelper.IterableActionHandler clickCallback = new IterableHelper.IterableActionHandler() {
            @Override
            public void execute(String result) {
                assertEquals(result, resultString);
            }
        };
        clickCallback.execute(resultString);
    }

    // ========== IterableSuccessCallback Tests ==========

    @Test
    public void testIterableSuccessCallback_WithRemoteSuccess() throws Exception {
        JSONObject testJson = new JSONObject().put("key", "value");
        IterableResponseObject.RemoteSuccess remoteSuccess = new IterableResponseObject.RemoteSuccess(testJson);
        
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        
        IterableHelper.IterableSuccessCallback callback = data -> {
            callbackInvoked.set(true);
            assertTrue(data instanceof IterableResponseObject.RemoteSuccess);
            try {
                assertEquals("value", ((IterableResponseObject.RemoteSuccess) data).getResponseJson().getString("key"));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        };
        
        callback.onSuccess(remoteSuccess);
        assertTrue("Callback should be invoked", callbackInvoked.get());
    }

    @Test
    public void testIterableSuccessCallback_WithLocalSuccess() {
        IterableResponseObject.Success localSuccess = IterableResponseObject.LocalSuccessResponse;
        
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        
        IterableHelper.IterableSuccessCallback callback = data -> {
            callbackInvoked.set(true);
            assertTrue(data instanceof IterableResponseObject.LocalSuccess);
            assertNotNull(data.getMessage());
        };
        
        callback.onSuccess(localSuccess);
        assertTrue("Callback should be invoked", callbackInvoked.get());
    }

    @Test
    public void testIterableSuccessCallback_WithAuthTokenSuccess() {
        String testToken = "test-jwt-token-123";
        IterableResponseObject.AuthTokenSuccess authSuccess = new IterableResponseObject.AuthTokenSuccess(testToken);
        
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        
        IterableHelper.IterableSuccessCallback callback = data -> {
            callbackInvoked.set(true);
            assertTrue(data instanceof IterableResponseObject.AuthTokenSuccess);
            assertEquals(testToken, ((IterableResponseObject.AuthTokenSuccess) data).getAuthToken());
        };
        
        callback.onSuccess(authSuccess);
        assertTrue("Callback should be invoked", callbackInvoked.get());
    }

    // ========== RemoteSuccessCallback Tests ==========

    @Test
    public void testRemoteSuccessCallback_WithCorrectType() throws Exception {
        JSONObject testJson = new JSONObject().put("status", "success");
        IterableResponseObject.RemoteSuccess remoteSuccess = new IterableResponseObject.RemoteSuccess(testJson);
        
        AtomicBoolean typedCallbackInvoked = new AtomicBoolean(false);
        
        IterableHelper.RemoteSuccessCallback callback = new IterableHelper.RemoteSuccessCallback() {
            @Override
            public void onSuccess(IterableResponseObject.RemoteSuccess data) {
                typedCallbackInvoked.set(true);
                assertEquals(testJson, data.getResponseJson());
            }
        };
        
        callback.onSuccess((IterableResponseObject.Success) remoteSuccess);
        assertTrue("Typed callback should be invoked for RemoteSuccess", typedCallbackInvoked.get());
    }

    @Test
    public void testRemoteSuccessCallback_WithLocalSuccess_LogsWarning() {
        IterableResponseObject.Success localSuccess = IterableResponseObject.LocalSuccessResponse;
        
        AtomicBoolean typedCallbackInvoked = new AtomicBoolean(false);
        
        IterableHelper.RemoteSuccessCallback callback = new IterableHelper.RemoteSuccessCallback() {
            @Override
            public void onSuccess(IterableResponseObject.RemoteSuccess data) {
                typedCallbackInvoked.set(true);
            }
        };
        
        // Should not invoke typed callback, should log warning instead
        callback.onSuccess(localSuccess);
        assertFalse("Typed callback should NOT be invoked for LocalSuccess", typedCallbackInvoked.get());
    }

    // ========== LocalSuccessCallback Tests ==========

    @Test
    public void testLocalSuccessCallback_WithCorrectType() {
        IterableResponseObject.Success localSuccess = IterableResponseObject.LocalSuccessResponse;
        
        AtomicBoolean typedCallbackInvoked = new AtomicBoolean(false);
        
        IterableHelper.LocalSuccessCallback callback = new IterableHelper.LocalSuccessCallback() {
            @Override
            public void onSuccess(IterableResponseObject.LocalSuccess data) {
                typedCallbackInvoked.set(true);
                assertNotNull(data.getMessage());
            }
        };
        
        callback.onSuccess(localSuccess);
        assertTrue("Typed callback should be invoked for LocalSuccess", typedCallbackInvoked.get());
    }

    @Test
    public void testLocalSuccessCallback_WithRemoteSuccess_LogsWarning() throws Exception {
        JSONObject testJson = new JSONObject().put("key", "value");
        IterableResponseObject.RemoteSuccess remoteSuccess = new IterableResponseObject.RemoteSuccess(testJson);
        
        AtomicBoolean typedCallbackInvoked = new AtomicBoolean(false);
        
        IterableHelper.LocalSuccessCallback callback = new IterableHelper.LocalSuccessCallback() {
            @Override
            public void onSuccess(IterableResponseObject.LocalSuccess data) {
                typedCallbackInvoked.set(true);
            }
        };
        
        // Should not invoke typed callback, should log warning instead
        callback.onSuccess((IterableResponseObject.Success) remoteSuccess);
        assertFalse("Typed callback should NOT be invoked for RemoteSuccess", typedCallbackInvoked.get());
    }

    // ========== AuthTokenCallback Tests ==========

    @Test
    public void testAuthTokenCallback_WithCorrectType() {
        String testToken = "jwt-token-xyz";
        IterableResponseObject.AuthTokenSuccess authSuccess = new IterableResponseObject.AuthTokenSuccess(testToken);
        
        AtomicBoolean typedCallbackInvoked = new AtomicBoolean(false);
        
        IterableHelper.AuthTokenCallback callback = new IterableHelper.AuthTokenCallback() {
            @Override
            public void onSuccess(IterableResponseObject.AuthTokenSuccess data) {
                typedCallbackInvoked.set(true);
                assertEquals(testToken, data.getAuthToken());
            }
        };
        
        callback.onSuccess((IterableResponseObject.Success) authSuccess);
        assertTrue("Typed callback should be invoked for AuthTokenSuccess", typedCallbackInvoked.get());
    }

    @Test
    public void testAuthTokenCallback_WithWrongType_LogsWarning() {
        IterableResponseObject.Success localSuccess = IterableResponseObject.LocalSuccessResponse;
        
        AtomicBoolean typedCallbackInvoked = new AtomicBoolean(false);
        
        IterableHelper.AuthTokenCallback callback = new IterableHelper.AuthTokenCallback() {
            @Override
            public void onSuccess(IterableResponseObject.AuthTokenSuccess data) {
                typedCallbackInvoked.set(true);
            }
        };
        
        // Should not invoke typed callback, should log warning instead
        callback.onSuccess(localSuccess);
        assertFalse("Typed callback should NOT be invoked for LocalSuccess", typedCallbackInvoked.get());
    }

    // ========== SuccessHandler (Deprecated) Backward Compatibility Tests ==========

    @Test
    public void testSuccessHandler_WithRemoteSuccess_PassesCorrectJSON() throws Exception {
        JSONObject testJson = new JSONObject()
            .put("key1", "value1")
            .put("key2", 123);
        IterableResponseObject.RemoteSuccess remoteSuccess = new IterableResponseObject.RemoteSuccess(testJson);
        
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        AtomicReference<JSONObject> receivedJson = new AtomicReference<>();
        
        IterableHelper.SuccessHandler handler = new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(JSONObject data) {
                callbackInvoked.set(true);
                receivedJson.set(data);
            }
        };
        
        handler.onSuccess((IterableResponseObject.Success) remoteSuccess);
        
        assertTrue("Callback should be invoked", callbackInvoked.get());
        assertNotNull("JSON should not be null", receivedJson.get());
        assertEquals("value1", receivedJson.get().getString("key1"));
        assertEquals(123, receivedJson.get().getInt("key2"));
    }

    @Test
    public void testSuccessHandler_WithLocalSuccess_PassesMessageJSON() throws Exception {
        IterableResponseObject.Success localSuccess = IterableResponseObject.LocalSuccessResponse;
        
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        AtomicReference<JSONObject> receivedJson = new AtomicReference<>();
        
        IterableHelper.SuccessHandler handler = new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(JSONObject data) {
                callbackInvoked.set(true);
                receivedJson.set(data);
            }
        };
        
        handler.onSuccess(localSuccess);
        
        assertTrue("Callback should be invoked", callbackInvoked.get());
        assertNotNull("JSON should not be null", receivedJson.get());
        assertTrue("JSON should contain message", receivedJson.get().has("message"));
        assertNotNull(receivedJson.get().getString("message"));
    }

    @Test
    public void testSuccessHandler_WithAuthTokenSuccess_PassesTokenJSON() throws Exception {
        String testToken = "test-auth-token";
        IterableResponseObject.AuthTokenSuccess authSuccess = new IterableResponseObject.AuthTokenSuccess(testToken);
        
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        AtomicReference<JSONObject> receivedJson = new AtomicReference<>();
        
        IterableHelper.SuccessHandler handler = new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(JSONObject data) {
                callbackInvoked.set(true);
                receivedJson.set(data);
            }
        };
        
        handler.onSuccess((IterableResponseObject.Success) authSuccess);
        
        assertTrue("Callback should be invoked", callbackInvoked.get());
        assertNotNull("JSON should not be null", receivedJson.get());
        assertTrue("JSON should contain newAuthToken", receivedJson.get().has("newAuthToken"));
        assertEquals(testToken, receivedJson.get().getString("newAuthToken"));
    }

    @Test
    public void testSuccessHandler_DoesNotMutateOriginalJSON() throws Exception {
        JSONObject originalJson = new JSONObject().put("original", "value");
        IterableResponseObject.RemoteSuccess remoteSuccess = new IterableResponseObject.RemoteSuccess(originalJson);
        
        IterableHelper.SuccessHandler handler = new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(JSONObject data) {
                // Received JSON is a copy, mutations here shouldn't affect original
                try {
                    data.put("modified", "newValue");
                } catch (JSONException e) {
                    // Ignore
                }
            }
        };
        
        handler.onSuccess((IterableResponseObject.Success) remoteSuccess);
        
        // Original JSON should not have the "modified" field
        assertFalse("Original JSON should not be mutated", originalJson.has("modified"));
        assertTrue("Original JSON should still have original field", originalJson.has("original"));
    }

    // ========== FailureHandler Tests ==========

    @Test
    public void testFailureHandler_ReceivesReasonAndData() throws Exception {
        String expectedReason = "Network error";
        JSONObject expectedData = new JSONObject().put("errorCode", 500);
        
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        
        IterableHelper.FailureHandler handler = (reason, data) -> {
            callbackInvoked.set(true);
            assertEquals(expectedReason, reason);
            assertNotNull(data);
            try {
                assertEquals(500, data.getInt("errorCode"));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        };
        
        handler.onFailure(expectedReason, expectedData);
        assertTrue("Failure handler should be invoked", callbackInvoked.get());
    }

    @Test
    public void testFailureHandler_HandlesNullData() {
        String expectedReason = "Unknown error";
        
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        
        IterableHelper.FailureHandler handler = (reason, data) -> {
            callbackInvoked.set(true);
            assertEquals(expectedReason, reason);
            // Data can be null, should not throw
        };
        
        handler.onFailure(expectedReason, null);
        assertTrue("Failure handler should be invoked with null data", callbackInvoked.get());
    }
}