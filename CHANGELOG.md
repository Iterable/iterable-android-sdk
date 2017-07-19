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
