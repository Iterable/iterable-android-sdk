package com.iterable.iterableapi;

import androidx.annotation.RestrictTo;

import java.util.Date;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ImpressionData {
    final String messageId;
    final boolean silentInbox;
    int displayCount = 0;
    float duration = 0.0f;

    Date impressionStarted = null;

    ImpressionData(String messageId, boolean silentInbox) {
        this.messageId = messageId;
        this.silentInbox = silentInbox;
    }

    void startImpression() {
        this.impressionStarted = new Date();
    }

    void endImpression() {
        if (this.impressionStarted != null) {
            this.displayCount += 1;
            this.duration += (float) (new Date().getTime() - this.impressionStarted.getTime()) / 1000;
            this.impressionStarted = null;
        }
    }
}
