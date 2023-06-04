package com.iterable.iterableapi

import androidx.annotation.RestrictTo
import java.util.Date

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class EmbeddedImpressionData(
    val messageId: String,
    var displayCount: Int = 0,
    var duration: Float = 0.0f,
    var start: Date? = null
) {
    constructor(
        messageId: String
    ) : this(messageId, 0, 0.0f, null)

    fun startImpression() {
        this.start = Date()
    }

    fun endImpression() {
        if(this.start != null) {
            this.displayCount = this.displayCount?.plus(1)
            this.duration =
                this.duration?.plus((Date().time - this.start!!.time) / 1000.0).toFloat()
            this.start = null
        }
    }
}