package com.iterable.inbox_customization.util

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.util.*

class PathBasedStaticDispatcher : Dispatcher() {
    private class PathBasedResponse(var pathPrefix: String, var response: MockResponse)

    private val responseList = ArrayList<PathBasedResponse>()

    fun setResponse(pathPrefix: String, mockResponse: MockResponse) {
        synchronized(responseList) {
            responseList.removeAll { it.pathPrefix == pathPrefix }
            responseList.add(PathBasedResponse(pathPrefix, mockResponse))
        }
    }

    @Throws(InterruptedException::class)
    override fun dispatch(request: RecordedRequest): MockResponse {
        synchronized(responseList) {
            responseList.firstOrNull { request.path?.startsWith(it.pathPrefix) == true }?.let {
                return it.response
            }
        }
        return MockResponse().setResponseCode(404).setBody("{}")
    }
}