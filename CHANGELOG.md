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
