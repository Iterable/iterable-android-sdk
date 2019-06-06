package com.iterable.iterableapi.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.RestrictTo;
import android.widget.ImageView;

import com.iterable.iterableapi.IterableLogger;
import com.iterable.iterableapi.util.Future;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BitmapLoader {

    private static final int DEFAULT_TIMEOUT_MS = 3000;

    static public void loadBitmap(final ImageView imageView, final Uri uri) {
        Future.runAsync(new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                return fetchBitmap(imageView.getContext(), uri);
            }
        })
        .onSuccess(new Future.SuccessCallback<Bitmap>() {
            @Override
            public void onSuccess(Bitmap result) {
                imageView.setImageBitmap(result);
            }
        })
        .onFailure(new Future.FailureCallback() {
            @Override
            public void onFailure(Throwable throwable) {
                IterableLogger.e("BitmapLoader", "Error while loading image: " + uri.toString(), throwable);
            }
        });
    }

    static Bitmap fetchBitmap(Context context, Uri uri) throws IOException {
        File imageFile = File.createTempFile("itbl_", ".temp", context.getCacheDir());
        if (!downloadFile(uri, imageFile)) {
            throw new RuntimeException("Failed to download image file");
        }
        return BitmapFactory.decodeFile(imageFile.getAbsolutePath());
    }

    static boolean downloadFile(Uri uri, File file) throws IOException {
        URL url = new URL(uri.toString());

        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        HttpURLConnection urlConnection = null;

        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(DEFAULT_TIMEOUT_MS);
            urlConnection.setUseCaches(true);
            inputStream = urlConnection.getInputStream();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                return false;
            }

            if (inputStream != null) {
                outputStream = new FileOutputStream(file);
                byte[] buffer = new byte[2048];

                int readLength;
                while ((readLength = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, readLength);
                }

                return true;
            }

            return false;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }

            if (outputStream != null) {
                outputStream.close();
            }

            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
