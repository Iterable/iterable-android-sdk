package com.iterable.inbox_customization.util

import android.content.Context
import android.content.SharedPreferences
import android.os.StrictMode
import android.util.Log
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableConfig
import com.iterable.iterableapi.IterableInitializationCallback
import com.iterable.iterableapi.IterableInternal
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.source
import java.lang.reflect.Field

class DataManager {
    var context: Context? = null
    var storedApiKey: String? = null
    val server = MockWebServer()
    val serverUrl: String
    val dispatcher = PathBasedStaticDispatcher()

    init {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        server.dispatcher = dispatcher
        serverUrl = server.url("").toString()
    }

    fun initializeIterableApi(context: Context, apiKey: String) {
        this.context = context
        this.storedApiKey = apiKey
        saveApiKeyToPreferences(context, apiKey)
        initHttpMocks()
        mockFirebaseToken()
        IterableApi.initialize(context, apiKey, IterableConfig.Builder().setLogLevel(Log.VERBOSE).build())
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
        saveApiKeyToPreferences(context, apiKey)
        initHttpMocks()
        mockFirebaseToken()
        IterableApi.initializeInBackground(context, apiKey) {
            IterableApi.getInstance().setEmail("user@example.com")
            loadData("simple-inbox-messages.json")
            callback?.onSDKInitialized()
        }
    }

    private fun mockFirebaseToken() {
        try {
            val utilClass = Class.forName("com.iterable.iterableapi.IterablePushRegistrationTask\$Util")
            val mockUtilImplClass = Class.forName("com.iterable.iterableapi.MockUtilImpl")
            val instanceField: Field = utilClass.getDeclaredField("instance")
            instanceField.isAccessible = true
            val constructor = mockUtilImplClass.getDeclaredConstructor()
            constructor.isAccessible = true
            instanceField.set(null, constructor.newInstance())
            Log.d("DataManager", "Successfully mocked Firebase token provider")
        } catch (e: Exception) {
            Log.w("DataManager", "Failed to mock Firebase token: ${e.message}. " +
                    "This is OK if Firebase is properly configured.")
        }
    }

    companion object {
        val instance = DataManager()
        
        private const val PREFS_NAME = "iterable_prefs"
        private const val KEY_API_KEY = "api_key"

        fun initializeIterableApi(context: Context, apiKey: String) {
            instance.initializeIterableApi(context, apiKey)
        }

        fun initializeIterableApiInBackground(context: Context, apiKey: String, callback: IterableInitializationCallback? = null) {
            instance.initializeIterableApiInBackground(context, apiKey, callback)
        }

        fun getStoredApiKey(context: Context? = null): String? {
            if (IterableApi.isSDKInitialized()) {
                try {
                    val apiKeyField: Field = IterableApi.getInstance().javaClass.getDeclaredField("_apiKey")
                    apiKeyField.isAccessible = true
                    (apiKeyField.get(IterableApi.getInstance()) as? String)?.let {
                        if (it.isNotEmpty()) {
                            instance.storedApiKey = it
                            return it
                        }
                    }
                } catch (e: Exception) {
                    Log.w("DataManager", "Failed to retrieve API key from SDK: ${e.message}")
                }
            }
            if (!instance.storedApiKey.isNullOrEmpty()) return instance.storedApiKey
            return context?.let { loadApiKeyFromPreferences(it) }
        }
        
        fun saveApiKeyToPreferences(context: Context, apiKey: String) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_API_KEY, apiKey).apply()
        }
        
        fun loadApiKeyFromPreferences(context: Context): String? {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_API_KEY, null)
        }
        
        fun clearApiKeyFromPreferences(context: Context) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_API_KEY).apply()
        }

        fun loadData(resourceName: String) {
            val body = getAssetString(resourceName).replace("https://somewhere.com/", instance.serverUrl)
            instance.dispatcher.setResponse("/inApp/getMessages", MockResponse().setBody(body))
            IterableInternal.syncInApp()
        }

        private fun getAssetString(fileName: String): String =
            instance.context!!.resources.assets.open(fileName).bufferedReader().use { it.readText() }
    }

}