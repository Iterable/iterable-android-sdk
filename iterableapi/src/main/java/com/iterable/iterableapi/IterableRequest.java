package com.iterable.iterableapi;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

/**
 * Async task to handle sending data to the Iterable server
 * Created by David Truong dt@iterable.com
 */
class IterableRequest extends AsyncTask<IterableApiRequest, Void, String> {
    static final String TAG = "IterableRequest";
    static final String ITERABLE_BASE_URL = "https://api.iterable.com/api/";

    static String overrideUrl;

    static final int DEFAULT_TIMEOUT_MS = 3000;   //3 seconds
    static final long RETRY_DELAY_MS = 2000;      //2 seconds
    static final int MAX_RETRY_COUNT = 5;

    int retryCount = 0;
    IterableApiRequest iterableApiRequest;
    boolean retryRequest;

    private boolean success = false;
    private String failureReason;
    private String requestResult;
    private @Nullable JSONObject requestResultJson;

    /**
     * Sends the given request to Iterable using a HttpUserConnection
     * Reference - http://developer.android.com/reference/java/net/HttpURLConnection.html
     * @param params
     * @return
     */
    protected String doInBackground(IterableApiRequest... params) {
        if (params != null && params.length > 0) {
            iterableApiRequest = params[0];
        }

        requestResult = null;
        if (iterableApiRequest != null) {
            URL url;
            HttpURLConnection urlConnection = null;

            IterableLogger.v(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");
            String baseUrl = (iterableApiRequest.baseUrl != null && !iterableApiRequest.baseUrl.isEmpty()) ? iterableApiRequest.baseUrl :
                    ITERABLE_BASE_URL;
            try {
                if (overrideUrl != null && !overrideUrl.isEmpty()) {
                    baseUrl = overrideUrl;
                }
                if (iterableApiRequest.requestType == IterableApiRequest.GET) {
                    Uri.Builder builder = Uri.parse(baseUrl + iterableApiRequest.resourcePath).buildUpon();

                    Iterator<?> keys = iterableApiRequest.json.keys();
                    while (keys.hasNext()) {
                        String key = (String) keys.next();
                        builder.appendQueryParameter(key, iterableApiRequest.json.getString(key));
                    }

                    url = new URL(builder.build().toString());
                    urlConnection = (HttpURLConnection) url.openConnection();

                    urlConnection.setReadTimeout(DEFAULT_TIMEOUT_MS);
                    urlConnection.setConnectTimeout(DEFAULT_TIMEOUT_MS);

                    urlConnection.setRequestProperty(IterableConstants.HEADER_API_KEY, iterableApiRequest.apiKey);
                    urlConnection.setRequestProperty(IterableConstants.HEADER_SDK_PLATFORM, "Android");
                    urlConnection.setRequestProperty(IterableConstants.HEADER_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER);

                    if (iterableApiRequest.authToken != null) {
                        urlConnection.setRequestProperty(IterableConstants.HEADER_SDK_AUTHORIZATION, IterableConstants.HEADER_SDK_AUTH_FORMAT + iterableApiRequest.authToken);
                    }

                    IterableLogger.v(TAG, "GET Request \nURI : " + baseUrl + iterableApiRequest.resourcePath + buildHeaderString(urlConnection) + "\n body : \n" + iterableApiRequest.json.toString(2));

                } else {
                    url = new URL(baseUrl + iterableApiRequest.resourcePath);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestMethod(iterableApiRequest.requestType);

                    urlConnection.setReadTimeout(DEFAULT_TIMEOUT_MS);
                    urlConnection.setConnectTimeout(DEFAULT_TIMEOUT_MS);

                    urlConnection.setRequestProperty("Accept", "application/json");
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    urlConnection.setRequestProperty(IterableConstants.HEADER_API_KEY, iterableApiRequest.apiKey);
                    urlConnection.setRequestProperty(IterableConstants.HEADER_SDK_PLATFORM, "Android");
                    urlConnection.setRequestProperty(IterableConstants.HEADER_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER);

                    if (iterableApiRequest.authToken != null) {
                        urlConnection.setRequestProperty(IterableConstants.HEADER_SDK_AUTHORIZATION, IterableConstants.HEADER_SDK_AUTH_FORMAT + iterableApiRequest.authToken);
                    }

                    IterableLogger.v(TAG, "POST Request \nURI : " + baseUrl + iterableApiRequest.resourcePath + buildHeaderString(urlConnection) + "\n body : \n" + iterableApiRequest.json.toString(2));

                    OutputStream os = urlConnection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(iterableApiRequest.json.toString());

                    writer.close();
                    os.close();
                }

                IterableLogger.v(TAG, "======================================");
                int responseCode = urlConnection.getResponseCode();

                String error = null;

                // Read the response body
                try {
                    BufferedReader in;
                    if (responseCode < 400) {
                        in = new BufferedReader(
                                new InputStreamReader(urlConnection.getInputStream()));
                    } else {
                        in = new BufferedReader(
                                new InputStreamReader(urlConnection.getErrorStream()));
                    }
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    requestResult = response.toString();
                } catch (IOException e) {
                    logError(baseUrl, e);
                    error = e.getMessage();
                }

                // Parse JSON
                JSONObject jsonResponse = null;
                String jsonError = null;

                try {
                    jsonResponse = new JSONObject(requestResult);
                    IterableLogger.v(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n" +
                            "Response from : " + baseUrl + iterableApiRequest.resourcePath);
                    IterableLogger.v(TAG, jsonResponse.toString(2));
                } catch (Exception e) {
                    logError(baseUrl, e);
                    jsonError = e.getMessage();
                }

                // Handle HTTP status codes
                if (responseCode == 401) {
                    handleFailure("Invalid API Key", jsonResponse);
                } else if (responseCode >= 400) {
                    String errorMessage = "Invalid Request";

                    if (jsonResponse != null && jsonResponse.has("msg")) {
                        errorMessage = jsonResponse.getString("msg");
                    } else if (responseCode >= 500) {
                        errorMessage = "Internal Server Error";
                        retryRequest = true;
                    }

                    handleFailure(errorMessage, jsonResponse);
                } else if (responseCode == 200) {
                    if (error == null && requestResult.length() > 0) {
                        if (jsonError != null) {
                            handleFailure("Could not parse json: " + jsonError, null);
                        } else if (jsonResponse != null) {
                            handleSuccess(jsonResponse);
                        } else {
                            handleFailure("Response is not a JSON object", jsonResponse);
                        }
                    } else if (error == null && requestResult.length() == 0) {
                        handleFailure("No data received", jsonResponse);
                    } else if (error != null) {
                        handleFailure(error, null);
                    }
                } else {
                    handleFailure("Received non-200 response: " + responseCode, jsonResponse);
                }
            } catch (JSONException e) {
                logError(baseUrl, e);
                handleFailure(e.getMessage(), null);
            } catch (IOException e) {
                logError(baseUrl, e);
                handleFailure(e.getMessage(), null);
            } catch (ArrayIndexOutOfBoundsException e) {
                // This exception is sometimes thrown from the inside of HttpUrlConnection/OkHttp
                logError(baseUrl, e);
                handleFailure(e.getMessage(), null);
            } catch (Exception e) {
                logError(baseUrl, e);
                handleFailure(e.getMessage(), null);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            IterableLogger.v(TAG, "======================================");
        }
        return requestResult;
    }

    private void logError(String baseUrl, Exception e) {
        IterableLogger.e(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n" +
                "Exception occurred for : " + baseUrl + iterableApiRequest.resourcePath);
        IterableLogger.e(TAG, e.getMessage(), e);
    }

    private String buildHeaderString(HttpURLConnection urlConnection) {
        StringBuilder headerString = new StringBuilder();
        headerString.append("\nHeaders { \n");
        Iterator<?> headerKeys = urlConnection.getRequestProperties().keySet().iterator();
        while (headerKeys.hasNext()) {
            String key = (String) headerKeys.next();
            headerString.append(key + " : " + urlConnection.getRequestProperties().get(key) + "\n");
        }
        headerString.append("}");
        return headerString.toString();
    }

    private void handleSuccess(@NonNull JSONObject data) {
        requestResultJson = data;
        success = true;
    }

    private void handleFailure(@NonNull String reason, @Nullable JSONObject data) {
        requestResultJson = data;
        failureReason = reason;
        success = false;
    }

    @Override
    protected void onPostExecute(String s) {
        if (retryRequest && retryCount <= MAX_RETRY_COUNT) {
            final IterableRequest request = new IterableRequest();
            request.setRetryCount(retryCount + 1);

            long delay = 0;
            if (retryCount > 2) {
                delay = RETRY_DELAY_MS * retryCount;
            }

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    request.execute(iterableApiRequest);
                }
            }, delay);
            return;
        } else if (success) {
            if (iterableApiRequest.successCallback != null) {
                iterableApiRequest.successCallback.onSuccess(requestResultJson);
            }
        } else {
            if (iterableApiRequest.failureCallback != null) {
                iterableApiRequest.failureCallback.onFailure(failureReason, requestResultJson);
            }
        }
        if (iterableApiRequest.legacyCallback != null) {
            iterableApiRequest.legacyCallback.execute(s);
        }
        super.onPostExecute(s);
    }

    protected void setRetryCount(int count) {
        retryCount = count;
    }

}

/**
 *  Iterable Request object
 */
class IterableApiRequest {
    static final String GET = "GET";
    static final String POST = "POST";

    final String apiKey;
    final String baseUrl;
    final String resourcePath;
    final JSONObject json;
    final String requestType;
    final String authToken;

    IterableHelper.IterableActionHandler legacyCallback;
    IterableHelper.SuccessHandler successCallback;
    IterableHelper.FailureHandler failureCallback;

    IterableApiRequest(String apiKey, String baseUrl, String resourcePath, JSONObject json, String requestType, String authToken, IterableHelper.SuccessHandler onSuccess, IterableHelper.FailureHandler onFailure) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.resourcePath = resourcePath;
        this.json = json;
        this.requestType = requestType;
        this.authToken = authToken;
        this.successCallback = onSuccess;
        this.failureCallback = onFailure;
    }

    IterableApiRequest(String apiKey, String resourcePath, JSONObject json, String requestType, String authToken, IterableHelper.SuccessHandler onSuccess, IterableHelper.FailureHandler onFailure) {
        this.apiKey = apiKey;
        this.baseUrl = null;
        this.resourcePath = resourcePath;
        this.json = json;
        this.requestType = requestType;
        this.authToken = authToken;
        this.successCallback = onSuccess;
        this.failureCallback = onFailure;
    }

    IterableApiRequest(String apiKey, String resourcePath, JSONObject json, String requestType, String authToken, final IterableHelper.IterableActionHandler callback) {
        this.apiKey = apiKey;
        this.baseUrl = null;
        this.resourcePath = resourcePath;
        this.json = json;
        this.requestType = requestType;
        this.authToken = authToken;
        this.legacyCallback = callback;
    }
}
