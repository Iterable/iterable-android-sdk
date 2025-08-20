# Iterable Android SDK Integration Tests

This module provides comprehensive integration testing for the Iterable Android SDK, ensuring that all critical features work correctly with the actual Iterable backend.

## Overview

The integration tests are designed to verify end-to-end functionality of the SDK, including:

1. **Push Notifications** - Configuration, delivery, display, and interaction
2. **In-App Messages** - Display, interaction, and deep linking
3. **Embedded Messages** - Eligibility, display, and metrics
4. **Deep Linking** - URL handling, app navigation, and intent processing

## Architecture

### Test Structure

```
integration-tests/
├── src/
│   ├── main/
│   │   ├── java/com/iterable/integration/tests/
│   │   │   ├── MainActivity.kt                    # Main test app activity
│   │   │   ├── activities/                        # Test-specific activities
│   │   │   ├── services/
│   │   │   │   └── IntegrationFirebaseMessagingService.kt  # Firebase service
│   │   │   └── utils/
│   │   │       └── IntegrationTestUtils.kt       # Test utilities
│   │   └── res/                                   # App resources
│   └── androidTest/
│       └── java/com/iterable/integration/tests/
│           ├── BaseIntegrationTest.kt             # Base test class
│           ├── PushNotificationIntegrationTest.kt # Push notification tests
│           ├── InAppMessageIntegrationTest.kt     # In-app message tests
│           ├── EmbeddedMessageIntegrationTest.kt  # Embedded message tests
│           └── DeepLinkIntegrationTest.kt         # Deep link tests
```

### Key Components

1. **BaseIntegrationTest** - Common setup, utilities, and test infrastructure
2. **IntegrationTestUtils** - Helper methods for all test scenarios
3. **Test Activities** - UI for manual testing and verification
4. **Firebase Services** - Handle push notifications from Iterable backend
5. **Test Receivers** - Capture and verify test events

## Setup

### Prerequisites

1. **Iterable Project Setup**
   - Create a test project in Iterable production
   - Note the API key and project ID
   - Configure push notification settings

2. **Firebase Setup**
   - Create a Firebase project
   - Add the Android app to Firebase
   - Download `google-services.json`
   - Configure Firebase Cloud Messaging

3. **Environment Variables**
   ```bash
   export ITERABLE_API_KEY="your_mobile_api_key"
   export ITERABLE_PROJECT_ID="your_project_id"
   export ITERABLE_SERVER_API_KEY="your_server_api_key"
   ```

### Installation

1. **Add the module to your project**
   ```gradle
   // settings.gradle
   include ':integration-tests'
   ```

2. **Configure the build**
   ```gradle
   // integration-tests/build.gradle
   // Already configured with all necessary dependencies
   ```

3. **Add Firebase configuration**
   ```bash
   # Copy google-services.json to integration-tests/
   cp google-services.json integration-tests/
   ```

## Firebase Integration

### How Push Notifications Work

1. **Iterable Backend** sends push notifications via Firebase Cloud Messaging
2. **Firebase** delivers the notification to the device
3. **IntegrationFirebaseMessagingService** receives the notification
4. **Iterable SDK** processes the notification and displays it
5. **Tests** verify the notification was received and displayed correctly

### Firebase Configuration

1. **Add Android App to Firebase**
   - Package name: `com.iterable.integration.tests`
   - SHA-1 fingerprint for your signing key

2. **Download Configuration**
   - `google-services.json` file
   - Place in `integration-tests/` directory

3. **Configure Iterable Backend**
   - Add Firebase Server Key to Iterable project settings
   - Configure push notification campaigns

## Test Scenarios

### 1. Push Notifications

**Tests Covered:**
- ✅ Push notification configuration
- ✅ Device receives push notification
- ✅ Device has permission granted
- ✅ Push notification is displayed
- ✅ Push delivery metrics are captured
- ✅ Tapping notification tracks open
- ✅ Tapping buttons with deep link invokes handlers
- ✅ Silent push works for in-app messages

**Manual Testing:**
1. Launch the integration test app
2. Navigate to "Push Notification Tests"
3. Send test notifications via Iterable backend
4. Verify notification display and interaction

**Automated Testing:**
```bash
./gradlew :integration-tests:connectedCheck
```

### 2. In-App Messages

**Tests Covered:**
- ✅ Silent push triggers in-app message
- ✅ In-app message is displayed
- ✅ Track in-app open metrics
- ✅ In-app message deep linking
- ✅ Custom action handlers
- ✅ Complete end-to-end flow validation
- ✅ Message lifecycle management

**JIRA Requirements Validated:**
1. **Silent push works** - Verified silent push notifications trigger in-app messages
2. **Confirm In App is displayed** - Validated in-app message rendering and display
3. **Track In App Open metric are validated** - Confirmed metrics tracking functionality
4. **Confirm In App is able to Deep link** - Tested deep linking capabilities
5. **Handlers are called and app navigated to certain module** - Verified action handler invocation

**Manual Testing:**
1. Navigate to "In-App Message Tests"
2. Use individual test buttons for specific scenarios
3. Run complete end-to-end test for full validation
4. Monitor test status and logs in real-time
5. Verify message display and interaction

**Automated Testing:**
```bash
# Run all in-app tests
./gradlew :integration-tests:connectedCheck -Pandroid.testInstrumentationRunnerArguments.class=com.iterable.integration.tests.InAppMessageIntegrationTest

# Run specific test
./gradlew :integration-tests:connectedCheck -Pandroid.testInstrumentationRunnerArguments.class=com.iterable.integration.tests.InAppMessageIntegrationTest#testSilentPushFunctionality
```

### 3. Embedded Messages

**Tests Covered:**
- ✅ Project eligibility configuration
- ✅ User list management
- ✅ Eligible message display
- ✅ Embedded metrics verification
- ✅ Deep linking functionality
- ✅ User profile eligibility changes

**Manual Testing:**
1. Navigate to "Embedded Message Tests"
2. Toggle user eligibility
3. Verify message display and metrics

### 4. Deep Linking

**Tests Covered:**
- ✅ SMS/Email flow with URL launch
- ✅ Project tracking domain configuration
- ✅ Associated domains setup (iOS equivalent)
- ✅ Intent filters configuration
- ✅ Deep link handler invocation

**Manual Testing:**
1. Navigate to "Deep Link Tests"
2. Test various deep link scenarios
3. Verify app navigation and handler calls

## Running Tests

### Local Development

```bash
# Run all integration tests
./gradlew :integration-tests:connectedCheck

# Run specific test class
./gradlew :integration-tests:connectedCheck -Pandroid.testInstrumentationRunnerArguments.class=com.iterable.integration.tests.PushNotificationIntegrationTest

# Run with coverage
./gradlew :integration-tests:jacocoIntegrationTestReport
```

### CI/CD Integration

The tests are designed to run in CI environments with:

1. **Emulator Setup** - Android emulator with API 21+
2. **Environment Variables** - API keys and configuration
3. **Firebase Configuration** - Real push notification testing
4. **Test Reporting** - Coverage and test results

### Manual Testing

1. **Install the app**
   ```bash
   ./gradlew :integration-tests:installDebug
   ```

2. **Launch the app**
   - Navigate through different test scenarios
   - Verify functionality manually
   - Check logs for detailed information

### In-App Message Testing

The `InAppMessageTestActivity` provides a comprehensive UI for testing in-app functionality:

1. **Individual Test Scenarios**
   - **Test Silent Push** - Verifies silent push triggers in-app messages
   - **Test In-App Display** - Confirms in-app message rendering
   - **Test Metrics Tracking** - Validates metrics collection
   - **Test Deep Linking** - Tests deep link handling
   - **Test Action Handlers** - Verifies custom action execution

2. **Complete End-to-End Test**
   - Runs all scenarios in sequence
   - Provides comprehensive test report
   - Shows pass/fail status for each requirement

3. **Real-time Monitoring**
   - Live test status updates
   - Detailed logging with timestamps
   - Test state management
   - Error reporting and debugging

## Configuration

### Iterable Backend Configuration

1. **Create Test Campaigns**
   - Push notification campaigns
   - In-app message campaigns
   - Embedded message campaigns

2. **Configure User Lists**
   - Test user lists for embedded messages
   - Eligibility criteria

3. **Set Up Deep Links**
   - Configure tracking domains
   - Set up destination URLs

4. **Configure Firebase Integration**
   - Add Firebase Server Key to project settings
   - Configure push notification templates

### Firebase Configuration

1. **Add Android App**
   - Package name: `com.iterable.integration.tests`
   - SHA-1 fingerprint

2. **Download Configuration**
   - `google-services.json` file
   - Firebase Server Key for Iterable backend

## Test Data Management

### Test Users
- Email: `akshay.ayyanchira@iterable.com` (Primary test user)
- Email: `integration.test@iterable.com` (Secondary test user)
- User ID: `integration_test_user`

### Test Campaigns
- Push notifications: `test_push_campaign`
- In-app messages: `14332357` (TEST_INAPP_CAMPAIGN_ID)
- Silent push campaigns: `14332360` (TEST_SILENT_PUSH_CAMPAIGN_ID)
- Deep link campaigns: `14332361` (TEST_DEEP_LINK_CAMPAIGN_ID)
- Action handler campaigns: `14332362` (TEST_ACTION_HANDLER_CAMPAIGN_ID)
- Embedded messages: `test_embedded_campaign`

### Test Events
- `test_event` - Triggers in-app messages
- `test_embedded_event` - Triggers embedded messages

## Troubleshooting

### Common Issues

1. **Push Notifications Not Received**
   - Verify Firebase configuration
   - Check FCM token registration
   - Ensure notification permissions
   - Verify Iterable backend configuration

2. **In-App Messages Not Displaying**
   - Verify campaign configuration
   - Check user eligibility
   - Review event tracking

3. **Deep Links Not Working**
   - Verify intent filter configuration
   - Check URL scheme registration
   - Test with adb commands

### Debug Information

Enable debug logging:
```kotlin
Log.d("IntegrationTest", "Debug information")
```

Check test state:
```kotlin
testUtils.resetTestStates()
```

## Contributing

When adding new integration tests:

1. **Extend BaseIntegrationTest** for common functionality
2. **Use IntegrationTestUtils** for helper methods
3. **Follow the test naming convention** - `test[Feature][Scenario]`
4. **Add manual test activities** for UI verification
5. **Update this README** with new test scenarios

## Best Practices

1. **Test Isolation** - Each test should be independent
2. **Timeout Handling** - Use appropriate timeouts for async operations
3. **State Management** - Reset test state between runs
4. **Error Handling** - Graceful handling of test failures
5. **Logging** - Comprehensive logging for debugging

## New In-App Integration Tests

The `InAppMessageIntegrationTest` class provides comprehensive automated testing for in-app functionality:

### Test Coverage

1. **Silent Push Functionality** (`testSilentPushFunctionality`)
   - Validates silent push notification delivery
   - Verifies in-app message triggering
   - Tests end-to-end silent push flow

2. **In-App Message Display** (`testInAppMessageDisplay`)
   - Confirms campaign triggering via API
   - Validates message rendering and display
   - Verifies message content and metadata

3. **Metrics Tracking** (`testInAppMetricsTracking`)
   - Tests in-app open tracking
   - Validates click tracking
   - Confirms delivery tracking
   - Verifies read status management

4. **Deep Linking** (`testInAppDeepLinking`)
   - Tests URL handling capabilities
   - Validates deep link processing
   - Confirms handler invocation

5. **Action Handlers** (`testInAppActionHandlers`)
   - Tests custom action execution
   - Validates handler invocation
   - Confirms action processing

6. **Complete End-to-End Flow** (`testCompleteInAppEndToEndFlow`)
   - Runs all scenarios in sequence
   - Validates complete user journey
   - Provides comprehensive validation

7. **Message Lifecycle** (`testInAppMessageLifecycle`)
   - Tests message creation and storage
   - Validates display and interaction
   - Confirms cleanup and removal

### Running the Tests

```bash
# Run all in-app tests
./gradlew :integration-tests:connectedCheck -Pandroid.testInstrumentationRunnerArguments.class=com.iterable.integration.tests.InAppMessageIntegrationTest

# Run specific test
./gradlew :integration-tests:connectedCheck -Pandroid.testInstrumentationRunnerArguments.class=com.iterable.integration.tests.InAppMessageIntegrationTest#testSilentPushFunctionality

# Run with coverage
./gradlew :integration-tests:jacocoIntegrationTestReport
```

## Future Enhancements

1. **Test Orchestration** - Automated test sequence execution
2. **Performance Testing** - SDK performance under load
3. **Stress Testing** - High-volume message testing
4. **Cross-Platform Testing** - iOS equivalent tests
5. **Analytics Integration** - Test result analytics and reporting 