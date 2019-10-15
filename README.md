[![Build Status](https://travis-ci.org/Iterable/iterable-android-sdk.svg?branch=master)](https://travis-ci.org/Iterable/iterable-android-sdk)
[![codecov](https://codecov.io/gh/Iterable/iterable-android-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/Iterable/iterable-android-sdk)
[![Download](https://api.bintray.com/packages/davidtruong/maven/Iterable-SDK/images/download.svg)](https://bintray.com/davidtruong/maven/Iterable-SDK/_latestVersion)

# Iterable Android SDK

The Iterable Android SDK is a Java implementation of an Android client for Iterable, supporting Android API versions 15 and higher.

## Setting up a push integration in Iterable

Before you even start with the SDK, you will need to: 

1. Set your application up to receive push notifications
2. Set up a push integration in Iterable. This allows Iterable to communicate on your behalf with Firebase Cloud Messaging.

For more details, read Iterable's [Setting up Android Push Notifications](https://support.iterable.com/hc/articles/115000331943-Setting-up-Android-Push-Notifications) guide.

Congratulations, you've configured your Iterable project to send push notifications to your app! Now, let's set up the Iterable SDK.

## Installing the SDK

Add the following dependencies to your application's **build.gradle**:

```groovy
compile 'com.iterable:iterableapi:3.2.0-beta1'
compile 'com.google.firebase:firebase-messaging:X.X.X' // Min version 17.4.0
```

See [Bintray](https://bintray.com/davidtruong/maven/Iterable-SDK) for the latest version of the Iterable Android SDK.

Beta versions of this SDK are subject to Iterable's
[Beta Mobile SDK Terms of Service](https://support.iterable.com/hc/en-us/articles/360034753412).

### Handling Firebase push messages and tokens

The SDK adds a `FirebaseMessagingService` to the app manifest automatically, so you don't have to do any extra setup to handle incoming push messages.

If your application implements its own FirebaseMessagingService, make sure you forward `onMessageReceived` and `onNewToken` calls to `IterableFirebaseMessagingService.handleMessageReceived` and `IterableFirebaseMessagingService.handleTokenRefresh`, respectively:

```java
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        IterableFirebaseMessagingService.handleMessageReceived(this, remoteMessage);
    }

    @Override
    public void onNewToken(String s) {
        IterableFirebaseMessagingService.handleTokenRefresh();
    }
}
```

Note that `FirebaseInstanceIdService` is deprecated and replaced with `onNewToken` in recent versions of Firebase.

## Migrating from a version prior to 3.1.0

- In-app messages: `spawnInAppNotification`

    - `spawnInAppNotification` is no longer needed and will fail to compile.
    The SDK now displays in-app messages automatically. For more information,
    see [In-app messages](#in-app-messages).

    - There is no need to poll the server for new messages.

- In-app messages: handling manually

    - To control when in-app messages display (rather than displaying them
    automatically), set `IterableConfig.inAppHandler` (an 
    `IterableInAppHandler` object). From its `onNewInApp` method, return 
    `InAppResponse.SKIP`.

    - To get the queue of available in-app messages, call 
    `IterableApi.getInstance().getInAppManager().getMessages()`. Then, call 
    `IterableApi.getInstance().getInAppManager().showMessage(message)`
    to show a specific message.

    - For more details, see [In-app messages](#in-app-messages).

- In-app messages: custom actions

    - This version of the SDK reserves the `iterable://` URL scheme for
    Iterable-defined actions handled by the SDK and the `action://` URL
    scheme for custom actions handled by the mobile application's custom
    action handler. For more details, see 
    [Handling in-app message buttons and links](#handling-in-app-message-buttons-and-links).

    - If you are currently using the `itbl://` URL scheme for custom actions,
    the SDK will still pass these actions to the custom action handler.
    However, support for this URL scheme will eventually be removed (timeline
    TBD), so it is best to move templates to the `action://` URL scheme as
    it's possible to do so.

- Consolidated deep link URL handling

    - By default, the SDK handles deep links with the the URL handler
    assigned to `IterableConfig`. Follow the instructions in 
    [Deep linking](#deep-linking) to migrate any existing URL handling code
    to this new API.

## Using the SDK

### Push notifications

1. In the `onCreate` method of the `Application`, initialize the Iterable SDK:

    ```java
    IterableConfig config = new IterableConfig.Builder()
            .setPushIntegrationName("myPushIntegration")
            .build();
    IterableApi.initialize(context, "<your-api-key>", config);
    ```

    * The `apiKey` should correspond to the API key of your project in Iterable. If you'd like, you can specify a different `apiKey` depending on whether you're building in `DEBUG` or `PRODUCTION`, and point the SDK to the relevant Iterable project.

    > &#x26A0; Don't call `IterableApi.initialize` from `Activity#onCreate`; it is necessary for Iterable SDK to be initialized when the application is starting, to make sure everything is set up regardless of whether the app is launched to open an activity or is woken up in background as a result of an incoming push message.

2. Once you know the email *(Preferred)* or userId of the user, call `setEmail` or `setUserId`

    * EMAIL: `IterableApi.getInstance().setEmail("email@example.com");`
    * USERID: `IterableApi.getInstance().setUserId("userId");`

    > &#x26A0; Don't specify both email and userId in the same session, as they will be treated as different users by the SDK. Only use one type of identifier, email or userId, to identify the user.

3. Register for remote notifications
    
    Iterable SDK automatically registers the push token with Iterable whenever `setEmail` or `setUserId` is called.
    > &#x26A0; This default behavior is the preferred way of handling token registrations.
    
    If you want to trigger token registration manually, first disable automatic registration by calling `setAutoPushRegistration(false)` on `IterableConfig.Builder` when initializing the SDK.
    
    Than call `registerForPush` whenever you want to register the token:

    ```
    IterableApi.getInstance().registerForPush();
    ```

    This will take care of retrieving the token and registering it with Iterable.

    > &#x26A0; Device registration will fail if user email or userId is not set. If you're calling `setEmail` or `setUserId` after the app is launched (i.e. when the user logs in), make sure you call `registerForPush()` again to register the device with the logged in user.
      
Congratulations! You can now send remote push notifications to your device from Iterable!

#### Disabling push notifications to a device

When a user logs out, you typically want to disable push notifications to that user/device. This can be accomplished by calling `disablePush()`. Please note that it will only attempt to disable the device if you have previously called `registerForPush()`.

In order to re-enable push notifcations to that device, simply call `registerForPush()` as usual when the user logs back in.

#### Customizing push notifications

##### Notification icon

Notifications are rendered with the app launcher icon by default. To specify a custom icon for notifications, add this line to `AndroidManifest.xml`:
```xml
<meta-data android:name="iterable_notification_icon" android:resource="@drawable/ic_notification_icon"/>
```
where `ic_notification_icon` is the name of the notification icon.

##### Notification color

Add this line to `AndroidManifest.xml` to specify the notification color:
```xml
<meta-data android:name="iterable_notification_color" android:value="#FFFFFF"/>
```
where `#FFFFFF` can be replaced with a hex representation of a color of your choice. In stock Android, the notification icon and action buttons will be tinted with this color.

You can also use a color resource:
```xml
<meta-data android:name="iterable_notification_color" android:resource="@color/notification_color"/>
```

##### Notification channel name

Since Android 8.0, Android requires apps to specify a channel for every notification. Iterable uses one channel for all notification; to customize the name of this channel, add this to `AndroidManifest.xml`:

```xml
<meta-data android:name="iterable_notification_channel_name" android:value="Notifications"/>
```

You can also use a string resource to localize the channel name:

```xml
<meta-data android:name="iterable_notification_channel_name" android:resource="@string/notification_channel_name"/>
```

### In-app messages

#### Default behavior

By default, when an in-app message arrives from the server, the SDK automatically shows it if the app is in the foreground. If an in-app message is already showing when the new message arrives, the new in-app message will be shown 30 seconds after the currently displayed in-app message closes ([see how to change this default value below](#Changing-the-display-interval-between-in-app-messages)). Once an in-app message is shown, it will be "consumed" from the server queue and removed from the local queue as well. There is no need to write any code to get this default behavior.

#### Overriding whether to show or skip a particular in-app message

An incoming in-app message triggers a call to the `onNewInApp` method of `IterableConfig.inAppHandler` (an object of type `IterableInAppHandler`). To override the default behavior, set `inAppHandler` in `IterableConfig` to a custom class that overrides the `onNewInApp` method. `onNewInApp` should return `InAppResponse.SHOW` to show the incoming in-app message or `InAppResponse.SKIP` to skip showing it.

```java
class MyInAppHandler implements IterableInAppHandler {
    @Override
    public InAppResponse onNewInApp(IterableInAppMessage message) {
        if (/* add conditions here */) {
            return InAppResponse.SHOW;
        } else {
            return InAppResponse.SKIP;
        }
    }
}

// ...

IterableConfig config = new IterableConfig.Builder()
                .setPushIntegrationName("myPushIntegration")
                .setInAppHandler(new MyInAppHandler())
                .build();
IterableApi.initialize(context, "<your-api-key>", config);
```

#### Getting the local queue of in-app messages

The SDK keeps the local in-app message queue in sync by checking the server queue every time the app goes into foreground, and via silent push messages that arrive from Iterable servers to notify the app whenever a new in-app message is added to the queue.

To access the in-app message queue, call `IterableApi.getInstance().getInAppManager().getMessages()`. To show a message, call `IterableApi.getInstance().getInAppManager().showMessage(message)`.

```java
// Get the in-app messages list
IterableInAppManager inAppManager = IterableApi.getInstance().getInAppManager();
List<IterableInAppMessage> messages = inAppManager.getMessages();

// Show an in-app message 
inAppManager.showMessage(message);

// Show an in-app message without consuming (not removing it from the queue)
inAppManager.showMessage(message, false, null);

```

#### Handling in-app message buttons and links

The SDK handles in-app message buttons and links as follows:

- If the URL of the button or link uses the `action://` URL scheme, the SDK
passes the action to `IterableConfig.customActionHandler.handleIterableCustomAction()`. 
If `customActionHandler` (an `IterableCustomActionHandler` object) has not 
been set, the action will not be handled.

    - For the time being, the SDK will treat `itbl://` URLs the same way as
    `action://` URLs. However, this behavior will eventually be deprecated
    (timeline TBD), so it's best to migrate to the `action://` URL scheme
    as it's possible to do so.

- The `iterable://` URL scheme is reserved for action names predefined by
the SDK. If the URL of the button or link uses an `iterable://` URL known
to the SDK, it will be handled automatically and will not be passed to the
custom action handler.

    - The SDK does not yet recognize any `iterable://` actions, but may
    do so in the future.

- The SDK passes all other URLs to `IterableConfig.urlHandler.handleIterableURL()`. 
If `urlHandler` (an `IterableUrlHandler` object) has not been set, or if it
returns `false` for the provided URL, the URL will be opened by the system
(using a web browser or other application, as applicable).

#### Changing the display interval between in-app messages

To customize the time delay between successive in-app messages, set `inAppDisplayInterval` on `IterableConfig` to an appropriate value in seconds. The default value is 30 seconds.

### Mobile Inbox

Apps using version 3.2.0 and later of this SDK can save in-app messages to an inbox. This inbox displays a list of saved in-app messages and allows users to read and interact with them at their convenience. The SDK provides a default user interface for the inbox, and it can be customized to match your brand's styles.

To configure an in-app message to use the inbox, marketers should:

- Enable the **Show in Inbox** toggle in the in-app message's template
- Choose whether or not to **Deliver Silently to Inbox** (so that the message does not display prominently when the device receives it, but can be viewed later by the user)

#### Mobile Inbox implementation guide

Add a new dependency, `iterableapi-ui`, to your `build.gradle` file:

```groovy
compile 'com.iterable:iterableapi-ui:3.2.0-beta1'
```

To open Inbox as a separate activity, launch `IterableInboxActivity`. The SDK also provides `IterableInboxFragment` if you need to embed the Inbox UI within an existing activity.

##### Show the inbox inside an existing activity

You can embed `IterableInboxFragment` either via a layout .xml file, or by calling `IterableInboxFragment.newInstance()` and inserting it into your activity.

##### Display mobile inbox as a result of an user action

It's also possible to show the inbox when a user taps a button (or completes some other action). To do this, respond to the user's action by starting `IterableInboxActivity`. For example:

```java
// Add this in your button handler
startActivity(this, IterableInboxActivity.class);
```

#### Mobile Inbox customization options

The SDK provides various ways to customize the inbox's interface to match your brand's styles. For simpler customization needs, specify a custom XML layout ID by calling `setItemLayoutId` on the `IterableInboxFragment`. For more advanced scenarios, define classes that inherit from `IterableInboxFragment` and `InboxRecyclerViewAdapter` and override existing methods as necessary.

##### inboxMode

`IterableInboxFragment` and `IterableInboxActivity` can display inbox messages in two ways:

- `POPUP` - (default): Displays the inbox message as a dialog on top of the Inbox UI.
- `ACTIVITY` - Displays the inbox message in a new activity.

##### itemLayoutId

To completely change the look of an inbox, copy the contents of `fragment_inbox_item.xml` from `iterableapi-ui` into a new layout XML in your project and modify it. Then call `setItemLayoutId` on the `IterableInboxFragment` in `onCreate` of the parent activity to specify the custom layout XML for Inbox cells.

#### Mobile inbox events and the events lifecycle

An inbox lists saved in-app messages. Because of this, Iterable events that apply to in-app messages also apply to inbox messages. These events are described below:

##### In-App Send

This event is triggered when an in-app message is sent to a user.

Important event properties:

- `eventType` - `inAppSend`
- `createdAt` - when the in-app message was sent
- `campaignId`- the in-app message campaign ID

##### In-App Delivery

This event is triggered when an in-app message is received by a user's device.

Important event properties:

- `eventType` - `inAppDelivery`
- `createdAt` - when the in-app message was received by the device
- `campaignId` - the in-app message campaign ID

##### Inbox Message Impression

This event tracks the number of unique times a message appeared in the inbox during a single inbox session (period when the user had the inbox open). The total represents the count of unique times the message displayed on-screen in the inbox during an inbox session (not the number of times the user actually opened that message).

Important event properties:

- `eventType` - `inboxMessageImpression`
- `createdAt` - when the event was created
- `campaignId` - the in-app message campaign ID
- `impressionCount` - how many times the message was visible in the inbox during the inbox session
- `totalDuration` - the total duration of time (in seconds) the message was visible in the inbox

##### In-App Open

This event is triggered when the user taps a message in the inbox to view its contents.

Important event properties:

- `eventType` - `inAppOpen`
- `createdAt` - when the in-app message was opened
- `campaignId`- the in-app message campaign ID

##### In-App Click

This event is triggered when the user taps on a link or button in an in-app message.

Important event properties:

- `eventType` - `inAppClick`
- `createdAt` - when the button in the in-app message was tapped
- `campaignId`- the in-app message campaign ID
- `clickedUrl` - the URL associated with the tapped link/button

##### In-App Close

This event is triggered when the user closes an inbox message by tapping a close button or a back button. Close buttons/links should have URL `iterable://dismiss`. Tapping a close button/link also creates an `inAppClick` event.

Important event properties:

- `eventType` - `inAppClose`
- `createdAt` - when the in-app message was closed
- `campaignId`- the in-app message campaign ID
- `closeAction` - the type of item the user tapped: `link` (for buttons/links), `back` for a back button, or `other`

##### In-App Delete 

This event is triggered when the user deletes an inbox message by swiping left on it and tapping the **Delete** button, or when the user taps a delete button/link in the in-app message, which should have a URL of `iterable://delete`. Tapping a delete button/link also creates an `inAppClick` event and an `inAppClose` event.

Important event properties:

- `eventType` - `inAppDelete`
- `createdAt` - when the in-app message was deleted
- `campaignId`- the in-app campaign ID
- `deleteAction` - how the message was deleted (`inbox-swipe` if the user swiped left in the inbox and tapped **Delete**, or `delete-button` if the user tapped a button/link with URL `iterable://delete`).

##### Inbox Session

This event captures information about an inbox session, which starts when a user opens the inbox and ends when they navigate away (or close/minimize the app). Viewing a message in the inbox does not end the session.

Important event properties

- `eventType` - `inboxSession`
- `campaignId`- the in-app campaign id
- `inboxSessionStart` - when the session started
- `inboxSessionEnd` - when the session ended
- `uniqueImpressionCount` - no of unique messages visible in the Inbox message list to the user during the inbox session
- `startTotalMessageCount` - the number of messages in the inbox at the session's start
- `endTotalMessageCount` - the number of messages in the inbox at the session's end (messages can be added or removed during a session)

### Deep linking

#### Handling links from push notifications

Push notifications and action buttons may have `openUrl` actions attached to them. When a URL is specified, the SDK first calls the `urlHandler` specified in your `IterableConfig` object. You can use this class to handle `openUrl` actions the same way as you handle normal deep links. If the handler is not set or returns NO, the SDK will open a browser with that URL.

```java
// MyApplication.java

@Override
public void onCreate() {
    super.onCreate();
    ...
    IterableConfig config = new IterableConfig.Builder()
        .setPushIntegrationName("myPushIntegration")
        .setUrlHandler(this)
        .build();
    IterableApi.initialize(context, "YOUR API KEY", config);
}

@Override
public boolean handleIterableURL(Uri uri, IterableActionContext actionContext) {
    // Assuming you have a DeeplinkHandler class that handles all deep link URLs and navigates to the right place in the app
    return DeeplinkHandler.handle(this, uri);
}
```

#### Handling email links

For App Links to work with link rewriting in emails, you need to set up an **assetlinks.json** file in the Iterable project. For more information, read [Setting up Android App Links](https://support.iterable.com/hc/articles/115001021063-Setting-up-Android-App-Links).

If you already have a `urlHandler` (see [Handling links from push notifications](#handling-links-from-push-notifications)), the same handler can be used for email deep links by calling `handleAppLink` in the activity that handles all app links in your app:

```java
// MainActivity.java
@Override
public void onCreate() {
    super.onCreate();
    ...
    handleIntent(getIntent());
}

@Override
public void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (intent != null) {
        handleIntent(intent);
    }
}

private void handleIntent(Intent intent) {
    if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
        IterableApi.handleAppLink(intent.getDataString());
        // Overwrite the intent to make sure we don't open the deep link
        // again when the user opens our app later from the task manager
        setIntent(new Intent(Intent.ACTION_MAIN));
    }
}
```

Alternatively, call `getAndTrackDeeplink` along with a callback to handle the original deep link URL. You can use this method for any incoming URLs, as it will execute the callback without changing the URL for non-Iterable URLs.

```java
IterableApi.getAndTrackDeeplink(uri, new IterableHelper.IterableActionHandler() {
    @Override
    public void execute(String result) {
        Log.d("HandleDeeplink", "Redirected to: "+ result);
        // Handle the original deep link URL here
    }
});
```

#### Deferred deep linking

[Deferred deep linking](https://en.wikipedia.org/wiki/Deferred_deep_linking) allows a user who does not have a specific app installed to:

- Click on a deep link that would normally open content in that app.
- Install the app from the App Store.
- Open the app and immediately see the content referenced by the link.
 
As the name implies, the deep link is _deferred_ until the app has been installed. 

After tapping a deep link in an email from an Iterable campaign, users without the associated app will be directed to Play Store to install it. If the app uses the Iterable SDK and has deferred deep linking enabled, the content associated with the deep link will load on first launch.

##### Enabling deferred deep linking

Set `checkForDeferredDeeplink` to `true` on `IterableConfig` when initializing the SDK to enable deferred deep linking for Iterable SDK. Make sure a `urlHandler` is also set up to handle deep links to open the right content within your app on first launch (see above for details).

## Optional setup

### GCM -> Firebase migration

The recommended migration path is to upgrade the existing Google Cloud project to Firebase, update the server token in the existing GCM push integration in Iterable with the new Firebase token, and update the Android app to support Firebase. If you keep using the same project for FCM and the same integration name, the old tokens will still be valid and won't require re-registration of existing devices.

If you're using a different project for FCM and have existing devices on a GCM project with a different sender ID, updating the app will generate new tokens for users, but the old tokens will still be valid. When migrating from one sender ID to another, specify `legacyGCMSenderId` in `IterableConfig` when initializing the SDK. This will disable old tokens to make sure users won't receive duplicate notifications.

## Additional information

For more information, take a look at:

- Iterable's [Android SDK Release Notes](https://support.iterable.com/hc/articles/360027543332)
- Iterable's [Setting up Android Push Notifications](https://support.iterable.com/hc/articles/115000331943-Setting-up-Android-Push-Notifications) guide
- Iterable's [Push Notification Setup FAQs](http://support.iterable.com/hc/articles/206791196-Push-Notification-Setup-FAQ-s)

## License

The MIT License

See [LICENSE](https://github.com/Iterable/iterable-android-sdk/blob/master/LICENSE)

## Want to contribute?

This library is open source, and we will look at pull requests!

See [CONTRIBUTING](CONTRIBUTING.md) for more information.
