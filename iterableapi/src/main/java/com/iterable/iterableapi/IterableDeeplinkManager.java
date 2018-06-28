package com.iterable.iterableapi;

import android.os.AsyncTask;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class IterableDeeplinkManager {

    private static Pattern deeplinkPattern = Pattern.compile(IterableConstants.ITBL_DEEPLINK_IDENTIFIER);

    /**
     * Tracks a link click and passes the redirected URL to the callback
     * @param url The URL that was clicked
     * @param callback The callback to execute the original URL is retrieved
     */
    static void getAndTrackDeeplink(String url, IterableHelper.IterableActionHandler callback) {
        if (url != null) {
            Matcher m = deeplinkPattern.matcher(url);
            if (m.find()) {
                new RedirectTask(callback).execute(url);
            } else {
                callback.execute(url);
            }
        } else {
            callback.execute(null);
        }
    }

    private static class RedirectTask extends AsyncTask<String, Void, String> {
        static final String TAG = "RedirectTask";
        static final int DEFAULT_TIMEOUT_MS = 1000;   //1 seconds

        private IterableHelper.IterableActionHandler callback;

        RedirectTask(IterableHelper.IterableActionHandler callback) {
            this.callback = callback;
        }

        @Override
        protected String doInBackground(String... params) {
            if (params == null || params.length == 0) {
                return null;
            }

            String urlString = params[0];
            HttpURLConnection urlConnection = null;

            try {
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setReadTimeout(DEFAULT_TIMEOUT_MS);
                urlConnection.setInstanceFollowRedirects(false);

                int responseCode = urlConnection.getResponseCode();

                if (responseCode >= 400) {
                    IterableLogger.d(TAG, "Invalid Request for: " + urlString + ", returned code " + responseCode);
                } else if (responseCode >= 300) {
                    urlString = urlConnection.getHeaderField(IterableConstants.LOCATION_HEADER_FIELD);
                }
            } catch (Exception e) {
                IterableLogger.e(TAG, e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return urlString;
        }

        @Override
        protected void onPostExecute(String s) {
            if (callback != null) {
                callback.execute(s);
            }
        }
    }
}
