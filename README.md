# Iterable Android SDK

The `iterable-android-sdk` is a java implementation of an android client for Iterable, supporting api versions 15 and higher.

## Setting up a push integration in Iterable

Before you even start with the SDK, you will need to: 

1. Set your application up to receive push notifications, and 
2. Set up a push integration in Iterable. This allows Iterable to communicate on your behalf with Google's Push Notification Service.

For information on setting up your Google Api Project, see

* [Configuring Push Notifications](http://docs.aws.amazon.com/sns/latest/dg/mobile-push-gcm.html)

To setup your push integration with Iterable in the web dashboard go to `Integrations -> Mobile Push`. When creating an integration, you will need to pick a name and a platform. The name is entirely up to you; it will be the `applicationName` when you use `registerForPush` or `registerDeviceToken` in our SDK. 

The platform will be `GCM` (This also includes FCM since it runs off of GCM). Add the Api server key (If on FCM use the Legacy Server Key).

![Creating an integration in Iterable](https://support.iterable.com/hc/en-us/article_attachments/211841066/2016-12-08_1442.png)

Congratulations, you've configured your mobile application to receive push notifications! Now, let's set up the Iterable SDK...

## Automatic Installation

See [Bintray](https://bintray.com/davidtruong/maven/Iterable-SDK) for the latest version of the Iterable Android SDK. 

## Additional Information

See our [setup guide](http://support.iterable.com/hc/en-us/articles/204780589-Push-Notification-Setup-iOS-and-Android-) for more information.

Also see our [push notification setup FAQs](http://support.iterable.com/hc/en-us/articles/206791196-Push-Notification-Setup-FAQ-s).

##Optional Setup

### Firebase Messaging
At this time there is no requirement to upgrade to FCM since Google will continue to support current versions of GCM android.

If you want to use using Firebase Cloud Messaging (FCM) instead of Google Cloud Messaging (GCM) pass in `IterableConstants. MESSAGING_PLATFORM_FIREBASE` as the pushServicePlatform.

```java
public void registerForPush(String iterableAppId, String projectNumber, String pushServicePlatform) {
```

**Note**: If you are upgrading to FCM, do not downgrade back to GCM as this will cause devices to be registered for notifications twice and users will get duplicate notifications.

### InApp Notifications
To display the user's InApp notifications call `spawnInAppNotification` with a defined `IterableActionHandler` callback handler. When a user clicks a button on the notification, the defined handler is called and passed the action name defined in the InApp template. If no action is defined, the callback handler will not be called.

InApp opens and button clicks are automatically tracked when the notification is called via `spawnInAppNotification`. `spawnInAppNotification` automatically consumes and removes the notification from the user's list of pending notification. If you do not want to remove the notification use `getInAppMessages` & `IterableInAppManager.showNotification` instead.

### Deeplinking

See our [Deeplinking Setup Guide] (https://support.iterable.com/hc/en-us/articles/211676923)

From your application's [onCreate] (https://developer.android.com/reference/android/app/Activity.html#onCreate(android.os.Bundle)) call `getAndTrackDeeplink` along with a callback to handle the destination deeplink url.

```java
protected void onCreate(Bundle savedInstanceState) {
	String dataUri = this.getIntent().getDataString();
	IterableHelper.IterableActionHandler clickCallback = 
		new IterableHelper.IterableActionHandler(){
			@Override
			public void execute(String result) {
			    Log.d("HandleDeeplink", "Redirected to: "+ result);
			    //handle deeplink here
			}
		};
	
	IterableApi.getAndTrackDeeplink(dataUri, clickCallback);
}
```

## License

The MIT License

See [LICENSE](https://github.com/Iterable/iterable-android-sdk/blob/master/LICENSE)

## Want to Contribute?

This library is open source, and we will look at pull requests!

See [CONTRIBUTING](CONTRIBUTING.md) for more information.