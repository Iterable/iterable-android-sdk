<img src="Iterable-Logo.svg">

[![Build Status](https://travis-ci.org/Iterable/iterable-android-sdk.svg?branch=master)](https://travis-ci.org/Iterable/iterable-android-sdk)
[![codecov](https://codecov.io/gh/Iterable/iterable-android-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/Iterable/iterable-android-sdk)
[![Download](https://api.bintray.com/packages/davidtruong/maven/Iterable-SDK/images/download.svg)](https://bintray.com/davidtruong/maven/Iterable-SDK/_latestVersion)

# Iterable Android SDK

The Iterable Android SDK is a Java implementation of an Android client for Iterable, supporting Android API versions 15 and higher.

## Installation

- [Installation and configuration of the Iterable Android SDK](https://support.iterable.com/hc/articles/360035019712-Iterable-s-Android-SDK-)

## Sample apps

This repository contains the following sample apps:

- [Inbox Customization](https://github.com/Iterable/iterable-android-sdk/tree/master/sample-apps/inbox-customization)

## Using the SDK

### Push notifications

- [Setting up Android Push Notifications](https://support.iterable.com/hc/articles/115000331943)

### Deep linking

A deep link is a URI that links to a specific location within your mobile 
app. The following sections describe how to work with deep links using
Iterable's Android SDK.

- [Deep Links in Push Notifications](https://support.iterable.com/hc/en-us/articles/360035453971#android-deep-links)
- [Android App Links](https://support.iterable.com/hc/en-us/articles/360035127392)
- [Deferred deep linking](https://support.iterable.com/hc/articles/360035165872)

### In-app messages

- [In-App Messages on Android](https://support.iterable.com/hc/en-us/articles/360035537231)

### Mobile Inbox

Apps using version 3.2.0 and later of this SDK can allow users to save in-app
messages to an inbox. This inbox displays a list of saved in-app messages and
allows users to read them at their convenience. The SDK provides a default user
interface for the inbox, which can be customized to match your brand's styles.

To learn more about Mobile Inbox, how to customize it, and events related to
its usage, read:

- [In-App Messages and Mobile Inbox](https://support.iterable.com/hc/articles/217517406)
- [Sending In-App Messages](https://support.iterable.com/hc/articles/360034903151)
- [Events for In-App Messages and Mobile Inbox](https://support.iterable.com/hc/articles/360038939972)
- [Setting up Mobile Inbox on Android](https://support.iterable.com/hc/articles/360038744152)
- [Customizing Mobile Inbox on Android](https://support.iterable.com/hc/articles/360039189931)

### Tracking custom events

- [Custom events](https://support.iterable.com/hc/articles/360035395671)
    
### User fields

- [Updating User Profiles](https://support.iterable.com/hc/articles/360035402611)
    
### Uninstall tracking

- [Uninstall tracking](https://support.iterable.com/hc/articles/205730229#uninstall)
## Optional setup

### Migrating from a version prior to 3.1.0

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

### GCM -> Firebase migration

The recommended migration path is to upgrade the existing Google Cloud project to Firebase, update the server token in the existing GCM push integration in Iterable with the new Firebase token, and update the Android app to support Firebase. If you keep using the same project for FCM and the same integration name, the old tokens will still be valid and won't require re-registration of existing devices.

If you're using a different project for FCM and have existing devices on a GCM project with a different sender ID, updating the app will generate new tokens for users, but the old tokens will still be valid. When migrating from one sender ID to another, specify `legacyGCMSenderId` in `IterableConfig` when initializing the SDK. This will disable old tokens to make sure users won't receive duplicate notifications.

## Additional information

For more information, take a look at Iterable's [Mobile Developer Guides](https://support.iterable.com/hc/categories/360002288712).

## License

The MIT License

See [LICENSE](https://github.com/Iterable/iterable-android-sdk/blob/master/LICENSE)

## Want to contribute?

This library is open source, and we will look at pull requests!

See [CONTRIBUTING](CONTRIBUTING.md) for more information.
