package com.iterable.iterableapi;

import android.net.Uri;

public interface IterableUrlHandler {

    boolean handleIterableURL(Uri uri, IterableAction action);

}
