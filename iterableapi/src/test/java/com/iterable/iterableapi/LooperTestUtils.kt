package com.iterable.iterableapi

import android.os.Handler
import android.os.HandlerThread
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.atomic.AtomicReference

object LooperTestUtils {

    /**
     * Runs [action] to completion on a background looper.
     * Work that [action] posts to the main looper stays queued.
     */
    @JvmStatic
    @Throws(InterruptedException::class)
    fun runOnBackgroundLooperAndWait(action: Runnable) {
        val thread = HandlerThread("test-background")
        val failure = AtomicReference<Throwable>()
        thread.start()
        try {
            shadowOf(thread.looper).pause()
            Handler(thread.looper).post {
                try {
                    action.run()
                } catch (error: Throwable) {
                    failure.set(error)
                }
            }
            shadowOf(thread.looper).idle()
        } finally {
            thread.quit()
            thread.join()
        }
        failure.get()?.let { throw AssertionError("Background action failed", it) }
    }
}
