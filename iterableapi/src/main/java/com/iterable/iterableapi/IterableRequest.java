package com.iterable.iterableapi;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by davidtruong on 4/21/16.
 */
class IterableRequest extends AsyncTask<String, Integer, String> {
    private Exception exception;

    // iterableBaseUrl
    // _apiKey
    // String uri
    // String json
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
            //TODO: read input stream to validate request status

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
