package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(TestRunner.class)
public class IterableApiRequestTest {
    @Before
    public void setUp() {

    }

    @Test
    public void testRequestSerialization() throws Exception {

        String apiKey = "apiKey";
        String resourcePath = "iterable.com/api/testmethod";
        JSONObject data = new JSONObject();
        data.put("SOME_DATA", "somedata");
        String requestType = "api";
        String authToken = "authToken123##";

        IterableHelper.SuccessHandler successHandler = new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(@NonNull JSONObject data) {
                IterableLogger.v("RequestSerializationTest", "Passed");
            }
        };

        IterableHelper.FailureHandler failureHandler = new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                IterableLogger.e("RequestSerializationTest", "Failed");
            }
        };

        IterableApiRequest request = new IterableApiRequest(apiKey, resourcePath, data, requestType, authToken, successHandler, failureHandler);
        JSONObject requestJSONObject = request.toJSONObject();
        IterableApiRequest newRequest = IterableApiRequest.fromJSON(requestJSONObject, successHandler, failureHandler);
        assert newRequest != null;
        assertEquals(request.apiKey, newRequest.apiKey);
        assertEquals(request.authToken, newRequest.authToken);
        assertEquals(request.requestType, newRequest.requestType);
        assertEquals(request.resourcePath, newRequest.resourcePath);
        assertEquals(request.json.toString(), newRequest.json.toString());
        assertEquals(request.successCallback, newRequest.successCallback);
        assertEquals(request.failureCallback, newRequest.failureCallback);
    }
}
