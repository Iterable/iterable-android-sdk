package com.iterable.iterableapi.util;

import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;

public final class IOUtils {
    private IOUtils() {
    }

    public static void closeQuietly(@Nullable Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }
}
