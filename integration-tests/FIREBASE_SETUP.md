# Firebase Setup for Integration Tests

This guide explains how to set up Firebase for the Iterable Android SDK integration tests.

## Overview

The integration tests use Firebase Cloud Messaging (FCM) to receive push notifications sent from the Iterable backend. Here's how it works:

1. **Iterable Backend** → Sends push notifications via Firebase
2. **Firebase** → Delivers notifications to the device
3. **IntegrationFirebaseMessagingService** → Receives and processes notifications
4. **Iterable SDK** → Handles notification display and interaction
5. **Tests** → Verify notification delivery and functionality

## Setup Steps

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project"
3. Enter project name: `iterable-integration-tests`
4. Enable Google Analytics (optional)
5. Click "Create project"

### 2. Add Android App to Firebase

1. In Firebase Console, click "Add app" → "Android"
2. Enter package name: `com.iterable.integration.tests`
3. Enter app nickname: `Integration Tests`
4. Click "Register app"

### 3. Download Configuration File

1. Download `google-services.json`
2. Place it in the `integration-tests/` directory
3. The file should be at: `integration-tests/google-services.json`

### 4. Get Firebase Server Key

1. In Firebase Console, go to Project Settings
2. Click "Cloud Messaging" tab
3. Copy the "Server key" (starts with `AAAA...`)
4. This key will be used in Iterable backend configuration

### 5. Configure Iterable Backend

1. In Iterable dashboard, go to your test project
2. Navigate to Settings → Mobile Apps
3. Add Firebase configuration:
   - **Firebase Server Key**: Paste the server key from step 4
   - **Package Name**: `com.iterable.integration.tests`
   - **App Name**: `Integration Tests`

### 6. Set Environment Variables

```bash
# Mobile API Key (for app-side SDK)
export ITERABLE_API_KEY="your_mobile_api_key"

# Server API Key (for backend API calls)
export ITERABLE_SERVER_API_KEY="your_server_api_key"

# Project ID
export ITERABLE_PROJECT_ID="your_project_id"
```

## File Structure

After setup, your project should look like this:

```
integration-tests/
├── google-services.json          # Firebase configuration
├── build.gradle                  # Module build file
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/iterable/integration/tests/
│   │   │       ├── services/
│   │   │       │   └── IntegrationFirebaseMessagingService.kt
│   │   │       └── utils/
│   │   │           └── IntegrationTestUtils.kt
│   │   └── AndroidManifest.xml
│   └── androidTest/
│       └── java/
│           └── com/iterable/integration/tests/
│               └── PushNotificationIntegrationTest.kt
└── README.md
```

## Testing Push Notifications

### Manual Testing

1. **Install the app**
   ```bash
   ./gradlew :integration-tests:installDebug
   ```

2. **Launch the app**
   - Navigate to "Push Notification Tests"
   - Send test notifications via Iterable backend
   - Verify notifications appear

### Automated Testing

```bash
# Run push notification tests
./gradlew :integration-tests:connectedCheck -Pandroid.testInstrumentationRunnerArguments.class=com.iterable.integration.tests.PushNotificationIntegrationTest
```

## Troubleshooting

### Common Issues

1. **Notifications not received**
   - Verify `google-services.json` is in correct location
   - Check Firebase Server Key in Iterable settings
   - Ensure app has notification permissions

2. **Build errors**
   - Verify Google Services plugin is applied
   - Check package name matches in `google-services.json`
   - Ensure Firebase dependencies are included

3. **FCM token not registered**
   - Check `IntegrationFirebaseMessagingService` logs
   - Verify Firebase project configuration
   - Ensure app is properly signed

### Debug Commands

```bash
# Check if FCM token is generated
adb logcat | grep "FCM"

# Check notification permissions
adb shell dumpsys notification | grep "iterable"

# Test push notification manually
adb shell am broadcast -a com.google.android.c2dm.intent.RECEIVE \
  -n com.iterable.integration.tests/.services.IntegrationFirebaseMessagingService \
  --es "message" "test"
```

## Security Notes

1. **Never commit `google-services.json`** to version control
2. **Use environment variables** for API keys in CI/CD
3. **Restrict Firebase project** to test environments only
4. **Monitor usage** to prevent abuse

## Next Steps

After Firebase setup is complete:

1. **Run initial tests** to verify configuration
2. **Set up CI/CD** with environment variables
3. **Create test campaigns** in Iterable backend
4. **Configure notification templates** for testing

## Support

For issues with:
- **Firebase setup**: Check [Firebase documentation](https://firebase.google.com/docs)
- **Iterable configuration**: Contact Iterable support
- **Integration tests**: Check logs and test reports 