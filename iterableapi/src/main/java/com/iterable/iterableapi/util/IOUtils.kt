package com.iterable.iterableapi.util

import androidx.annotation.Nullable

import java.io.Closeable
import java.io.IOException

object IOUtils {
    fun closeQuietly(closeable: Closeable?) {
        closeable?.let {
            try {
                it.close()
            } catch (ignored: IOException) {
            }
        }
    }
}
