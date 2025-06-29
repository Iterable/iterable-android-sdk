package com.iterable.iterableapi

import com.iterable.iterableapi.IterableConstants.ENDPOINT_DISABLE_DEVICE
import com.iterable.iterableapi.IterableConstants.ENDPOINT_GET_REMOTE_CONFIGURATION
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Async task to handle sending data to the Iterable server
 * Created by David Truong dt@iterable.com
 */
internal class IterableRequestTask : AsyncTask<IterableApiRequest, Void, IterableApiResponse>() {

    companion object {
        const val TAG = "IterableRequest"

        @JvmStatic
        var overrideUrl: String? = null

        const val POST_REQUEST_DEFAULT_TIMEOUT_MS = 3000    //3 seconds
        const val GET_REQUEST_DEFAULT_TIMEOUT_MS = 10000    //10 seconds
        const val RETRY_DELAY_MS = 2000L      //2 seconds
        const val MAX_RETRY_COUNT = 5

        const val ERROR_CODE_INVALID_JWT_PAYLOAD = "InvalidJwtPayload"
        const val ERROR_CODE_MISSING_JWT_PAYLOAD = "BadAuthorizationHeader"
        const val ERROR_CODE_JWT_USER_IDENTIFIERS_MISMATCHED = "JwtUserIdentifiersMismatched"

        private val handler = Handler(Looper.getMainLooper())

        @JvmStatic
        private fun retryRequestWithNewAuthToken(newAuthToken: String, iterableApiRequest: IterableApiRequest) {
            val request = IterableApiRequest(
                iterableApiRequest.apiKey,
                iterableApiRequest.resourcePath,
                iterableApiRequest.json,
                iterableApiRequest.requestType,
                newAuthToken,
                iterableApiRequest.legacyCallback!!
            )
            val requestTask = IterableRequestTask()
            requestTask.execute(request)
        }

        @WorkerThread
        @JvmStatic
        fun executeApiRequest(iterableApiRequest: IterableApiRequest?): IterableApiResponse? {
            var apiResponse: IterableApiResponse? = null
            var requestResult: String? = null

            if (iterableApiRequest != null) {
                var urlConnection: HttpURLConnection? = null

                IterableLogger.v(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n")
                var baseUrl = getBaseUrl()

                try {
                    if (overrideUrl != null && overrideUrl!!.isNotEmpty()) {
                        baseUrl = overrideUrl!!
                    }
                    if (iterableApiRequest.requestType == IterableApiRequest.GET) {
                        val builder = Uri.parse(baseUrl + iterableApiRequest.resourcePath).buildUpon()

                        val keys = iterableApiRequest.json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next() as String
                            builder.appendQueryParameter(key, iterableApiRequest.json.getString(key))
                        }

                        val url = URL(builder.build().toString())
                        urlConnection = url.openConnection() as HttpURLConnection

                        urlConnection.readTimeout = GET_REQUEST_DEFAULT_TIMEOUT_MS
                        urlConnection.connectTimeout = GET_REQUEST_DEFAULT_TIMEOUT_MS

                        urlConnection.setRequestProperty(IterableConstants.HEADER_API_KEY, iterableApiRequest.apiKey)
                        urlConnection.setRequestProperty(IterableConstants.HEADER_SDK_PLATFORM, "Android")
                        urlConnection.setRequestProperty(IterableConstants.HEADER_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER)
                        urlConnection.setRequestProperty(IterableConstants.KEY_SENT_AT, (Date().time / 1000).toString())
                        urlConnection.setRequestProperty(IterableConstants.HEADER_SDK_PROCESSOR_TYPE, iterableApiRequest.getProcessorType().toString())
                        if (iterableApiRequest.authToken != null) {
                            urlConnection.setRequestProperty(IterableConstants.HEADER_SDK_AUTHORIZATION, IterableConstants.HEADER_SDK_AUTH_FORMAT + iterableApiRequest.authToken)
                        }

                        IterableLogger.v(TAG, "GET Request \nURI : " + baseUrl + iterableApiRequest.resourcePath + buildHeaderString(urlConnection) + "\n body : \n" + iterableApiRequest.json.toString(2))

                    } else {
                        val url = URL(baseUrl + iterableApiRequest.resourcePath)
                        urlConnection = url.openConnection() as HttpURLConnection
                        urlConnection.doOutput = true
                        urlConnection.requestMethod = iterableApiRequest.requestType

                        urlConnection.readTimeout = POST_REQUEST_DEFAULT_TIMEOUT_MS
                        urlConnection.connectTimeout = POST_REQUEST_DEFAULT_TIMEOUT_MS

                        urlConnection.setRequestProperty("Accept", "application/json")
                        urlConnection.setRequestProperty("Content-Type", "application/json")
                        urlConnection.setRequestProperty(IterableConstants.HEADER_API_KEY, iterableApiRequest.apiKey)
                        urlConnection.setRequestProperty(IterableConstants.HEADER_SDK_PLATFORM, "Android")
                        urlConnection.setRequestProperty(IterableConstants.HEADER_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER)
                        urlConnection.setRequestProperty(IterableConstants.KEY_SENT_AT, (Date().time / 1000).toString())
                        urlConnection.setRequestProperty(IterableConstants.HEADER_SDK_PROCESSOR_TYPE, iterableApiRequest.getProcessorType().toString())
                        if (iterableApiRequest.authToken != null) {
                            urlConnection.setRequestProperty(IterableConstants.HEADER_SDK_AUTHORIZATION, IterableConstants.HEADER_SDK_AUTH_FORMAT + iterableApiRequest.authToken)
                        }

                        IterableLogger.v(TAG, "POST Request \nURI : " + baseUrl + iterableApiRequest.resourcePath + buildHeaderString(urlConnection) + "\n body : \n" + iterableApiRequest.json.toString(2))

                        val os = urlConnection.outputStream
                        val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                        writer.write(iterableApiRequest.json.toString())

                        writer.close()
                        os.close()
                    }

                    IterableLogger.v(TAG, "======================================")
                    val responseCode = urlConnection.responseCode

                    var error: String? = null

                    // Read the response body
                    try {
                        val `in`: BufferedReader
                        if (responseCode < 400) {
                            `in` = BufferedReader(
                                InputStreamReader(urlConnection.inputStream)
                            )
                        } else {
                            `in` = BufferedReader(
                                InputStreamReader(urlConnection.errorStream)
                            )
                        }
                        var inputLine: String?
                        val response = StringBuffer()
                        while (`in`.readLine().also { inputLine = it } != null) {
                            response.append(inputLine)
                        }
                        `in`.close()
                        requestResult = response.toString()
                    } catch (e: IOException) {
                        logError(iterableApiRequest, baseUrl, e)
                        error = e.message
                    }

                    // Parse JSON
                    var jsonResponse: JSONObject? = null
                    var jsonError: String? = null

                    try {
                        jsonResponse = JSONObject(requestResult)
                        IterableLogger.v(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n" +
                                "Response from : " + baseUrl + iterableApiRequest.resourcePath)
                        IterableLogger.v(TAG, jsonResponse.toString(2))
                    } catch (e: Exception) {
                        logError(iterableApiRequest, baseUrl, e)
                        jsonError = e.message
                    }

                    // Handle HTTP status codes
                    when {
                        responseCode == 401 -> {
                            if (matchesJWTErrorCodes(jsonResponse)) {
                                apiResponse = IterableApiResponse.failure(responseCode, requestResult, jsonResponse, "JWT Authorization header error")
                                IterableApi.getInstance().authManager.handleAuthFailure(iterableApiRequest.authToken, getMappedErrorCodeForMessage(jsonResponse))
                                // We handle the JWT Retry for both online and offline here rather than handling online request in onPostExecute
                                requestNewAuthTokenAndRetry(iterableApiRequest)
                            } else {
                                apiResponse = IterableApiResponse.failure(responseCode, requestResult, jsonResponse, "Invalid API Key")
                            }
                        }
                        responseCode >= 400 -> {
                            var errorMessage = "Invalid Request"

                            if (jsonResponse != null && jsonResponse.has("msg")) {
                                errorMessage = jsonResponse.getString("msg")
                            } else if (responseCode >= 500) {
                                errorMessage = "Internal Server Error"
                            }

                            apiResponse = IterableApiResponse.failure(responseCode, requestResult, jsonResponse, errorMessage)
                        }
                        responseCode == 200 -> {
                            when {
                                error == null && requestResult!!.isNotEmpty() -> {
                                    when {
                                        jsonError != null -> {
                                            apiResponse = IterableApiResponse.failure(responseCode, requestResult, jsonResponse, "Could not parse json: $jsonError")
                                        }
                                        jsonResponse != null -> {
                                            apiResponse = IterableApiResponse.success(responseCode, requestResult, jsonResponse)
                                        }
                                        else -> {
                                            apiResponse = IterableApiResponse.failure(responseCode, requestResult, jsonResponse, "Response is not a JSON object")
                                        }
                                    }
                                }
                                error == null && requestResult!!.isEmpty() -> {
                                    apiResponse = IterableApiResponse.failure(responseCode, requestResult, jsonResponse, "No data received")
                                }
                                error != null -> {
                                    apiResponse = IterableApiResponse.failure(responseCode, requestResult, jsonResponse, error)
                                }
                            }
                        }
                        else -> {
                            apiResponse = IterableApiResponse.failure(responseCode, requestResult, jsonResponse, "Received non-200 response: $responseCode")
                        }
                    }
                } catch (e: JSONException) {
                    logError(iterableApiRequest, baseUrl, e)
                    apiResponse = IterableApiResponse.failure(0, requestResult, null, e.message)
                } catch (e: IOException) {
                    logError(iterableApiRequest, baseUrl, e)
                    apiResponse = IterableApiResponse.failure(0, requestResult, null, e.message)
                } catch (e: ArrayIndexOutOfBoundsException) {
                    // This exception is sometimes thrown from the inside of HttpUrlConnection/OkHttp
                    logError(iterableApiRequest, baseUrl, e)
                    apiResponse = IterableApiResponse.failure(0, requestResult, null, e.message)
                } catch (e: Exception) {
                    logError(iterableApiRequest, baseUrl, e)
                    apiResponse = IterableApiResponse.failure(0, requestResult, null, e.message)
                } finally {
                    urlConnection?.disconnect()
                }
                IterableLogger.v(TAG, "======================================")
            }
            return apiResponse
        }

        @JvmStatic
        private fun getBaseUrl(): String {
            val config = IterableApi.getInstance().config
            val dataRegion = config.dataRegion
            var baseUrl = dataRegion.endpoint

            if (overrideUrl != null && overrideUrl!!.isNotEmpty()) {
                baseUrl = overrideUrl!!
            }

            return baseUrl
        }

        @JvmStatic
        private fun matchesErrorCode(jsonResponse: JSONObject?, errorCode: String): Boolean {
            return try {
                jsonResponse != null && jsonResponse.has("code") && jsonResponse.getString("code") == errorCode
            } catch (e: JSONException) {
                false
            }
        }

        @JvmStatic
        private fun getMappedErrorCodeForMessage(jsonResponse: JSONObject?): AuthFailureReason? {
            return try {
                if (jsonResponse == null || !jsonResponse.has("msg")) {
                    return null
                }

                val errorMessage = jsonResponse.getString("msg")

                when (errorMessage.lowercase(Locale.getDefault())) {
                    "exp must be less than 1 year from iat" -> AuthFailureReason.AUTH_TOKEN_EXPIRATION_INVALID
                    "jwt format is invalid" -> AuthFailureReason.AUTH_TOKEN_FORMAT_INVALID
                    "jwt token is expired" -> AuthFailureReason.AUTH_TOKEN_EXPIRED
                    "jwt is invalid" -> AuthFailureReason.AUTH_TOKEN_SIGNATURE_INVALID
                    "jwt payload requires a value for userid or email", "email could not be found" -> AuthFailureReason.AUTH_TOKEN_USER_KEY_INVALID
                    "jwt token has been invalidated" -> AuthFailureReason.AUTH_TOKEN_INVALIDATED
                    "invalid payload" -> AuthFailureReason.AUTH_TOKEN_PAYLOAD_INVALID
                    "jwt authorization header is not set" -> AuthFailureReason.AUTH_TOKEN_MISSING
                    else -> AuthFailureReason.AUTH_TOKEN_GENERIC_ERROR
                }
            } catch (e: JSONException) {
                null
            }
        }

        @JvmStatic
        private fun matchesJWTErrorCodes(jsonResponse: JSONObject?): Boolean {
            return matchesErrorCode(jsonResponse, ERROR_CODE_INVALID_JWT_PAYLOAD) || 
                   matchesErrorCode(jsonResponse, ERROR_CODE_MISSING_JWT_PAYLOAD) || 
                   matchesErrorCode(jsonResponse, ERROR_CODE_JWT_USER_IDENTIFIERS_MISMATCHED)
        }

        @JvmStatic
        private fun logError(iterableApiRequest: IterableApiRequest, baseUrl: String, e: Exception) {
            IterableLogger.e(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n" +
                    "Exception occurred for : " + baseUrl + iterableApiRequest.resourcePath)
            IterableLogger.e(TAG, e.message, e)
        }

        @JvmStatic
        private fun buildHeaderString(urlConnection: HttpURLConnection): String {
            val headerString = StringBuilder()
            headerString.append("\nHeaders { \n")
            val headerKeys = urlConnection.requestProperties.keys.iterator()
            while (headerKeys.hasNext()) {
                val key = headerKeys.next() as String
                if (isSensitive(key)) {
                    continue
                }
                headerString.append(key).append(" : ").append(urlConnection.requestProperties[key]).append("\n")
            }
            headerString.append("}")
            return headerString.toString()
        }

        @JvmStatic
        private fun isSensitive(key: String): Boolean {
            return (key == IterableConstants.HEADER_API_KEY) || key == IterableConstants.HEADER_SDK_AUTHORIZATION
        }

        @JvmStatic
        private fun requestNewAuthTokenAndRetry(iterableApiRequest: IterableApiRequest) {
            IterableApi.getInstance().authManager.setIsLastAuthTokenValid(false)
            val retryInterval = IterableApi.getInstance().authManager.getNextRetryInterval()
            IterableApi.getInstance().authManager.scheduleAuthTokenRefresh(retryInterval, false) { data ->
                try {
                    val newAuthToken = data.getString("newAuthToken")
                    retryRequestWithNewAuthToken(newAuthToken, iterableApiRequest)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private var retryCount = 0
    private lateinit var iterableApiRequest: IterableApiRequest

    /**
     * Sends the given request to Iterable using a HttpUserConnection
     * Reference - http://developer.android.com/reference/java/net/HttpURLConnection.html
     *
     * @param params
     * @return
     */
    override fun doInBackground(vararg params: IterableApiRequest): IterableApiResponse? {
        if (params.isNotEmpty()) {
            iterableApiRequest = params[0]
        }
        return executeApiRequest(iterableApiRequest)
    }

    override fun onPostExecute(response: IterableApiResponse?) {
        if (response == null) return

        if (shouldRetry(response)) {
            retryRequestWithDelay()
            return
        } else if (response.success) {
            handleSuccessResponse(response)
        } else {
            handleErrorResponse(response)
        }

        if (iterableApiRequest.legacyCallback != null) {
            iterableApiRequest.legacyCallback!!.execute(response.responseBody)
        }
        super.onPostExecute(response)
    }

    private fun shouldRetry(response: IterableApiResponse): Boolean {
        return !response.success && response.responseCode >= 500 && retryCount <= MAX_RETRY_COUNT
    }

    private fun retryRequestWithDelay() {
        val requestTask = IterableRequestTask()
        requestTask.setRetryCount(retryCount + 1)

        val delay = if (retryCount > 2) RETRY_DELAY_MS * retryCount else 0

        handler.postDelayed({
            requestTask.execute(iterableApiRequest)
        }, delay)
    }

    private fun handleSuccessResponse(response: IterableApiResponse) {
        if (!Objects.equals(iterableApiRequest.resourcePath, ENDPOINT_GET_REMOTE_CONFIGURATION) && 
            !Objects.equals(iterableApiRequest.resourcePath, ENDPOINT_DISABLE_DEVICE)) {
            IterableApi.getInstance().authManager.resetFailedAuth()
            IterableApi.getInstance().authManager.pauseAuthRetries(false)
            IterableApi.getInstance().authManager.setIsLastAuthTokenValid(true)
        }

        if (iterableApiRequest.successCallback != null) {
            iterableApiRequest.successCallback!!.onSuccess(response.responseJson)
        }
    }

    private fun handleErrorResponse(response: IterableApiResponse) {
        if (iterableApiRequest.failureCallback != null) {
            iterableApiRequest.failureCallback!!.onFailure(response.errorMessage, response.responseJson)
        }
    }

    fun setRetryCount(count: Int) {
        retryCount = count
    }
}

/**
 *  Iterable Request object
 */
internal class IterableApiRequest {

    companion object {
        private const val TAG = "IterableApiRequest"

        const val GET = "GET"
        const val POST = "POST"

        @JvmStatic
        fun fromJSON(
            jsonData: JSONObject,
            @Nullable onSuccess: IterableHelper.SuccessHandler?,
            @Nullable onFailure: IterableHelper.FailureHandler?
        ): IterableApiRequest? {
            return try {
                val apikey = jsonData.getString("apiKey")
                val resourcePath = jsonData.getString("resourcePath")
                val requestType = jsonData.getString("requestType")
                var authToken = ""
                if (jsonData.has("authToken")) {
                    authToken = jsonData.getString("authToken")
                }
                val json = jsonData.getJSONObject("data")
                IterableApiRequest(apikey, resourcePath, json, requestType, authToken, onSuccess, onFailure)
            } catch (e: JSONException) {
                IterableLogger.e(TAG, "Failed to create Iterable request from JSON")
                null
            }
        }
    }

    enum class ProcessorType {
        ONLINE {
            @NonNull
            override fun toString(): String {
                return "Online"
            }
        },
        OFFLINE {
            @NonNull
            override fun toString(): String {
                return "Offline"
            }
        }
    }

    val apiKey: String
    val baseUrl: String?
    val resourcePath: String
    val json: JSONObject
    val requestType: String
    val authToken: String?

    private var processorType = ProcessorType.ONLINE
    var legacyCallback: IterableHelper.IterableActionHandler? = null
    var successCallback: IterableHelper.SuccessHandler? = null
    var failureCallback: IterableHelper.FailureHandler? = null

    constructor(
        apiKey: String,
        baseUrl: String?,
        resourcePath: String,
        json: JSONObject,
        requestType: String,
        authToken: String?,
        onSuccess: IterableHelper.SuccessHandler?,
        onFailure: IterableHelper.FailureHandler?
    ) {
        this.apiKey = apiKey
        this.baseUrl = baseUrl
        this.resourcePath = resourcePath
        this.json = json
        this.requestType = requestType
        this.authToken = authToken
        this.successCallback = onSuccess
        this.failureCallback = onFailure
    }

    constructor(
        apiKey: String,
        resourcePath: String,
        json: JSONObject,
        requestType: String,
        authToken: String?,
        onSuccess: IterableHelper.SuccessHandler?,
        onFailure: IterableHelper.FailureHandler?
    ) {
        this.apiKey = apiKey
        this.baseUrl = null
        this.resourcePath = resourcePath
        this.json = json
        this.requestType = requestType
        this.authToken = authToken
        this.successCallback = onSuccess
        this.failureCallback = onFailure
    }

    constructor(
        apiKey: String,
        resourcePath: String,
        json: JSONObject,
        requestType: String,
        authToken: String?,
        callback: IterableHelper.IterableActionHandler?
    ) {
        this.apiKey = apiKey
        this.baseUrl = null
        this.resourcePath = resourcePath
        this.json = json
        this.requestType = requestType
        this.authToken = authToken
        this.legacyCallback = callback
    }

    fun getProcessorType(): ProcessorType {
        return processorType
    }

    internal fun setProcessorType(processorType: ProcessorType) {
        this.processorType = processorType
    }

    @Throws(JSONException::class)
    fun toJSONObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("apiKey", this.apiKey)
        jsonObject.put("resourcePath", this.resourcePath)
        jsonObject.put("authToken", this.authToken)
        jsonObject.put("requestType", this.requestType)
        jsonObject.put("data", this.json)
        return jsonObject
    }
}

internal class IterableApiResponse(
    val success: Boolean,
    val responseCode: Int,
    val responseBody: String?,
    val responseJson: JSONObject?,
    val errorMessage: String?
) {

    companion object {
        @JvmStatic
        fun success(responseCode: Int, body: String?, @NonNull json: JSONObject): IterableApiResponse {
            return IterableApiResponse(true, responseCode, body, json, null)
        }

        @JvmStatic
        fun failure(responseCode: Int, body: String?, @Nullable json: JSONObject?, errorMessage: String?): IterableApiResponse {
            return IterableApiResponse(false, responseCode, body, json, errorMessage)
        }
    }
}