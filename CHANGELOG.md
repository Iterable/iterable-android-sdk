# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
#### Added
- nothing yet

#### Removed
- nothing yet

#### Changed
- nothing yet

#### Fixed
- nothing yet

## [3.2.7](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.2.7)
#### Added
- Added authentication support

## [3.2.6](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.2.6)
#### Added
- Added a new method - `setAutoDisplayPaused` to `InAppManager`. This method pauses the display of in-app messages and can be used to prevent interruptions in certain areas of your app.

#### Changed
- Changed `messageId` argument to be non-null in `trackPushOpen`.

#### Fixed
- Fixed an issue where the in-app message queue wasn't being refreshed on app launch.
- Removed warning messages from logs when using string resources for notification channel name.

## [3.2.5](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.2.5)
#### Changed
- `app_name` was removed from published strings and replaced with plain string values in test manifests.
- `disableToken` now disables all devices with the current device token when `email` and `userId` are not set.

#### Fixed
- Fixed an issue where in-app click events were not being registered after displaying an in-app message.
- Fixed NullPointerExceptions in `IterableInAppFragmentHTMLNotification` that could occur in some cases when the activity is destroyed and recreated.
- Fixed an issue where in-app messages were not getting cleared upon logout.

## [3.2.4](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.2.4)
#### Added
- Added support for new parameters - `mergeNestedObject` in `updateUser` method.
- Added public methods - `setDeviceAttribute` and `removeDeviceAttribute` to support additional device attributes.

#### Fixed
- Method tracing now logs only in VERBOSE log level.

## [3.2.3](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.2.3)
#### Added
- `IterableInAppMessage` now stores the `campaignId` it belongs to. (Thanks to @nkotula!)

#### Changed
- The SDK now uses `DialogFragment` to show in-app messages. In-app messages are more stable than before and resilient to device configuration changes like device rotation.

## [3.2.2](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.2.2)
#### Fixed
-  Fixed an ArrayIndexOutOfBoundsException in IterableRequest which is thrown from inside HttpUrlConnection/OkHttp module in certain Android firmwares

## [3.2.1](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.2.1)
#### Added
- Added support for new parameters - `subscribedMessageTypeIDs`, `campaignId`, `templateId` in `updateSubscriptions` method.

## [3.2.0](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.2.0)
#### Added
- **[Mobile Inbox](https://github.com/Iterable/iterable-android-sdk/blob/master/README.md#mobile-inbox)**

    Changes since beta:
     - Added support for various ways to customize the default interface for a mobile inbox
     - Added a sample project that demonstrates how to customize the default interface for a mobile inbox
     - Added tracking for inbox sessions (when the inbox is visible in the app) and inbox message impressions (when a individual message's item is visible in the mobile inbox message list)

#### Removed
- Removed all old initialization methods starting with `sharedInstanceWithApiKey`
- Removed `sendPush` methods (these API methods can't be called from mobile apps)
- Removed all deprecated methods with extra parameters for push registration:
  - `void registerDeviceToken(String token)` is the only one available now for token registration
  - `void disablePush()` is the only one available for disabling the current push token
  - Platform is always FCM, and push integration name is taken from `IterableConfig`

#### Changed
- The SDK now depends on *AndroidX* libraries. [Migrate your app to use AndroidX](https://developer.android.com/jetpack/androidx/migrate) before using version 3.2.0 or higher of the SDK.
- When `pushIntegrationName` is not set on `IterableConfig`, the SDK now defaults it to the app's package name.
If you've set up your push integration with the new Mobile Apps UI, you don't have to specify `pushIntegrationName` in the SDK anymore.

## [3.1.6](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.1.6)
#### Added
- Added a new static method to `IterableFirebaseMessagingService`: `isGhostPush`. Use this method to determine whether a Firebase message is an Iterable ghost push or silent push message.

#### Fixed
- Fixed the height of full-screen in-app messages to make sure they're not clipped by the Android navigation bar.
- The SDK doesn't log an error message anymore when a custom notification channel name is not set.

## [3.1.5](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.1.5)
#### Changed
- Automatic push registration is now only done if the app is running in foreground

## [3.1.4](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.1.4)
#### Added
- Push notifications now also display image thumbnails when collapsed

#### Changed
- Connection timeout for GET requests is now 3 seconds, to match other request timeouts
- `Api-Key` header is now also used for GET requests

#### Fixed
- Fixed possible exceptions when an in-app message is not on the screen when it's being resized

## [3.1.3](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.1.3)
#### Fixed
- Fixed a NullPointerException that could occur in some cases when trying to get Advertising ID

## [3.1.2](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.1.2)
#### Changed
- Removed FirebaseInstanceIDService dependency to support newer versions of `firebase-messaging` library. This bumps the minimum required version of `firebase-messaging` to 17.4.0.

#### Fixed
- Fixed deserialization of in-app messages stored with beta versions of the SDK

## [3.1.1](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.1.1)
#### Added
- SDK platform and version is now sent with every API request via headers

#### Changed
- `Api-Key` header name is now used for the API key instead of `api_key`, for consistency with HTTP header naming conventions

#### Fixed
- Fixed an issue that could cause the SDK not to persist in-app message properties (processed/consumed)
- Fixed a NullPointerException that could occur in IterableActivityMonitor if it was initialized after application start

## [3.1.0](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.1.0)

#### Added
**BREAKING CHANGES**

The in-app messaging implementation has been significantly improved:

* The SDK now maintains a local queue and keep it in sync with the server-side queue automatically
* Iterable servers now notify apps via silent push messages whenever the in-app message queue is updated
* In-app messages are shown by default whenever they arrive

Check the [In-app messages documentation](https://github.com/Iterable/iterable-android-sdk#in-app-messages) for more details.

Please refer to the [Migration guide](https://github.com/Iterable/iterable-android-sdk#migrating-in-app-messages-from-the-previous-version-of-the-sdk) if you've been using in-app messages in your app and updating a newer version of the SDK.

#### Changed
- **BREAKING CHANGE:** Added `IterableContext` argument to `IterableCustomActionHandler`
  
  The new method signature is:
  ```java
  boolean handleIterableCustomAction(IterableAction action, IterableActionContext actionContext)
  ```
  `actionContext` can be used to determine where the call is calling from - push message, in-app message, or a deep link.
- The SDK now sets `notificationsEnabled` flag on the device to indicate whether notifications are enabled for the app
- Changes to in-app links:
  - `action://` URL scheme is now reserved for app-specific custom actions.
  When a user clicks on a link with `href` = `action://myCustomAction`, the SDK calls `IterableCustomActionHandler.handleIterableCustomAction` with action type set to `myCustomAction`.
  - `iterable://` URL scheme is now reserved for actions handled by the SDK (i.e. future versions of the SDK may define `iterable://delete` as an action to delete the in-app message)
  - `itbl://` links will keep working as custom actions (similar to `action://` URLs) for backwards compatibility, but `itbl://` namespace is deprecated in favor of `action://`.
  - **Migration:** if you've been using `itbl://` links in the past, please update your templates with `action://` instead
- Connect timeout for deeplink resolution is now 3 seconds

#### Removed
- `spawnInAppNotification` has been removed. Please refer to the in-app migration guide (above)

#### Fixed
- Fixed the URL parameter in `inAppClick` event

## [3.0.9](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.0.9)
#### Changed
- The SDK now passes `preferUserId` flag to create a user by userId if it does not exist when using userId to identify the user
- Incresed the deep link timeout to 3 seconds

#### Fixed
- Fixed InAppClick event parameter to properly track the URL that was clicked
- Fixed a NullPointerException when an in-app message was being shown while the app was in background

## [3.0.8](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.0.8)
#### Added
- Added an option to specify notification channel name via manifest metadata (`iterable_notification_channel_name`)
- Added support for color resource references in `iterable_notification_color` manifest metadata parameter

#### Changed
- `updateEmail` can now be used if the user is identified with userId
- Connection timeout is now 3 seconds

#### Fixed
- Fixed a NullPointerException when the app has a plain-text label in `AndroidManifest.xml` instead of a string resource reference

## [3.0.7](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.0.7)
#### Added
- Added `updateEmail` method with success & failure callbacks

## [3.0.6](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.0.6)
#### Added
- Added public methods to `IterableFirebaseMessagingService` and `IterableFirebaseInstanceIDService` that can be called from a custom `FirebaseMessagingService` subclass

## [3.0.5](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.0.5)
#### Added
- Added a new field to `IterableConfig` - `logLevel` - to specify the log level for Iterable SDK log messages

#### Changed
- The SDK now uses `preferUserId` flag to create a user by userId instead of the deprecated `createUserForUserId` API

#### Fixed
- The SDK now catches any RuntimeExceptions that may sometimes happen when calling `getAdvertisingIdInfo`
- `registerDeviceToken` and `disableToken` now use the email/userId that was set at the time of the call, to handle login/logout correctly

## [3.0.4](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.0.4)
#### Added
- Added support for user registration with userId. The SDK will now create a new user for userId if it does not exist before registering the device on the user's profile.

## [3.0.3](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.0.3)
#### Added
- Added new device fields (Iterable SDK version, app version, app package name) to `registerDeviceToken` call
- Deferred Deep Linking support

#### Fixed
- Fixed a NullPointerException when an in-app was resized after being dismissed

## [3.0.2](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.0.2)
#### Added
- The SDK now registers the token when a new email/userId is set and disables the old device if email/userId was previously set and then changed. This can be disabled by setting `autoPushRegistration` to `false` in `IterableConfig`.

#### Fixed
- Fixed a NullPointerException when SDK isn't initialized in Application#onCreate and the app is opened from a push notification

## [3.0.1](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.0.1)
 _Released on 2018-08-10_

#### Fixed
- The new email is now persisted when `updateEmail` is called
- SDK now ensures that only one in-app message can be shown at a time

## [3.0.0](https://github.com/Iterable/iterable-android-sdk/releases/tag/3.0.0)
 _Released on 2018-07-30_
#### Added
- Full FCM support
- Added support for push action buttons
- Added a new SDK initialization method that takes `IterableConfig` object with configuration options
- User ID/email is now decoupled from SDK initialization. It can be changed by calling `setEmail` or `setUserId` on the `IterableApi` instance.
- The SDK now stores attribution data within 24 hours of opening the app from a push notififcation or from an App Link in an email
- Added two handlers: `IterableUrlHandler` and `IterableCustomActionHandler` that can be used to customize URL and custom action handling for push notifications
- Added `getPayloadData()` method to retrieve the entire notification payload for the notification that opened the app (thanks @steelbrain)

#### Removed
- Removed GCM support

#### Changed
- Old initialization methods (`sharedInstanceWithApiKey`) are now deprecated
- Old `registerForPush` and `registerDeviceToken` methods are now deprecated

#### Migration Notes
1. If you're using GCM, update your Android app to Firebase Cloud Messaging
2. Replace `IterableAPI.sharedInstanceWithApiKey(...)` with the following:
```java
IterableConfig config = new IterableConfig.Builder()
        .setPushIntegrationName("myPushIntegration")
        .setUrlHandler(this)        // If you want to handle URLs coming from push notifications
        .build();
IterableApi.initialize(context, "YOUR API KEY", config);
```
3. Call `registerForPush()` to retrieve the push token and register the device.
4. User email/userId is now persisted, so you'll only need to call `setEmail` or `setUserId` when the user logs in or logs out.
5. The SDK now tracks push opens automatically, as long as the SDK is initialized in `Application`'s `onCreate`. See README for instructions. Once it is set up, remove all direct calls to `trackPushOpen`.

## [2.2.5](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.2.5)
 _Released on 2018-03-31_
 
#### Changed
- Updated requests to not send when there is an exception while constructing the JSON request body.
 
#### Fixed
- Fixed the reference to internal fields in NotificationCompat.Builder for buildVersion 27.

## [2.2.4](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.2.4)
 _Released on 2018-03-07_
 
#### Fixed
- Fixed the load sequence for retrieving a notification image.

## [2.2.3](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.2.3)
 _Released on 2018-01-22_
 
#### Added
- Added non-empty data body for notification rendering.
- Added default channel id support.

## [2.2.2](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.2.2)
 _Released on 2017-11-30_
 
#### Fixed
- Fixed error in IterablePushRegistration when `getDeviceToken` returns an empty PushRegistrationObject.

## [2.2.1](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.2.1)
 _Released on 2017-11-20_
 
#### Added
- Added the `updateSubscriptions` function to create to modify channel, list, and message subscription preferences.

## [2.2.0](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.2.0)
 _Released on 2017-11-03_
 
#### Added
- Added support for html based in-app notifications.

## [2.1.9](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.1.9)
 _Released on 2017-10-20_
 
 
#### Fixed
- Fixed payload path for image url.

## [2.1.8](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.1.8)
 _Released on 2017-07-28_
 
#### Added
- Added support for android image notifications.
 
#### Fixed
- Fixed load error for empty image url.

## [2.1.7](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.1.7)
 _Released on 2017-07-19_
 
#### Fixed
- Fixed in-app button clicks without an action defined.

## [2.1.6](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.1.6)
 _Released on 2017-07-19_
 
#### Added
- Added the in-app consume logic to automatically remove the notification from list of in-app notifications.

#### Fixed
- Fixed the payloadfor trackInAppClick to contain the userId.

## [2.1.5](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.1.5)
 _Released on 2017-06-09_
 
#### Added
- Added full support for newly created Firebase applications
- Added new functionality for `registerForPush` which takes in the optional pushServicePlatform
	- `IterableConstants.MESSAGING_PLATFORM_GOOGLE` (GCM)
	- `IterableConstants. MESSAGING_PLATFORM_FIREBASE` (FCM)
- `IterableFirebaseInstanceIDService` handles firebase token registrations automatically on install.
- Added in default tracked device values for `registerDeviceToken`

#### Changed
- Changed IterablePushRegistrationGCM to IterablePushRegistration so the registration class is not GCM specific.
- Changed the disable logic to no longer enable the deviceToken prior to disabling.

## [2.1.4](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.1.4)
 _Released on 2017-02-23_

#### Fixed
- fixed uploaded pom file

## [2.1.3](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.1.3)
 _Released on 2017-02-22_

#### Added
- Added support for Android deeplink tracking
- `getAndTrackDeeplink` tracks a click and returns the destination url.

## [2.1.2](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.1.2)
 _Released on 2017-01-09_

#### Changed
- Updated the PendingIntent request code to use the messageId instead of the current timestamp.

## [2.1.1](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.1.1)
 _Released on 2017-01-09_

#### Fixed
- fixed overwritten pushnotification metadata on subsequent notifications

## [2.1.0](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.1.0)
 _Released on 2016-12-28_

- added support for In-App Notifications with different views layouts
	- Full screen 
	- Bottom
	- Center
	- Top
- includes tracking for In-App opens and clicks
- includes support for GET requests

## [2.0.1](https://github.com/Iterable/iterable-android-sdk/releases/tag/2.0.1)
 _Released on 2016-10-13_
 
#### Added 
- Added ability to send data by userId
