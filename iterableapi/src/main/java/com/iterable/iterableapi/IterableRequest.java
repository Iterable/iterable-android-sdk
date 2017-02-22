package com.iterable.iterableapi;

import android.net.Uri;
import android.os.AsyncTask;

import org.json.JSONException;
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

    static final String iterableBaseUrl = "https://api.iterable.com/api/";

    static String overrideUrl;

    static final int DEFAULT_TIMEOUT_MS = 1000;   //1 seconds
    static final long RETRY_DELAY_MS = 2000;      //2 seconds
    static final int MAX_RETRY_COUNT = 5;

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

        if (retryCount > 2) {
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
                    Uri.Builder builder = Uri.parse(baseUrl+iterableApiRequest.resourcePath).buildUpon();
                    builder.appendQueryParameter(IterableConstants.KEY_API_KEY, iterableApiRequest.apiKey);

                    Iterator<?> keys = iterableApiRequest.json.keys();
                    while( keys.hasNext() ) {
                        String key = (String) keys.next();
                        builder.appendQueryParameter(key, iterableApiRequest.json.getString(key));
                    }

                    url = new URL(builder.build().toString());
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

                } else if (iterableApiRequest.requestType== IterableApiRequest.REDIRECT) {
                    url = new URL(iterableApiRequest.resourcePath);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setReadTimeout(DEFAULT_TIMEOUT_MS);
                    urlConnection.setInstanceFollowRedirects(false);
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
                    if (responseCode >= 500) {
                        retryRequest = true;
                    } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        IterableLogger.d(TAG, "Invalid API Key");
                    }
                    InputStream errorStream = urlConnection.getErrorStream();
                    java.util.Scanner scanner = new java.util.Scanner(errorStream).useDelimiter("\\A");
                    requestResult = scanner.hasNext() ? scanner.next() : "";
                    IterableLogger.d(TAG, "Invalid Request for: " + iterableApiRequest.resourcePath);
                    IterableLogger.d(TAG, requestResult);
                } else if (iterableApiRequest.requestType== IterableApiRequest.REDIRECT) {
                    if (responseCode >= 300) {
                        String newUrl = urlConnection.getHeaderField("Location");
                        requestResult = newUrl;
                    } else {
                        //pass back original url
                        requestResult = url.toString();
                    }
                }
            } catch (JSONException e) {
                IterableLogger.e(TAG, e.getMessage());
            } catch (IOException e) {
                IterableLogger.e(TAG, e.getMessage());
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
        } else if (iterableApiRequest.callback != null) {
            iterableApiRequest.callback.execute(s);
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
    static String REDIRECT = "REDIRECT";

    String apiKey = "";
    String resourcePath = "";
    JSONObject json;
    String requestType = "";

    IterableHelper.IterableActionHandler callback;

    public IterableApiRequest(String apiKey, String resourcePath, JSONObject json, String requestType, IterableHelper.IterableActionHandler callback){
        this.apiKey = apiKey;
        this.resourcePath = resourcePath;
        this.json = json;
        this.requestType = requestType;
        this.callback = callback;
    }
}
