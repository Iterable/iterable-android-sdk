#!/bin/bash

# This script is to be used by LLMs and AI agents to build the Iterable Android SDK.
# It uses Gradle to build the project and shows errors in a clean format.
# It also checks if the build is successful and exits with the correct status.
# 
# Usage: ./agent/build.sh [--clean]
#   --clean: Force a clean build (slower, but ensures clean state)

# Note: Not using set -e because we need to handle build failures gracefully

echo "Building Iterable Android SDK..."

# Create a temporary file for the build output
TEMP_OUTPUT=$(mktemp)

# Function to clean up temp file on exit
cleanup() {
    rm -f "$TEMP_OUTPUT"
}
trap cleanup EXIT

# Check if we have Android SDK
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "⚠️  Warning: ANDROID_HOME or ANDROID_SDK_ROOT not set. Build may fail if Android SDK is not in PATH."
fi

# Parse command line arguments for clean build option
CLEAN_BUILD=false
if [[ "$1" == "--clean" ]]; then
    CLEAN_BUILD=true
    echo "🧹 Clean build requested"
fi

# Run the build and capture all output
if [ "$CLEAN_BUILD" = true ]; then
    echo "🔨 Clean building all modules..."
    ./gradlew clean build -x test --no-daemon --console=plain > "$TEMP_OUTPUT" 2>&1
    BUILD_STATUS=$?
else
    echo "🔨 Building all modules (incremental)..."
    ./gradlew build -x test --no-daemon --console=plain > "$TEMP_OUTPUT" 2>&1
    BUILD_STATUS=$?
fi

# Show appropriate output based on build result
if [ $BUILD_STATUS -eq 0 ]; then
    echo "✅ Iterable Android SDK build succeeded!"
    echo ""
    echo "📦 Built modules:"
    echo "  • iterableapi (core SDK)"
    echo "  • iterableapi-ui (UI components)"
    echo "  • app (sample app)"
else
    echo "❌ Iterable Android SDK build failed with status $BUILD_STATUS"
    echo ""
    echo "🔍 Build errors:"
    
    # Extract and show compilation errors with file paths and line numbers
    grep -E "\.java:[0-9]+: error:|\.kt:[0-9]+: error:|error:|Error:|FAILURE:|Failed|Exception:" "$TEMP_OUTPUT" | head -20
    
    echo ""
    echo "⚠️  Build warnings:"
    grep -E "\.java:[0-9]+: warning:|\.kt:[0-9]+: warning:|warning:|Warning:" "$TEMP_OUTPUT" | head -10
    
    # If no specific errors found, show the failure section
    if ! grep -q -E "\.java:[0-9]+: error:|\.kt:[0-9]+: error:|error:" "$TEMP_OUTPUT"; then
        echo ""
        echo "📋 Build failure details:"
        grep -A 10 -B 2 "FAILURE\|BUILD FAILED" "$TEMP_OUTPUT" | head -15
    fi
    
    echo ""
    echo "💡 Common solutions:"
    echo "  • Check Java version (JDK 17+ required)"
    echo "  • Verify ANDROID_HOME is set correctly"
    echo "  • Run './gradlew --stop' to kill daemon processes"
    echo "  • Check dependencies in build.gradle files"
fi

exit $BUILD_STATUS 