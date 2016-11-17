package com.iterable.iterableapi;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
    static final String AUTHENTICATION_IO_EXCEPTION = "Received authentication challenge is null";

    static final String iterableBaseUrl = "https://api.iterable.com/api/";
    static String overrideUrl;

    static final int DEFAULT_TIMEOUT_MS = 10000;   //10 seconds
    static final long RETRY_DELAY_MS = 10000;      //10 seconds
    static final int MAX_RETRY_COUNT = 3;

    int retryCount = 0;
    IterableApiRequest iterableApiRequest;
    boolean retryRequest;

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

        if (retryCount > 0) {
            try {
                Thread.sleep(RETRY_DELAY_MS * retryCount);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String requestResult = null;
        if (iterableApiRequest != null) {
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                String baseUrl = (overrideUrl != null && !overrideUrl.isEmpty()) ? overrideUrl : iterableBaseUrl;
                if (iterableApiRequest.requestType == IterableApiRequest.GET) {
                    String urlString = baseUrl + iterableApiRequest.resourcePath + "?" + IterableConstants.KEY_API_KEY + "=" + iterableApiRequest.apiKey;
                    Iterator<?> keys = iterableApiRequest.json.keys();

                    while( keys.hasNext() ) {
                        String key = (String) keys.next();
                        urlString = urlString + "&&" + key + "=" + iterableApiRequest.json.getString(key);
                    }

                    url = new URL(urlString);
                    urlConnection = (HttpURLConnection) url.openConnection();

                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(urlConnection.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    requestResult = response.toString();

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
                if (responseCode >= 400) {
                    InputStream errorStream = urlConnection.getErrorStream();
                    java.util.Scanner scanner = new java.util.Scanner(errorStream).useDelimiter("\\A");
                    requestResult = scanner.hasNext() ? scanner.next() : "";
                    IterableLogger.d(TAG, "Invalid Request for: " + iterableApiRequest.resourcePath);
                    IterableLogger.d(TAG, requestResult);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                String message = e.getMessage();
                if (message.equals(AUTHENTICATION_IO_EXCEPTION)) {
                    IterableLogger.d(TAG, "Invalid API Key");
                } else {
                    retryRequest = true;
                }
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                retryRequest = true;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }
        return requestResult;
    }

    @Override
    protected void onPostExecute(String s) {
        if (retryRequest && retryCount <= MAX_RETRY_COUNT) {
            IterableRequest request = new IterableRequest();
            request.setRetryCount(retryCount + 1);
            request.execute(iterableApiRequest);
        }
        if (iterableApiRequest.callback != null) {
            iterableApiRequest.callback.execute(s);
        }
        super.onPostExecute(s);
    }

    protected void setRetryCount(int count) {
        retryCount = count;
    }

}

class IterableApiRequest {

    public interface OnCallbackHandlerListener{
        void execute(String s);
    }
    static String GET = "GET";
    static String POST = "POST";

    String apiKey = "";
    String resourcePath = "";
    JSONObject json;
    String requestType = "";

    OnCallbackHandlerListener callback;

    public IterableApiRequest(String apiKey, String resourcePath, JSONObject json, String requestType, OnCallbackHandlerListener callback){
        this.apiKey = apiKey;
        this.resourcePath = resourcePath;
        this.json = json;
        this.requestType = requestType;
        this.callback = callback;
    }
}
