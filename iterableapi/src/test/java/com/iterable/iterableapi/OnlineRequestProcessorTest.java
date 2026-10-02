package com.iterable.iterableapi;

import static org.junit.Assert.assertNotNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class OnlineRequestProcessorTest extends BaseTest {

    @Test
    public void testMalformedCreatedAtDoesNotPreventPushRequestSubmission() throws JSONException {
        AtomicReference<Runnable> submittedRequest = new AtomicReference<>();
        IterableRequestDispatcher dispatcher = new IterableRequestDispatcher(
                submittedRequest::set,
                Runnable::run,
                (runnable, delayMs) -> {
                }
        );
        OnlineRequestProcessor processor = new OnlineRequestProcessor(dispatcher);
        JSONObject requestJson = new JSONObject()
                .put(IterableConstants.KEY_CREATED_AT, "not-a-timestamp");

        processor.processPostRequest(
                "apiKey",
                IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN,
                requestJson,
                null,
                null,
                null,
                () -> true
        );

        assertNotNull(submittedRequest.get());
    }
}
