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
    var storedApiKey: String? = null
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

    fun initializeIterableApi(context: Context, apiKey: String) {
        this.context = context
        this.storedApiKey = apiKey
        initHttpMocks()
        IterableApi.overrideURLEndpointPath(serverUrl)
        IterableApi.initialize(context, apiKey)
        // Note: setEmail should be called separately after initialization
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

    fun initializeIterableApiInBackground(context: Context, apiKey: String, callback: IterableInitializationCallback? = null) {
        this.context = context
        this.storedApiKey = apiKey
        initHttpMocks()
        IterableApi.overrideURLEndpointPath(serverUrl)
        IterableApi.initializeInBackground(context, apiKey) {
            // Set email after initialization completes
            IterableApi.getInstance().setEmail("user@example.com")
            loadData("simple-inbox-messages.json")
            callback?.onSDKInitialized()
        }
    }

    companion object {
        val instance = DataManager()

        fun initializeIterableApi(context: Context, apiKey: String) {
            instance.initializeIterableApi(context, apiKey)
        }

        fun initializeIterableApiInBackground(context: Context, apiKey: String, callback: IterableInitializationCallback? = null) {
            instance.initializeIterableApiInBackground(context, apiKey, callback)
        }

        fun getStoredApiKey(): String? {
            return instance.storedApiKey
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