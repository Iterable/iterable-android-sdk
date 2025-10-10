package com.iterable.inbox_customization.util

import android.content.Context
import android.os.StrictMode
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableInitializationCallback
import com.iterable.iterableapi.IterableInternal
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.source

class DataManager {
    var context: Context? = null
    val server = MockWebServer()
    val serverUrl: String
    val dispatcher = PathBasedStaticDispatcher()

    init {
        // Required for `server.url("")` to work on main thread without throwing an exception
        // DO NOT USE THIS in production. This is for demo purposes only.
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

        server.dispatcher = dispatcher
        serverUrl = server.url("").toString()
    }

    fun initializeIterableApi(context: Context) {
        this.context = context
        initHttpMocks()
        IterableApi.overrideURLEndpointPath(serverUrl)
        IterableApi.initialize(context, "apiKey")
        IterableApi.getInstance().setEmail("user@example.com")
        loadData("simple-inbox-messages.json")
    }

    fun initHttpMocks() {
        loadFileMock("mocha.png")
        loadFileMock("black-coffee.png")
        loadFileMock("cappuccino.png")
        loadFileMock("latte.png")
    }

    fun loadFileMock(imageName: String) {
        val buffer = Buffer()
        context!!.resources.assets.open(imageName).source().use {
            buffer.writeAll(it)
        }
        dispatcher.setResponse("/$imageName", MockResponse().setBody(buffer))
    }

    fun initializeIterableApiInBackground(context: Context, callback: IterableInitializationCallback? = null) {
        this.context = context
        initHttpMocks()
        IterableApi.overrideURLEndpointPath(serverUrl)
        IterableApi.initializeInBackground(context, "apiKey") {
            // Set email after initialization completes
            IterableApi.getInstance().setEmail("user@example.com")
            loadData("simple-inbox-messages.json")
            callback?.onSDKInitialized()
        }
    }

    companion object {
        val instance = DataManager()

        fun initializeIterableApi(context: Context) {
            instance.initializeIterableApi(context)
        }

        fun initializeIterableApiInBackground(context: Context, callback: IterableInitializationCallback? = null) {
            instance.initializeIterableApiInBackground(context, callback)
        }

        fun loadData(resourceName: String) {
            val body = getAssetString(resourceName).replace("https://somewhere.com/", instance.serverUrl)
            instance.dispatcher.setResponse("/inApp/getMessages", MockResponse().setBody(body))
            IterableInternal.syncInApp()
        }

        private fun getAssetString(fileName: String): String {
            return instance.context!!.resources.assets.open(fileName).bufferedReader().use {
                it.readText()
            }
        }
    }

}