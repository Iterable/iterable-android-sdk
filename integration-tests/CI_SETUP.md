# GitHub Actions CI/CD Setup for Integration Tests

This document describes the GitHub Actions CI/CD setup for the Iterable Android SDK integration tests.

## Overview

The integration tests run automatically on:
- **Push** to `master` and `develop` branches
- **Pull Requests** targeting `master` and `develop` branches  
- **Scheduled runs** daily at 2 AM UTC
- **Manual triggers** via GitHub Actions UI

## Workflow Structure

### 1. Integration Tests Workflow (`integration-tests.yml`)

**Triggers:**
- Push to `master`/`develop` branches
- Pull requests to `master`/`develop` branches
- Daily scheduled runs at 2 AM UTC

**Test Matrix:**
- Android API levels: 21, 29, 34
- Runs on `macos-latest` runners

**Test Types:**
- In-App Message Integration Tests
- Push Notification Integration Tests
- All Integration Tests (nightly only)

### 2. Build and Test Workflow (`build.yml`)

**Triggers:**
- Push to `master` branch
- Pull requests

**Jobs:**
- Lint checks
- Unit tests
- Instrumentation tests

## Required Secrets

Configure the following secrets in your GitHub repository settings:

### Repository Secrets

1. **`ITERABLE_API_KEY`**
   - Description: Iterable API key for client-side operations
   - Usage: SDK initialization and API calls
   - Example: `live_abc123def456...`
   - Note: The API key is already tied to a specific project, so no separate project ID is needed

2. **`ITERABLE_SERVER_API_KEY`**
   - Description: Server-side API key for campaign triggering
   - Usage: Triggering campaigns via Iterable API
   - Example: `server_xyz789...`

### Setting Up Secrets

1. Go to your GitHub repository
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add each secret with the appropriate name and value

## Environment Setup

### Android SDK Configuration

The workflow automatically:
- Sets up Android SDK 34
- Installs required system images
- Creates Android Virtual Devices (AVDs)
- Configures emulator settings

### Emulator Configuration

**Hardware:**
- Memory: 2048MB
- Cores: 2
- GPU: Software rendering
- No audio/camera

**System Images:**
- API 21: `system-images;android-21;google_apis;x86_64`
- API 29: `system-images;android-29;google_apis;x86_64`
- API 34: `system-images;android-34;google_apis;x86_64`

### Permissions

The workflow automatically grants:
- `POST_NOTIFICATIONS` permission for push notification tests
- Disables animations for faster test execution

## Test Execution

### Running Tests Locally

```bash
# Run all integration tests
./gradlew :integration-tests:connectedCheck

# Run specific test class
./gradlew :integration-tests:connectedCheck \
  -Pandroid.testInstrumentationRunnerArguments.class=com.iterable.integration.tests.InAppMessageIntegrationTest

# Run with specific API level
./gradlew :integration-tests:connectedCheck \
  -Pandroid.testInstrumentationRunnerArguments.class=com.iterable.integration.tests.InAppMessageIntegrationTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.test.runner.AndroidJUnitRunner
```

### CI Test Execution

Tests run in the following order:
1. **In-App Message Tests** - Tests in-app message delivery, display, and interaction
2. **Push Notification Tests** - Tests push notification delivery and handling
3. **All Tests** (nightly only) - Complete test suite

## Artifacts and Reports

### Generated Artifacts

1. **Test Results**
   - Location: `integration-tests/build/reports/`
   - Format: HTML, XML
   - Contains: Test execution results, failures, timing

2. **Coverage Reports**
   - Location: `integration-tests/build/reports/jacoco/`
   - Format: HTML, XML, CSV
   - Contains: Code coverage metrics

3. **Test Logs**
   - Location: `integration-tests/build/test-logs.txt`
   - Contains: Android logcat output during test execution

4. **Screenshots** (if generated)
   - Location: `integration-tests/screenshots/`
   - Contains: Test execution screenshots for debugging

### Accessing Artifacts

1. Go to the GitHub Actions tab in your repository
2. Click on the specific workflow run
3. Scroll down to the **Artifacts** section
4. Download the relevant artifact files

## Debugging Failed Tests

### Common Issues

1. **Emulator Startup Failures**
   - Check available system resources
   - Verify system image downloads
   - Review emulator logs

2. **Test Timeouts**
   - Increase timeout values in test configuration
   - Check emulator performance
   - Review test complexity

3. **Permission Issues**
   - Verify notification permissions are granted
   - Check app installation status
   - Review permission manifest

4. **API Key Issues**
   - Verify secrets are correctly configured
   - Check API key validity
   - Review network connectivity

### Debugging Steps

1. **Check Test Logs**
   ```bash
   # Download and examine test logs
   cat integration-tests/build/test-logs.txt | grep -i error
   ```

2. **Review Test Reports**
   - Open HTML test reports
   - Check for specific test failures
   - Review stack traces

3. **Examine Screenshots**
   - Look for UI state issues
   - Verify element visibility
   - Check for rendering problems

4. **Check Coverage Reports**
   - Identify untested code paths
   - Review test effectiveness
   - Plan additional test coverage

## Performance Optimization

### CI Performance

- **Parallel Execution**: Tests run in parallel across API levels
- **Caching**: Gradle dependencies are cached between runs
- **Resource Management**: Emulators are properly cleaned up

### Test Performance

- **Animation Disabled**: Faster UI interactions
- **Optimized Timeouts**: Balanced between reliability and speed
- **Efficient Selectors**: Fast element identification

## Monitoring and Alerts

### Test Status Monitoring

- **Success Rate**: Track test pass/fail rates over time
- **Execution Time**: Monitor test duration trends
- **Coverage Trends**: Track code coverage changes

### Failure Notifications

- **Email Notifications**: Automatic failure notifications
- **Slack Integration**: Team notifications (if configured)
- **GitHub Status**: PR status checks

## Best Practices

### Test Development

1. **Isolation**: Each test should be independent
2. **Cleanup**: Proper resource cleanup in tearDown
3. **Reliability**: Use appropriate waits and timeouts
4. **Documentation**: Clear test descriptions and comments

### CI Maintenance

1. **Regular Updates**: Keep dependencies current
2. **Resource Monitoring**: Watch CI resource usage
3. **Performance Tuning**: Optimize test execution time
4. **Security**: Regularly rotate API keys

## Troubleshooting

### Workflow Failures

1. **Check Workflow Logs**
   - Review step-by-step execution
   - Look for error messages
   - Check environment setup

2. **Verify Secrets**
   - Confirm secrets are properly set
   - Check secret values are correct
   - Verify secret permissions

3. **Test Environment**
   - Verify emulator setup
   - Check Android SDK installation
   - Review system image availability

### Local Development

1. **Environment Setup**
   ```bash
   # Install required tools
   brew install android-platform-tools
   
   # Set up Android SDK
   export ANDROID_SDK_ROOT=/path/to/android-sdk
   export PATH=$PATH:$ANDROID_SDK_ROOT/tools:$ANDROID_SDK_ROOT/platform-tools
   ```

2. **Running Tests**
   ```bash
   # Start emulator
   emulator -avd test_device_api_34 -no-audio -no-window
   
   # Run tests
   ./gradlew :integration-tests:connectedCheck
   ```

## Support

For issues with the CI setup:

1. **Check Documentation**: Review this guide and related docs
2. **Examine Logs**: Look at workflow execution logs
3. **Test Locally**: Reproduce issues in local environment
4. **Create Issues**: Report bugs with detailed information

## Future Enhancements

1. **Test Parallelization**: Run tests in parallel within same emulator
2. **Visual Testing**: Add screenshot comparison tests
3. **Performance Testing**: Add performance benchmarks
4. **Cross-Platform**: Extend to iOS integration tests
5. **Advanced Reporting**: Enhanced test reporting and analytics
