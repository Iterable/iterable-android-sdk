package com.iterable.iterableapi;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import androidx.work.ListenableWorker;
import androidx.work.testing.TestListenableWorkerBuilder;

import org.junit.Before;
import org.junit.Test;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class IterableNotificationImageRetryWorkerTest extends BaseTest {

    private static final String MESSAGE_ID = "abc123imagepush";
    private static final int NOTIFICATION_ID = Math.abs(MESSAGE_ID.hashCode());

    private NotificationManager notificationManager;

    @Before
    public void setUp() throws Exception {
        IterableTestUtils.resetIterableApi();
        IterableTestUtils.createIterableApiNew();
        notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    @Test
    public void testRetryRepostsShownNotificationWithImageWithoutAlerting() throws Exception {
        long originalWhen = System.currentTimeMillis() - 60_000;
        Bundle extras = imagePushExtras(getWorkingImageUrl());
        postTextOnlyNotification(extras, originalWhen);
        Notification before = getOnlyShownNotification();
        assertFalse(before.extras.containsKey(Notification.EXTRA_PICTURE));
        assertEquals(0, before.flags & Notification.FLAG_ONLY_ALERT_ONCE);

        ListenableWorker.Result result = runRetry(extras, originalWhen);

        assertEquals(ListenableWorker.Result.success(), result);
        StatusBarNotification[] shown = notificationManager.getActiveNotifications();
        assertEquals(1, shown.length);
        assertEquals(NOTIFICATION_ID, shown[0].getId());
        Notification after = shown[0].getNotification();
        assertTrue("Re-posted notification should have the image",
                after.extras.containsKey(Notification.EXTRA_PICTURE));
        assertTrue("Re-posted notification should not alert again",
                (after.flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0);
        assertEquals("Re-posted notification should keep the original time", originalWhen, after.when);
    }

    @Test
    public void testRetryDoesNothingWhenNotificationWasDismissed() throws Exception {
        long originalWhen = System.currentTimeMillis() - 60_000;
        Bundle extras = imagePushExtras(getWorkingImageUrl());
        postTextOnlyNotification(extras, originalWhen);
        notificationManager.cancel(NOTIFICATION_ID);

        ListenableWorker.Result result = runRetry(extras, originalWhen);

        assertEquals(ListenableWorker.Result.success(), result);
        assertEquals(0, notificationManager.getActiveNotifications().length);
    }

    @Test
    public void testRetryDoesNothingWhenNotificationIsDismissedDuringDownload() throws Exception {
        MockResponse imageResponse = IterableTestUtils.getImageResponse();
        MockWebServer imageServer = new MockWebServer();
        imageServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                notificationManager.cancel(NOTIFICATION_ID);
                return imageResponse;
            }
        });
        imageServer.start();
        long originalWhen = System.currentTimeMillis() - 60_000;
        Bundle extras = imagePushExtras(imageServer.url("/image.png").toString());
        postTextOnlyNotification(extras, originalWhen);

        ListenableWorker.Result result = runRetry(extras, originalWhen);

        assertEquals(ListenableWorker.Result.success(), result);
        assertEquals(1, imageServer.getRequestCount());
        assertEquals(0, notificationManager.getActiveNotifications().length);
        imageServer.shutdown();
    }

    @Test
    public void testRetryIgnoresNotificationsThatAreNotTheOriginalPush() throws Exception {
        long originalWhen = System.currentTimeMillis() - 60_000;
        Bundle extras = imagePushExtras(getWorkingImageUrl());
        notificationManager.notify(NOTIFICATION_ID + 1, buildTextOnlyNotification(extras, MESSAGE_ID, originalWhen));
        notificationManager.notify("tag", NOTIFICATION_ID, buildTextOnlyNotification(extras, MESSAGE_ID, originalWhen));
        notificationManager.notify(NOTIFICATION_ID, buildTextOnlyNotification(extras, "otherPush", originalWhen));

        ListenableWorker.Result result = runRetry(extras, originalWhen);

        assertEquals(ListenableWorker.Result.success(), result);
        StatusBarNotification[] shown = notificationManager.getActiveNotifications();
        assertEquals(3, shown.length);
        for (StatusBarNotification notification : shown) {
            assertFalse(notification.getNotification().extras.containsKey(Notification.EXTRA_PICTURE));
        }
    }

    @Test
    public void testRetryWaitsOutTheDnsFailureCacheOnlyRightAfterTheFirstAttempt() {
        long now = System.currentTimeMillis();
        assertEquals(3_000L, IterableNotificationImageRetryWorker.retryDelayMillis(now, now));
        assertEquals(1_000L, IterableNotificationImageRetryWorker.retryDelayMillis(now - 2_000, now));
        assertEquals(0L, IterableNotificationImageRetryWorker.retryDelayMillis(now - 60_000, now));
    }

    @Test
    public void testRetryDoesNothingWhenOlderThanMaxAge() throws Exception {
        long originalWhen = System.currentTimeMillis()
                - IterableNotificationImageRetryWorker.MAX_RETRY_AGE_MILLIS - 60_000;
        Bundle extras = imagePushExtras(getWorkingImageUrl());
        postTextOnlyNotification(extras, originalWhen);

        ListenableWorker.Result result = runRetry(extras, originalWhen);

        assertEquals(ListenableWorker.Result.success(), result);
        Notification shown = getOnlyShownNotification();
        assertFalse(shown.extras.containsKey(Notification.EXTRA_PICTURE));
        assertEquals(0, shown.flags & Notification.FLAG_ONLY_ALERT_ONCE);
    }

    @Test
    public void testRetryGivesUpQuietlyWhenDownloadFailsAgain() throws Exception {
        long originalWhen = System.currentTimeMillis() - 60_000;
        Bundle extras = imagePushExtras(IterableTestUtils.getUnreachableUrl());
        postTextOnlyNotification(extras, originalWhen);

        ListenableWorker.Result result = runRetry(extras, originalWhen);

        assertEquals(ListenableWorker.Result.success(), result);
        Notification shown = getOnlyShownNotification();
        assertFalse(shown.extras.containsKey(Notification.EXTRA_PICTURE));
        assertEquals(0, shown.flags & Notification.FLAG_ONLY_ALERT_ONCE);
    }

    private Bundle imagePushExtras(String imageUrl) throws Exception {
        Bundle extras = new Bundle();
        extras.putString(IterableConstants.ITERABLE_DATA_KEY, IterableTestUtils.getImagePushPayload(imageUrl));
        extras.putString(IterableConstants.ITERABLE_DATA_BODY, "Image push");
        return extras;
    }

    private String getWorkingImageUrl() {
        return getClass().getClassLoader().getResource("350x150.png").toString();
    }

    private Notification buildTextOnlyNotification(Bundle extras, String messageId, long when) {
        IterableNotificationBuilder builder = IterableNotificationHelper.createNotification(
                getContext(), IterableNotificationHelper.removePushImageFromBundle(extras));
        builder.setWhen(when);
        builder.getExtras().putString(IterableNotificationWorker.EXTRA_MESSAGE_ID, messageId);
        return builder.build();
    }

    private void postTextOnlyNotification(Bundle extras, long when) {
        notificationManager.notify(NOTIFICATION_ID, buildTextOnlyNotification(extras, MESSAGE_ID, when));
    }

    private Notification getOnlyShownNotification() {
        StatusBarNotification[] shown = notificationManager.getActiveNotifications();
        assertEquals(1, shown.length);
        return shown[0].getNotification();
    }

    private ListenableWorker.Result runRetry(Bundle extras, long originalPostTime) {
        IterableNotificationImageRetryWorker worker = TestListenableWorkerBuilder
                .from(getContext(), IterableNotificationImageRetryWorker.class)
                .setInputData(IterableNotificationImageRetryWorker.createInputData(
                        extras, NOTIFICATION_ID, originalPostTime))
                .build();
        return worker.doWork();
    }
}
