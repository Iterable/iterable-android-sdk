#!/bin/bash

# In-App Message Integration Test Runner
# This script helps run in-app message tests locally and provides debugging options

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
API_LEVEL=34
TEST_CLASS="com.iterable.integration.tests.InAppMessageIntegrationTest"
TEST_METHOD=""
VERBOSE=false
CLEAN=false
SCREENSHOTS=false

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -a, --api-level LEVEL    Android API level (default: 34)"
    echo "  -c, --class CLASS        Test class (default: InAppMessageIntegrationTest)"
    echo "  -m, --method METHOD      Specific test method"
    echo "  -v, --verbose            Verbose output"
    echo "  -s, --screenshots        Take screenshots during tests"
    echo "  -C, --clean              Clean before running"
    echo "  -h, --help               Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Run all in-app message tests"
    echo "  $0 -m testInAppMessageMVP            # Run specific test method"
    echo "  $0 -a 29 -v                          # Run on API 29 with verbose output"
    echo "  $0 -s -C                             # Clean, run with screenshots"
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check if Android SDK is available
    if [ -z "$ANDROID_SDK_ROOT" ]; then
        print_error "ANDROID_SDK_ROOT environment variable is not set"
        exit 1
    fi
    
    # Check if adb is available
    if ! command -v adb &> /dev/null; then
        print_error "adb command not found. Please ensure Android SDK platform-tools are in PATH"
        exit 1
    fi
    
    # Check if emulator is available
    if ! command -v emulator &> /dev/null; then
        print_error "emulator command not found. Please ensure Android SDK emulator is in PATH"
        exit 1
    fi
    
    print_success "Prerequisites check passed"
}

# Function to setup emulator
setup_emulator() {
    print_status "Setting up Android emulator for API level $API_LEVEL..."
    
    # Check if AVD exists
    AVD_NAME="inapp_test_api_$API_LEVEL"
    if ! avdmanager list avd | grep -q "$AVD_NAME"; then
        print_status "Creating AVD: $AVD_NAME"
        echo "no" | avdmanager create avd \
            -n "$AVD_NAME" \
            -k "system-images;android-$API_LEVEL;google_apis;x86_64" \
            -c 2048M \
            -f
    else
        print_status "AVD $AVD_NAME already exists"
    fi
    
    # Start emulator
    print_status "Starting emulator..."
    emulator -avd "$AVD_NAME" \
        -no-audio \
        -no-window \
        -no-snapshot \
        -camera-back none \
        -camera-front none \
        -gpu swiftshader_indirect \
        -memory 2048 \
        -cores 2 &
    
    EMULATOR_PID=$!
    print_status "Emulator started with PID: $EMULATOR_PID"
    
    # Wait for emulator to be ready
    print_status "Waiting for emulator to be ready..."
    adb wait-for-device
    
    # Wait for boot completion
    while [ "`adb shell getprop sys.boot_completed`" != "1" ]; do
        print_status "Waiting for boot completion..."
        sleep 5
    done
    
    # Unlock screen
    adb shell input keyevent 82
    adb shell input keyevent 82
    
    # Disable animations for faster testing
    adb shell settings put global window_animation_scale 0
    adb shell settings put global transition_animation_scale 0
    adb shell settings put global animator_duration_scale 0
    
    print_success "Emulator setup complete"
}

# Function to grant permissions
grant_permissions() {
    print_status "Granting permissions..."
    adb shell pm grant com.iterable.integration.tests android.permission.POST_NOTIFICATIONS
    adb shell pm grant com.iterable.integration.tests android.permission.INTERNET
    adb shell pm grant com.iterable.integration.tests android.permission.ACCESS_NETWORK_STATE
    adb shell pm grant com.iterable.integration.tests android.permission.WAKE_LOCK
    print_success "Permissions granted"
}

# Function to run tests
run_tests() {
    print_status "Running in-app message tests..."
    
    # Build test command
    TEST_CMD="./gradlew :integration-tests:connectedCheck"
    
    if [ -n "$TEST_METHOD" ]; then
        TEST_CMD="$TEST_CMD -Pandroid.testInstrumentationRunnerArguments.class=$TEST_CLASS#$TEST_METHOD"
    else
        TEST_CMD="$TEST_CMD -Pandroid.testInstrumentationRunnerArguments.class=$TEST_CLASS"
    fi
    
    if [ "$VERBOSE" = true ]; then
        TEST_CMD="$TEST_CMD --info --stacktrace"
    fi
    
    print_status "Executing: $TEST_CMD"
    
    # Run tests
    if eval $TEST_CMD; then
        print_success "Tests completed successfully"
    else
        print_error "Tests failed"
        return 1
    fi
}

# Function to collect artifacts
collect_artifacts() {
    print_status "Collecting test artifacts..."
    
    # Create artifacts directory
    ARTIFACTS_DIR="integration-tests/artifacts/$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$ARTIFACTS_DIR"
    
    # Copy test reports
    if [ -d "integration-tests/build/reports" ]; then
        cp -r integration-tests/build/reports "$ARTIFACTS_DIR/"
        print_status "Test reports copied to $ARTIFACTS_DIR/reports"
    fi
    
    # Copy test logs
    adb logcat -d > "$ARTIFACTS_DIR/test-logs.txt"
    print_status "Test logs saved to $ARTIFACTS_DIR/test-logs.txt"
    
    # Take screenshot if requested
    if [ "$SCREENSHOTS" = true ]; then
        adb shell screencap -p /sdcard/screenshot.png
        adb pull /sdcard/screenshot.png "$ARTIFACTS_DIR/final-screenshot.png"
        print_status "Screenshot saved to $ARTIFACTS_DIR/final-screenshot.png"
    fi
    
    print_success "Artifacts collected in $ARTIFACTS_DIR"
}

# Function to cleanup
cleanup() {
    print_status "Cleaning up..."
    
    # Kill emulator if it was started by this script
    if [ -n "$EMULATOR_PID" ]; then
        kill $EMULATOR_PID 2>/dev/null || true
        print_status "Emulator stopped"
    fi
    
    # Clean up any running emulators
    adb emu kill 2>/dev/null || true
    
    print_success "Cleanup complete"
}

# Function to handle script interruption
handle_interrupt() {
    print_warning "Script interrupted by user"
    cleanup
    exit 1
}

# Set up signal handlers
trap handle_interrupt INT TERM

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -a|--api-level)
            API_LEVEL="$2"
            shift 2
            ;;
        -c|--class)
            TEST_CLASS="$2"
            shift 2
            ;;
        -m|--method)
            TEST_METHOD="$2"
            shift 2
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -s|--screenshots)
            SCREENSHOTS=true
            shift
            ;;
        -C|--clean)
            CLEAN=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Main execution
main() {
    print_status "Starting In-App Message Integration Test Runner"
    print_status "API Level: $API_LEVEL"
    print_status "Test Class: $TEST_CLASS"
    if [ -n "$TEST_METHOD" ]; then
        print_status "Test Method: $TEST_METHOD"
    fi
    
    # Check prerequisites
    check_prerequisites
    
    # Clean if requested
    if [ "$CLEAN" = true ]; then
        print_status "Cleaning project..."
        ./gradlew clean
    fi
    
    # Setup emulator
    setup_emulator
    
    # Grant permissions
    grant_permissions
    
    # Run tests
    if run_tests; then
        # Collect artifacts on success
        collect_artifacts
        print_success "All tests completed successfully!"
    else
        # Collect artifacts on failure for debugging
        collect_artifacts
        print_error "Tests failed. Check artifacts for debugging information."
        cleanup
        exit 1
    fi
    
    # Cleanup
    cleanup
    print_success "Test run completed successfully!"
}

# Run main function
main "$@"