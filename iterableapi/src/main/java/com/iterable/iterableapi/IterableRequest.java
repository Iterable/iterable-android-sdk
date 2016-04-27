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
class IterableRequest extends AsyncTask<String, Integer, String> {
    static final String TAG = "IterableRequest";

    private Exception exception;

    /**
     * Sends the given request to Iterable using a HttpUserConnection
     * @param params array of parameters
     *               iterableBaseUrl
     *               apiKey
     *               uri
     *               json
     * @return
     */
    protected String doInBackground(String... params) {
        String iterableBaseUrl = params[0];
        String apiKey = params[1];
        String uri = params[2];
        String json = params[3];

        URL url;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL(iterableBaseUrl + uri);

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");

            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Content-type", "application/json");
            urlConnection.setRequestProperty("api_key", apiKey);

            OutputStream os = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(json);
            writer.flush();
            writer.close();
            os.close();

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
            String inputString = s.hasNext() ? s.next() : "";
            Log.d(TAG, inputString);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return null;
    }
}
