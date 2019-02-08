[![Build Status](https://travis-ci.org/Iterable/iterable-android-sdk.svg?branch=master)](https://travis-ci.org/Iterable/iterable-android-sdk)
[![codecov](https://codecov.io/gh/Iterable/iterable-android-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/Iterable/iterable-android-sdk)
[![Download](https://api.bintray.com/packages/davidtruong/maven/Iterable-SDK/images/download.svg)](https://bintray.com/davidtruong/maven/Iterable-SDK/_latestVersion)

# Iterable Android SDK

The `iterable-android-sdk` is a Java implementation of an Android client for Iterable, supporting Android API versions 15 and higher.

# Setting up a push integration in Iterable

Before you even start with the SDK, you will need to: 

1. Set your application up to receive push notifications, and 
2. Set up a push integration in Iterable. This allows Iterable to communicate on your behalf with Firebase Cloud Messaging.

For information on setting up your Firebase Project, see

* [Add Firebase to Your Android Project](https://firebase.google.com/docs/android/setup)

To setup your push integration with Iterable in the web dashboard go to `Integrations -> Mobile Push`. When creating an integration, you will need to pick a name and a platform. The name is entirely up to you; it will be the `pushIntegrationName` in `IterableConfig` when you initialize our SDK. 

The platform will be `GCM` (This also includes FCM since it runs off of the same infrastructure). Add the Firebase Cloud Messaging Server Key obtained from the Firebase console.

![Creating an integration in Iterable](https://support.iterable.com/hc/en-us/article_attachments/211841066/2016-12-08_1442.png)

Congratulations, you've configured your mobile application to receive push notifications! Now, let's set up the Iterable SDK...

# Installing the SDK

Add the following dependencies to your application's `build.gradle`:

```groovy
compile 'com.iterable:iterableapi:3.0.7'
compile 'com.google.firebase:firebase-messaging:X.X.X' // Min version 9.0.0
```

See [Bintray](https://bintray.com/davidtruong/maven/Iterable-SDK) for the latest version of the Iterable Android SDK.

#### Handling Firebase push messages and tokens

The SDK adds a FirebaseMessagingService and FirebaseInstanceIdService to the app manifest automatically, so you don't have to do any extra setup to handle incoming push messages.
If your application implements its own FirebaseMessagingService, make sure you forward `onMessageReceived` and `onNewToken` calls to `IterableFirebaseMessagingService.handleMessageReceived` and `IterableFirebaseInstanceIDService.handleTokenRefresh`, respectively:

```java
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        IterableFirebaseMessagingService.handleMessageReceived(this, remoteMessage);
    }

    @Override
    public void onNewToken(String s) {
        IterableFirebaseInstanceIDService.handleTokenRefresh();
    }
}
```

Note that `FirebaseInstanceIdService` is deprecated and replaced with `onNewToken` in recent versions of Firebase.

# Using the SDK

1. In `Application`'s `onCreate`, initialize the Iterable SDK:

	```java
	IterableConfig config = new IterableConfig.Builder()
			.setPushIntegrationName("myPushIntegration")
			.build();
	IterableApi.initialize(context, "<your-api-key>", config);
	```

  * The `apiKey` should correspond to the API key of your project in Iterable. If you'd like, you can specify a different `apiKey` depending on whether you're building in `DEBUG` or `PRODUCTION`, and point the SDK to the relevant Iterable project.
  * It is possible to call this elsewhere but we strongly encourage initializing the SDK in `Application`'s `onCreate`. This will let the SDK automatically track a push open for you if the application was launched from a remote Iterable push notification.

2. Once you know the email *(Preferred)* or userId of the user, call `setEmail` or `setUserId`
  * EMAIL: `IterableApi.getInstance().setEmail("email@example.com");`
  * USERID: `IterableApi.getInstance().setUserId("userId");`
      * If you are setting a userId, an existing user must already exist for that userId
      * It is preferred that you use Email since that doesn't require an additional lookup by userId call on the backend.

3. **Register for remote notifications**  
    On application launch (or whenever you want to register the token), call `registerForPush`:

    ```
    IterableApi.getInstance().registerForPush();
    ```

    This will take care of retrieving the token and registering it with Iterable.
   > &#x26A0; Device registration will fail if user email or userId is not set. If you're calling `setEmail` or `setUserId` after the app is launched (i.e. when the user logs in), make sure you call `registerForPush()` again to register the device with the logged in user.
      
Congratulations! You can now send remote push notifications to your device from Iterable!


### Disabling push notifications to a device

When a user logs out, you typically want to disable push notifications to that user/device. This can be accomplished by calling `disablePush()`. Please note that it will only attempt to disable the device if you have previously called `registerForPush()`.

In order to re-enable push notifcations to that device, simply call `registerForPush()` as usual when the user logs back in.

## In-app messages

If you are already using in-app messages with IterableSDK, please check out the [migration section](#migrating-in-app-messages-from-the-previous-version-of-the-sdk).

### Default behavior

By default, when an in-app message arrives from the server, the SDK automatically shows it if the app is in the foreground. If an in-app message is already showing when the new message arrives, the new in-app message will be shown 30 seconds after the currently displayed in-app message closes ([see how to change this default value below](#Changing-the-display-interval-between-in-app-messages)). Once an in-app message is shown, it will be "consumed" from the server queue and removed from the local queue as well. There is no need to write any code to get this default behavior.

### Overriding whether to show or skip a particular in-app message

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

### Getting the local queue of in-app messages
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

### Handling user clicks in in-app messages

Button/link clicks from in-app messages are handled similarly to deep links from push notifications and emails. Please refer to the [Deep Linking](#Deep-Linking) section. Normally, the URL of the link (`href` property) will be passed to your app's `IterableUrlHandler`, if one is set. If this handler is not set or it returns `false` for that URL, tapping a link will open a browser with the URL of the link.
	
Custom actions can be specified by using `itbl` scheme in the URL: `itbl://customActionName`. If the URL of the link starts with `itbl://`, tapping the link will call your app's `IterableCustomActionHandler`. If `customActionHandler` is not set, nothing will happen by default.
	
### Changing the display interval between in-app messages

To customize the time delay between successive in-app messages, set `inAppDisplayInterval` on `IterableConfig` to an appropriate value in seconds. The default value is 30 seconds.

### Migrating in-app messages from the previous version of the SDK

If you are already using in-app messages, you will have to make the following changes to your code:

1. `spawnInAppNotification` is no longer needed and will fail to compile. In-app messages are now displayed automatically. If you need to customize the process, please refer to the sections above. To handle clicks on links in in-app messages, define `urlHandler` on `IterableConfig`.
2. If you want to handle all in-app messages manually, as before these changes, define an `inAppHandler` on `IterableConfig` that always returns `InAppResponse.SKIP` to never show in-app messages automatically, and use `getInAppManager().getMessages()` to get the messages and `getInAppManager().showMessage(message)` to show a specific message.


## Deep Linking
### Handling links from push notifications
Push notifications and action buttons may have `openUrl` actions attached to them. When a URL is specified, the SDK first calls `urlHandler` specified in your `IterableConfig` object. You can use this class to handle `openUrl` actions the same way as you handle normal deep links. If the handler is not set or returns NO, the SDK will open a browser with that URL.

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

### Handling email links
For App Links to work with link rewriting in emails, you need to set up assetlinks.json file in the Iterable project. More instructions here: [Setting up Android App Links](https://support.iterable.com/hc/en-us/articles/115001021063-Setting-up-Android-App-Links)

If you already have a `urlHandler` (see *Handling links from push notifications* section above), the same handler can be used for email deep links by calling `handleAppLink` in the activity that handles all app links in your app:

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

Alternatively, call `getAndTrackDeeplink` along with a callback to handle the original deeplink url. You can use this method for any incoming URLs, as it will execute the callback without changing the URL for non-Iterable URLs.

```java
IterableApi.getAndTrackDeeplink(uri, new IterableHelper.IterableActionHandler() {
	@Override
	public void execute(String result) {
		Log.d("HandleDeeplink", "Redirected to: "+ result);
		// Handle the original deep link URL here
	}
});
```

### Deferred deep linking
	
[Deferred deep linking](https://en.wikipedia.org/wiki/Deferred_deep_linking) allows a user who does not have a specific app installed to:
	
 - Click on a deep link that would normally open content in that app.
 - Install the app from the App Store.
 - Open the app and immediately see the content referenced by the link.
 
As the name implies, the deep link is _deferred_ until the app has been installed. 
	
After tapping a deep link in an email from an Iterable campaign, users without the associated app will be directed to Play Store to install it. If the app uses the Iterable SDK and has deferred deep linking enabled, the content associated with the deep link will load on first launch.
	
#### Enabling deferred deep linking
	
Set `checkForDeferredDeeplink` to `true` on `IterableConfig` when initializing the SDK to enable deferred deep linking for IterableSDK. Make sure a `urlHandler` is also set up to handle deep links to open the right content within your app on first launch (see above for details).

## Additional Information

See our [setup guide](https://support.iterable.com/hc/en-us/articles/115000331943-Setting-up-Android-Push-Notifications) for more information.

Also see our [push notification setup FAQs](http://support.iterable.com/hc/en-us/articles/206791196-Push-Notification-Setup-FAQ-s).

## Optional Setup

### GCM -> Firebase migration
The recommended migration path is to upgrade the existing Google Cloud project to Firebase, update the server token in the existing GCM push integration in Iterable with the new Firebase token, and update the Android app to support Firebase. If you keep using the same project for FCM and the same integration name, the old tokens will still be valid and won't require re-registration of existing devices.

If you're using a different project for FCM and have existing devices on a GCM project with a different sender ID, updating the app will generate new tokens for users, but the old tokens will still be valid. When migrating from one sender ID to another, specify `legacyGCMSenderId` in `IterableConfig` when initializing the SDK. This will disable old tokens to make sure users won't receive duplicate notifications.

## License

The MIT License

See [LICENSE](https://github.com/Iterable/iterable-android-sdk/blob/master/LICENSE)

## Want to Contribute?

This library is open source, and we will look at pull requests!

See [CONTRIBUTING](CONTRIBUTING.md) for more information.
