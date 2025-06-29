package com.iterable.iterableapi.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import androidx.core.view.ViewCompat
import android.widget.ImageView

import com.iterable.iterableapi.IterableLogger
import com.iterable.iterableapi.util.Future

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object BitmapLoader {

    private const val DEFAULT_TIMEOUT_MS = 3000

    fun loadBitmap(imageView: ImageView, uri: Uri?) {
        if (uri == null || uri.path == null || uri.path!!.isEmpty()) {
            IterableLogger.d("BitmapLoader", "Empty url for Thumbnail in inbox")
            return
        }

        Future.runAsync(Callable {
            fetchBitmap(imageView.context, uri)
        })
        .onSuccess { result ->
            if (ViewCompat.isAttachedToWindow(imageView)) {
                imageView.setImageBitmap(result)
            }
        }
        .onFailure { throwable ->
            IterableLogger.e("BitmapLoader", "Error while loading image: " + uri.toString(), throwable)
        }
    }

    @Throws(IOException::class)
    internal fun fetchBitmap(context: Context, uri: Uri): Bitmap {
        val imageFile = File.createTempFile("itbl_", ".temp", context.cacheDir)
        if (!downloadFile(uri, imageFile)) {
            throw RuntimeException("Failed to download image file")
        }
        return BitmapFactory.decodeFile(imageFile.absolutePath)
    }

    @Throws(IOException::class)
    internal fun downloadFile(uri: Uri, file: File): Boolean {
        val url = URL(uri.toString())

        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        var urlConnection: HttpURLConnection? = null

        try {
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connectTimeout = DEFAULT_TIMEOUT_MS
            urlConnection.useCaches = true
            inputStream = urlConnection.inputStream

            val responseCode = urlConnection.responseCode
            if (responseCode != 200) {
                return false
            }

            if (inputStream != null) {
                outputStream = FileOutputStream(file)
                val buffer = ByteArray(2048)

                var readLength: Int
                while (inputStream.read(buffer).also { readLength = it } != -1) {
                    outputStream.write(buffer, 0, readLength)
                }

                return true
            }

            return false
        } finally {
            inputStream?.close()
            outputStream?.close()
            urlConnection?.disconnect()
        }
    }
}
