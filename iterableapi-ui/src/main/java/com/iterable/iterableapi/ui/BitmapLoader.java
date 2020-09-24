package com.iterable.iterableapi.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.view.ViewCompat;
import android.widget.ImageView;

import com.iterable.iterableapi.IterableLogger;
import com.iterable.iterableapi.util.Future;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BitmapLoader {

    private static final int DEFAULT_TIMEOUT_MS = 3000;

    public static void loadBitmap(final @NonNull ImageView imageView, final @Nullable Uri uri) {
        if (uri == null || uri.getPath() == null || uri.getPath().isEmpty()) {
            IterableLogger.d("BitmapLoader", "Empty url for Thumbnail in inbox");
            return;
        }

        Future.runAsync(() -> fetchBitmap(imageView.getContext(), uri))
        .onSuccess(result -> {
            if (ViewCompat.isAttachedToWindow(imageView)) {
                imageView.setImageBitmap(result);
            }
        })
        .onFailure(throwable -> {
            IterableLogger.e("BitmapLoader", "Error while loading image: " + uri.toString(), throwable);
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
