#!/bin/bash

# In-App Message Integration Test Runner
# This script runs the in-app message integration tests with proper configuration

set -e

echo "🚀 Starting In-App Message Integration Tests"
echo "=============================================="

# Check if device is connected
echo "📱 Checking device connection..."
adb devices | grep -q "device$" || {
    echo "❌ No device connected. Please connect a device and try again."
    exit 1
}

echo "✅ Device connected"

# Check if environment variables are set
echo "🔧 Checking environment configuration..."
if [ -z "$ITERABLE_API_KEY" ]; then
    echo "⚠️  WARNING: ITERABLE_API_KEY not set. Tests may fail."
fi

if [ -z "$ITERABLE_SERVER_API_KEY" ]; then
    echo "⚠️  WARNING: ITERABLE_SERVER_API_KEY not set. Campaign triggering may fail."
fi

# Build the integration tests module
echo "🔨 Building integration tests..."
./gradlew :integration-tests:assembleDebugAndroidTest

# Run the in-app message tests
echo "🧪 Running In-App Message Integration Tests..."
echo ""

# Run basic in-app message tests
echo "📋 Running basic in-app message tests..."
./gradlew :integration-tests:connectedAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.iterable.integration.tests.InAppMessageIntegrationTest \
    --info

echo ""

# Run activity-based in-app message tests
echo "📋 Running activity-based in-app message tests..."
./gradlew :integration-tests:connectedAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.iterable.integration.tests.InAppMessageActivityIntegrationTest \
    --info

echo ""
echo "✅ In-App Message Integration Tests completed!"
echo "=============================================="

# Generate test report
echo "📊 Generating test report..."
./gradlew :integration-tests:connectedAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.iterable.integration.tests.InAppMessageIntegrationTest \
    --continue

echo "📈 Test report generated in integration-tests/build/reports/androidTests/connected/"
