# In-App Message MVP Test Setup

This document describes the MVP (Minimum Viable Product) setup for testing the `testInAppMessageMVP` method in the CI/CD pipeline.

## Overview

The MVP test focuses on the core functionality of in-app message display and interaction, providing a streamlined test that can run quickly and reliably in CI environments.

## Test Method

**Method**: `testInAppMessageMVP`  
**Class**: `com.iterable.integration.tests.InAppMessageIntegrationTest`  
**Purpose**: Verify basic in-app message functionality including:
- User sign-in
- In-app message triggering
- Message display
- Button interaction
- Message dismissal

## Workflow Configuration

The MVP test uses the existing workflow file: `.github/workflows/inapp-e2e-tests.yml` (renamed to "In-App Message MVP Tests")

### Key Features:
- **Focused Test**: Runs only the `testInAppMessageMVP` method
- **Multiple API Levels**: Tests on Android API 29 and 34
- **Streamlined Setup**: Uses existing infrastructure
- **Fast Execution**: Optimized for quick feedback

## Required Secrets

The following GitHub secrets must be configured in your repository:

1. **`ITERABLE_API_KEY`** - Your Iterable mobile API key
2. **`ITERABLE_SERVER_API_KEY`** - Your Iterable server API key

### Setting up Secrets:

1. Go to your GitHub repository
2. Navigate to Settings → Secrets and variables → Actions
3. Add the following secrets:
   - `ITERABLE_API_KEY`: Your mobile API key from Iterable dashboard
   - `ITERABLE_SERVER_API_KEY`: Your server API key from Iterable dashboard

## Test Configuration

The test uses the following configuration:

- **Test User Email**: Configured via `ITERABLE_TEST_USER_EMAIL` environment variable
- **Campaign ID**: Uses `TEST_INAPP_CAMPAIGN_ID` (14332357) from TestConstants
- **Event Name**: Uses `test_inapp_event` for triggering

## Running the MVP Test

### Locally:
```bash
# Set environment variables
export ITERABLE_API_KEY="your_mobile_api_key"
export ITERABLE_SERVER_API_KEY="your_server_api_key"
export ITERABLE_TEST_USER_EMAIL="test@example.com"

# Run the MVP test
./gradlew :integration-tests:connectedCheck \
  -Pandroid.testInstrumentationRunnerArguments.class=com.iterable.integration.tests.InAppMessageIntegrationTest#testInAppMessageMVP
```

### In CI:
The test runs automatically on:
- Push to `InApp-Display-E2E` branch
- Pull requests to `InApp-Display-E2E`, `master`, or `develop` branches
- Manual trigger via GitHub Actions

## Test Flow

1. **Setup**: Initialize IterableApi with custom handlers
2. **User Sign-in**: Ensure test user is signed in
3. **Trigger Campaign**: Use SDK track method to trigger in-app message
4. **Sync Messages**: Simulate background/foreground cycle
5. **Verify Display**: Check that in-app message is displayed
6. **Interact**: Click the "No Thanks" button
7. **Verify Dismissal**: Confirm message disappears

## Expected Results

The MVP test should:
- ✅ Successfully sign in the test user
- ✅ Trigger the in-app message campaign
- ✅ Display the in-app message on screen
- ✅ Allow button interaction
- ✅ Dismiss the message after interaction

## Troubleshooting

### Common Issues:

1. **API Key Issues**: Ensure secrets are properly configured
2. **Campaign Not Triggering**: Check campaign ID and user eligibility
3. **Message Not Displaying**: Verify custom handlers are set up correctly
4. **Button Click Failing**: Check UI automation selectors

### Debug Information:

The workflow collects:
- Test execution logs
- Screenshots at test completion
- Specific test logs filtered for Iterable components

## Next Steps

Once the MVP test is stable, you can:
1. Add more comprehensive test scenarios
2. Test on multiple API levels
3. Add performance metrics
4. Integrate with full test suite

## Files Modified

- `.github/workflows/inapp-mvp-test.yml` - MVP workflow configuration
- `integration-tests/MVP_TEST_SETUP.md` - This documentation
