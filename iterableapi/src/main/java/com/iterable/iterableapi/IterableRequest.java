package com.iterable.iterableapi;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Async task to handle sending data to the Iterable server
 * Created by David Truong dt@iterable.com
 */
class IterableRequest extends AsyncTask<IterableApiRequest, Void, String> {
    static final String TAG = "IterableRequest";
    static final String AUTHENTICATION_IO_EXCEPTION = "Received authentication challenge is null";
    static final int DEFAULT_TIMEOUT = 10000;
    static final long MAX_RETRY_DELAY = 180000;

    long retryDelay = 10000;

    static final String iterableBaseUrl = "https://api.iterable.com/api/";

    static String overrideUrl;

    /**
     * Sends the given request to Iterable using a HttpUserConnection
     * Reference - http://developer.android.com/reference/java/net/HttpURLConnection.html
     * @param params
     * @return
     */
    protected String doInBackground(IterableApiRequest... params) {
        IterableApiRequest iterableApiRequest = params[0];

        String requestResult = null;
        if (iterableApiRequest != null) {
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                String baseUrl = (overrideUrl != null && !overrideUrl.isEmpty()) ? overrideUrl : iterableBaseUrl;
                url = new URL(baseUrl + iterableApiRequest.resourcePath);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");

                urlConnection.setReadTimeout(DEFAULT_TIMEOUT);
                urlConnection.setConnectTimeout(DEFAULT_TIMEOUT);

                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty(IterableConstants.KEY_API_KEY, iterableApiRequest.apiKey);

                OutputStream os = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(iterableApiRequest.json);
                writer.close();
                os.close();

                int responseCode = urlConnection.getResponseCode();
                if (responseCode >= 400) {
                    InputStream errorStream = urlConnection.getErrorStream();
                    java.util.Scanner scanner = new java.util.Scanner(errorStream).useDelimiter("\\A");
                    requestResult = scanner.hasNext() ? scanner.next() : "";
                    Log.d(TAG, "Invalid Request for: " + iterableApiRequest.resourcePath);
                    Log.d(TAG, requestResult);
                }
            } catch (FileNotFoundException e) {
                String mess = e.getMessage();
                e.printStackTrace();
            } catch (IOException e) {
                String mess = e.getMessage();
                if (mess.equals(AUTHENTICATION_IO_EXCEPTION)) {
                    Log.d(TAG, "Invalid API Key");
                } else
                {
                    retryRequest(iterableApiRequest);
                }
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                retryRequest(iterableApiRequest);
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
        super.onPostExecute(s);
    }

    private void retryRequest(IterableApiRequest iterableApiRequest) {
        try {
            Thread.sleep(retryDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        retryDelay = Math.min(retryDelay * 2, MAX_RETRY_DELAY); //exponential retry backoff
        doInBackground(iterableApiRequest);
    }

}

class IterableApiRequest {
    String apiKey = "";
    String resourcePath = "";
    String json = "";

    public IterableApiRequest(String apiKey, String resourcePath, String json){
        this.apiKey = apiKey;
        this.resourcePath = resourcePath;
        this.json = json;
    }
}
