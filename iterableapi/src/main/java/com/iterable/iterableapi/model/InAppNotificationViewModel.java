package com.iterable.iterableapi.model;

import android.arch.lifecycle.ViewModel;

import com.iterable.iterableapi.IterableHelper;
import com.iterable.iterableapi.IterableInAppLocation;

public class InAppNotificationViewModel extends ViewModel {
    public String htmlString;
    public String messageID;
    public IterableHelper.IterableUrlCallback clickCallback;
    public IterableInAppLocation location;
}
