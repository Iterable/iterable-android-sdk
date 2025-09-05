# In-App Message Integration Testing

This document describes the comprehensive in-app message integration testing framework for the Iterable Android SDK.

## Overview

The in-app message testing framework provides end-to-end testing capabilities for:
- In-app message delivery and display
- HTML content rendering in WebView
- Button click interactions
- Event tracking (click, close, open)
- Message dismissal and cleanup

## Test Framework Components

### 1. BaseIntegrationTest.kt
Base class that provides common functionality for all integration tests:
- SDK initialization with custom handlers
- Test environment setup and cleanup
- Utility methods for waiting and verification
- Campaign triggering via Iterable API

### 2. InAppMessageIntegrationTest.kt
Main integration test class that tests in-app message functionality:
- `testInAppMessageDeliveryAndDisplay()` - Core test for message display and interaction
- `testInAppMessageWithCustomEvent()` - Tests event-triggered messages
- `testInAppMessageBackButtonDismissal()` - Tests back button dismissal

### 3. InAppMessageActivityIntegrationTest.kt
Enhanced test class that uses a dedicated test activity:
- `testInAppMessageDeliveryAndDisplayWithActivity()` - Uses test activity for better control
- `testInAppMessageWithTestActivity()` - Tests activity-based message handling
- `testInAppMessageClearFunctionality()` - Tests message clearing functionality

### 4. InAppTestActivity.kt
Dedicated test activity for in-app message testing:
- Provides a controlled environment for testing
- Includes a WebView for displaying message content
- Offers manual trigger and clear functionality
- Shows real-time status updates

## Testing Capabilities

### UI Testing Framework
The framework uses multiple UI testing approaches:

1. **UiAutomator** - For finding and interacting with native Android elements
2. **Espresso Web** - For WebView content testing and HTML element interaction
3. **Espresso** - For standard UI element interaction

### WebView Testing
- HTML content verification using DOM matchers
- Button click simulation within HTML content
- URL click handling and verification
- Content loading verification

### Event Tracking
- In-app message open events
- In-app message click events
- In-app message close events
- Custom action handling

## Test Scenarios

### 1. Basic Message Display
```kotlin
@Test
fun testInAppMessageDeliveryAndDisplay() {
    // Trigger campaign
    val campaignTriggered = triggerCampaignViaAPI(TEST_CAMPAIGN_ID)
    Assert.assertTrue("Campaign should be triggered", campaignTriggered)
    
    // Wait for message display
    val messageDisplayed = waitForInAppMessage(30)
    Assert.assertTrue("Message should be displayed", messageDisplayed)
    
    // Verify visibility
    val messageVisible = verifyInAppMessageVisible()
    Assert.assertTrue("Message should be visible", messageVisible)
}
```

### 2. Button Interaction
```kotlin
// Find and click button in in-app message
val buttonClicked = clickInAppMessageButton()
Assert.assertTrue("Should be able to click button", buttonClicked)

// Verify click event tracking
val clickTracked = waitForInAppClickEvent(10)
Assert.assertTrue("Click should be tracked", clickTracked)
```

### 3. Message Dismissal
```kotlin
// Press back button to dismiss
uiDevice.pressBack()

// Verify message is dismissed
val messageDismissed = waitForInAppMessageDismissed(10)
Assert.assertTrue("Message should be dismissed", messageDismissed)
```

## Configuration

### Build Configuration
The integration tests require the following build configuration:

```gradle
androidTestImplementation 'androidx.test.uiautomator:uiautomator:2.2.0'
androidTestImplementation 'androidx.test.espresso:espresso-web:3.5.1'
androidTestImplementation 'androidx.test.espresso:espresso-intents:3.5.1'
androidTestImplementation 'androidx.test.espresso:espresso-contrib:3.5.1'
androidTestImplementation 'org.awaitility:awaitility:4.2.0'
```

### Environment Variables
Set the following environment variables for API access:
- `ITERABLE_API_KEY` - Your Iterable API key
- `ITERABLE_SERVER_API_KEY` - Server-side API key for campaign triggering
- `ITERABLE_PROJECT_ID` - Your Iterable project ID

## Running Tests

### Run All In-App Message Tests
```bash
./gradlew :integration-tests:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.iterable.integration.tests.InAppMessageIntegrationTest
```

### Run Specific Test
```bash
./gradlew :integration-tests:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.iterable.integration.tests.InAppMessageIntegrationTest#testInAppMessageDeliveryAndDisplay
```

### Run Activity-Based Tests
```bash
./gradlew :integration-tests:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.iterable.integration.tests.InAppMessageActivityIntegrationTest
```

## Test Data Requirements

### Campaign Setup
1. Create an in-app message campaign in Iterable
2. Set the campaign to trigger on a specific event or immediately
3. Include HTML content with clickable buttons
4. Configure the campaign for the test user email

### Test User
- Email: `akshay.ayyanchira@iterable.com`
- Ensure the user is properly configured in Iterable
- User should have appropriate permissions for in-app messages

## Debugging

### Logs
The tests provide comprehensive logging:
- `BaseIntegrationTest` - SDK initialization and setup
- `InAppMessageIntegrationTest` - Test execution and verification
- `InAppTestActivity` - Activity state and WebView interactions

### Common Issues
1. **Message not displaying**: Check campaign configuration and user eligibility
2. **Button clicks not working**: Verify HTML content and WebView configuration
3. **Events not tracking**: Check custom handlers and API configuration
4. **Test timeouts**: Increase timeout values or check device performance

### Manual Testing
Use the `InAppTestActivity` for manual testing:
1. Launch the activity
2. Tap "Trigger In-App" to test message display
3. Use "Clear Messages" to reset state
4. Monitor status text for real-time feedback

## Best Practices

1. **Test Isolation**: Each test should be independent and clean up after itself
2. **Timeout Management**: Use appropriate timeouts for different operations
3. **Error Handling**: Implement proper error handling and logging
4. **Resource Cleanup**: Always clean up resources in tearDown methods
5. **Real Device Testing**: Test on real devices for accurate results

## Future Enhancements

1. **Screenshot Testing**: Add visual regression testing
2. **Performance Testing**: Measure message load times and interactions
3. **Accessibility Testing**: Verify accessibility compliance
4. **Multi-Device Testing**: Test across different screen sizes and orientations
5. **Automated Campaign Creation**: Programmatically create test campaigns

## Troubleshooting

### Test Failures
1. Check device logs for detailed error information
2. Verify Iterable API configuration
3. Ensure test user has proper permissions
4. Check network connectivity and API availability

### Performance Issues
1. Use appropriate polling intervals
2. Implement proper timeout handling
3. Clean up resources promptly
4. Use efficient UI element selectors

This testing framework provides a robust foundation for ensuring in-app message functionality works correctly across different scenarios and devices.
