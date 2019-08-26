package com.iterable.iterableapi;

import android.support.annotation.RestrictTo;

import java.util.Date;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IterableInboxSession {
    public final Date sessionStartTime;
    public final Date sessionEndTime;
    public final int startTotalMessageCount;
    public final int startUnreadMessageCount;
    public final int endTotalMessageCount;
    public final int endUnreadMessageCount;

    public IterableInboxSession(Date sessionStartTime, Date sessionEndTime, int startTotalMessageCount, int startUnreadMessageCount, int endTotalMessageCount, int endUnreadMessageCount) {
        this.sessionStartTime = sessionStartTime;
        this.sessionEndTime = sessionEndTime;
        this.startTotalMessageCount = startTotalMessageCount;
        this.startUnreadMessageCount = startUnreadMessageCount;
        this.endTotalMessageCount = endTotalMessageCount;
        this.endUnreadMessageCount = endUnreadMessageCount;
    }

    public IterableInboxSession() {
        this.sessionStartTime = null;
        this.sessionEndTime = null;
        this.startTotalMessageCount = 0;
        this.startUnreadMessageCount = 0;
        this.endTotalMessageCount = 0;
        this.endUnreadMessageCount = 0;
    }
}
