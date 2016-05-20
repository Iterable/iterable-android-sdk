package com.iterable.iterableapi;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Async task to handle sending data to the Iterable server
 * Created by davidtruong on 4/21/16.
 */
class IterableRequest extends AsyncTask<IterableApiRequest, Void, String> {
    static final String TAG = "IterableRequest";

    /**
     * Sends the given request to Iterable using a HttpUserConnection
     * Reference - http://developer.android.com/reference/java/net/HttpURLConnection.html
     * @param params
     * @return
     */
    protected String doInBackground(IterableApiRequest... params) {
        //TODO: perhaps loop through all the request parameters
        IterableApiRequest iterableApiRequest = params[0];
        if (iterableApiRequest != null) {
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(iterableApiRequest.url + iterableApiRequest.resourcePath);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");

                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty(IterableConstants.KEY_API_KEY, iterableApiRequest.apiKey);

                OutputStream os = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(iterableApiRequest.json);
                writer.close();
                os.close();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
                String inputString = s.hasNext() ? s.next() : "";
                Log.d(TAG, inputString); //TODO: pass back as the result

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }

        return null;
    }
}

class IterableApiRequest {
    String url = "";
    String apiKey = "";
    String resourcePath = "";
    String json = "";

    public IterableApiRequest(String url, String apiKey, String resourcePath, String json){
        this.url = url;
        this.apiKey = apiKey;
        this.resourcePath = resourcePath;
        this.json = json;
    }
}
