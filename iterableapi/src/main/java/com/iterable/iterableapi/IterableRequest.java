package com.iterable.iterableapi;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;

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

    static final String iterableBaseUrl = "https://api.iterable.com/api/";

    static String overrideUrl;

    static final int DEFAULT_TIMEOUT_MS = 1000;   //1 seconds
    static final long RETRY_DELAY_MS = 2000;      //2 seconds
    static final int MAX_RETRY_COUNT = 5;

    int retryCount = 0;
    IterableApiRequest iterableApiRequest;
    boolean retryRequest;

    private boolean success = false;
    private String failureReason;
    private String requestResult;
    private JSONObject requestResultJson;

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

            try {
                String baseUrl = (overrideUrl != null && !overrideUrl.isEmpty()) ? overrideUrl : iterableBaseUrl;
                if (iterableApiRequest.requestType == IterableApiRequest.GET) {
                    Uri.Builder builder = Uri.parse(baseUrl+iterableApiRequest.resourcePath).buildUpon();
                    builder.appendQueryParameter(IterableConstants.KEY_API_KEY, iterableApiRequest.apiKey);

                    Iterator<?> keys = iterableApiRequest.json.keys();
                    while( keys.hasNext() ) {
                        String key = (String) keys.next();
                        builder.appendQueryParameter(key, iterableApiRequest.json.getString(key));
                    }

                    url = new URL(builder.build().toString());
                    urlConnection = (HttpURLConnection) url.openConnection();
                } else {
                    url = new URL(baseUrl + iterableApiRequest.resourcePath);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestMethod(iterableApiRequest.requestType);

                    urlConnection.setReadTimeout(DEFAULT_TIMEOUT_MS);
                    urlConnection.setConnectTimeout(DEFAULT_TIMEOUT_MS);

                    urlConnection.setRequestProperty("Accept", "application/json");
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    urlConnection.setRequestProperty(IterableConstants.KEY_API_KEY, iterableApiRequest.apiKey);

                    OutputStream os = urlConnection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(iterableApiRequest.json.toString());

                    writer.close();
                    os.close();
                }

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
                    IterableLogger.e(TAG, e.getMessage());
                    error = e.getMessage();
                }

                // Parse JSON
                JSONObject jsonResponse = null;
                String jsonError = null;
                try {
                    jsonResponse = new JSONObject(requestResult);
                } catch (Exception e) {
                    IterableLogger.e(TAG, e.getMessage());
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
                IterableLogger.e(TAG, e.getMessage());
                handleFailure(e.getMessage(), null);
            } catch (IOException e) {
                IterableLogger.e(TAG, e.getMessage());
                handleFailure(e.getMessage(), null);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }
        return requestResult;
    }

    private void handleSuccess(JSONObject data) {
        requestResultJson = data;
        success = true;
    }

    private void handleFailure(String reason, JSONObject data) {
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
        }
        else {
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
    static String GET = "GET";
    static String POST = "POST";

    String apiKey = "";
    String resourcePath = "";
    JSONObject json;
    String requestType = "";

    IterableHelper.IterableActionHandler legacyCallback;
    IterableHelper.SuccessHandler successCallback;
    IterableHelper.FailureHandler failureCallback;

    public IterableApiRequest(String apiKey, String resourcePath, JSONObject json, String requestType, IterableHelper.SuccessHandler onSuccess, IterableHelper.FailureHandler onFailure) {
        this.apiKey = apiKey;
        this.resourcePath = resourcePath;
        this.json = json;
        this.requestType = requestType;
        this.successCallback = onSuccess;
        this.failureCallback = onFailure;
    }

    public IterableApiRequest(String apiKey, String resourcePath, JSONObject json, String requestType, final IterableHelper.IterableActionHandler callback) {
        this.apiKey = apiKey;
        this.resourcePath = resourcePath;
        this.json = json;
        this.requestType = requestType;
        this.legacyCallback = callback;
    }
}
