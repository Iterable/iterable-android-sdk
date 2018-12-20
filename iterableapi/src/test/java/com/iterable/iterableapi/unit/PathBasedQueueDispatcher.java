package com.iterable.iterableapi.unit;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

public class PathBasedQueueDispatcher extends Dispatcher {

    private static class PathBasedResponse {
        String pathPrefix;
        MockResponse response;

        public PathBasedResponse(String pathPrefix, MockResponse response) {
            this.pathPrefix = pathPrefix;
            this.response = response;
        }
    }

    private final ArrayList<PathBasedResponse> responseQueue = new ArrayList<>();



    public void enqueueResponse(String pathPrefix, MockResponse mockResponse) {
        synchronized (responseQueue) {
            responseQueue.add(new PathBasedResponse(pathPrefix, mockResponse));
        }
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        String path = request.getPath();
        PathBasedResponse selectedResponse = null;
        synchronized (responseQueue) {
            for (PathBasedResponse response : responseQueue) {
                if (path.startsWith(response.pathPrefix)) {
                    selectedResponse = response;
                }
            }
            if (selectedResponse != null) {
                responseQueue.remove(selectedResponse);
                return selectedResponse.response;
            }
        }
        return new MockResponse().setResponseCode(404).setBody("{}");
    }
}
