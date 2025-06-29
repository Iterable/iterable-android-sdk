package com.iterable.iterableapi

import androidx.annotation.RestrictTo

import java.util.Date

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ImpressionData(
    val messageId: String,
    val silentInbox: Boolean
) {
    var displayCount = 0
    var duration = 0.0f

    internal var impressionStarted: Date? = null

    fun startImpression() {
        this.impressionStarted = Date()
    }

    fun endImpression() {
        //increment count and add to duration if impression has been started
        impressionStarted?.let { started ->
            this.displayCount += 1
            this.duration += (Date().time - started.time).toFloat() / 1000
            this.impressionStarted = null
        }
    }
}
