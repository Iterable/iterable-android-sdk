package com.iterable.androidsdk.inboxCustomization.util

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.util.*

class PathBasedStaticDispatcher : Dispatcher() {
    private class PathBasedResponse(var pathPrefix: String, var response: MockResponse)

    private val responseList = ArrayList<PathBasedResponse>()

    fun setResponse(pathPrefix: String, mockResponse: MockResponse) {
        synchronized(responseList) {
            for (response in responseList) {
                if (response.pathPrefix == pathPrefix) {
                    responseList.remove(response)
                    break
                }
            }
            responseList.add(PathBasedResponse(pathPrefix, mockResponse))
        }
    }

    @Throws(InterruptedException::class)
    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.path
        var selectedResponse: PathBasedResponse? = null
        synchronized(responseList) {
            for (response in responseList) {
                if (path!!.startsWith(response.pathPrefix)) {
                    selectedResponse = response
                    break
                }
            }
            if (selectedResponse != null) {
                return selectedResponse!!.response
            }
        }
        return MockResponse().setResponseCode(404).setBody("{}")
    }
}