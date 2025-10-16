#!/bin/bash

# CI Test Runner for Integration Tests
# Runs any integration test class/method for CI environments (GitHub Actions, etc.)

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Variables
AVD_NAME=""
EMULATOR_PID=""
STARTED_EMULATOR=false

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

show_usage() {
    echo "Usage: $0 <test_class> [test_method] [--avd <avd_name>]"
    echo ""
    echo "Arguments:"
    echo "  test_class    Full test class name (e.g., com.iterable.integration.tests.InAppMessageIntegrationTest)"
    echo "  test_method   Optional: Specific test method name (e.g., testInAppMessageMVP)"
    echo ""
    echo "Options:"
    echo "  --avd <name>  Specific AVD to use (if not already running)"
    echo ""
    echo "Examples:"
    echo "  $0 com.iterable.integration.tests.InAppMessageIntegrationTest"
    echo "  $0 com.iterable.integration.tests.InAppMessageIntegrationTest testInAppMessageMVP"
    echo "  $0 com.iterable.integration.tests.InAppMessageIntegrationTest testInAppMessageMVP --avd Pixel_5_API_34"
}

cleanup() {
    if [ "$STARTED_EMULATOR" = true ] && [ -n "$EMULATOR_PID" ]; then
        print_info "Shutting down emulator (PID: $EMULATOR_PID)..."
        kill $EMULATOR_PID 2>/dev/null || true
        adb emu kill 2>/dev/null || true
    fi
}

trap cleanup EXIT INT TERM

ensure_adb_server() {
    print_info "Ensuring ADB server is ready..."
    
    # Kill any existing adb servers to avoid conflicts
    adb kill-server 2>/dev/null || true
    sleep 1
    
    # Start adb server
    adb start-server
    sleep 2
    
    print_success "ADB server ready"
}

check_device_connected() {
    DEVICES=$(adb devices 2>/dev/null | grep -v "List of devices" | grep "device$" | wc -l)
    if [ "$DEVICES" -gt 0 ]; then
        print_success "Device already connected"
        return 0
    else
        return 1
    fi
}

get_available_avd() {
    print_info "Looking for available AVDs..." >&2
    FIRST_AVD=$(emulator -list-avds | head -n 1)
    
    if [ -z "$FIRST_AVD" ]; then
        print_error "No AVDs found. Please create an AVD first." >&2
        exit 1
    fi
    
    print_info "Found AVD: $FIRST_AVD" >&2
    echo "$FIRST_AVD"
}

start_emulator() {
    local avd_to_start="$1"
    
    print_info "Starting emulator: $avd_to_start"
    
    # Find the correct emulator path
    local emulator_path=""
    
    if [ -n "$ANDROID_SDK_ROOT" ] && [ -f "$ANDROID_SDK_ROOT/emulator/emulator" ]; then
        emulator_path="$ANDROID_SDK_ROOT/emulator/emulator"
        print_info "Using emulator from ANDROID_SDK_ROOT: $emulator_path"
    elif [ -n "$ANDROID_HOME" ] && [ -f "$ANDROID_HOME/emulator/emulator" ]; then
        emulator_path="$ANDROID_HOME/emulator/emulator"
        print_info "Using emulator from ANDROID_HOME: $emulator_path"
    elif command -v emulator &> /dev/null; then
        emulator_path=$(command -v emulator)
        print_info "Using emulator from PATH: $emulator_path"
    else
        print_error "emulator command not found!"
        print_error "Checked: ANDROID_SDK_ROOT/emulator/emulator, ANDROID_HOME/emulator/emulator, PATH"
        exit 1
    fi
    
    # Build emulator command with full path and optimized flags
    local emulator_cmd="$emulator_path -avd $avd_to_start"
    emulator_cmd="$emulator_cmd -no-audio -no-boot-anim"
    emulator_cmd="$emulator_cmd -camera-back none -camera-front none"
    emulator_cmd="$emulator_cmd -memory 2048 -cores 2"
    emulator_cmd="$emulator_cmd -no-metrics -skip-adb-auth"
    emulator_cmd="$emulator_cmd -partition-size 2048"
    
    # Use snapshot if available (way faster boot)
    if [ -n "$CI" ]; then
        emulator_cmd="$emulator_cmd -snapshot default_boot"
    else
        emulator_cmd="$emulator_cmd -no-snapshot"
    fi
    
    # Only run headless in CI environments
    if [ -n "$CI" ]; then
        print_info "CI environment detected, running headless with optimizations..."
        emulator_cmd="$emulator_cmd -no-window -no-skin -gpu swiftshader_indirect"
    else
        print_info "Local environment detected, showing emulator window..."
        emulator_cmd="$emulator_cmd -gpu auto"
    fi
    
    print_info "Emulator command: $emulator_cmd"
    
    # Start emulator in background
    $emulator_cmd > /tmp/emulator.log 2>&1 &
    
    EMULATOR_PID=$!
    STARTED_EMULATOR=true
    
    print_info "Emulator started with PID: $EMULATOR_PID"
    print_info "Emulator logs: /tmp/emulator.log"
}

wait_for_device() {
    print_info "Waiting for device to be ready..."
    adb wait-for-device
    
    print_info "Waiting for boot to complete..."
    local timeout=600
    local elapsed=0
    
    while [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
        if [ $elapsed -ge $timeout ]; then
            print_error "Timeout waiting for device to boot"
            print_error "Last boot status:"
            adb shell getprop | grep boot || true
            exit 1
        fi
        
        sleep 5
        elapsed=$((elapsed + 5))
        
        # Show more detailed status every 30 seconds
        if [ $((elapsed % 30)) -eq 0 ]; then
            local boot_progress=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
            local boot_reason=$(adb shell getprop sys.boot.reason 2>/dev/null | tr -d '\r')
            print_info "Still waiting... (${elapsed}s) [boot_completed=${boot_progress:-'?'}, reason=${boot_reason:-'?'}]"
        else
            print_info "Still waiting... (${elapsed}s)"
        fi
    done
    
    print_success "Device is ready"
    
    # Unlock screen and disable animations
    print_info "Configuring device..."
    adb shell input keyevent 82 || true
    adb shell settings put global window_animation_scale 0 || true
    adb shell settings put global transition_animation_scale 0 || true
    adb shell settings put global animator_duration_scale 0 || true
}

# Detect environment
if [ -n "$CI" ]; then
    print_info "CI environment detected - will run emulator headless"
else
    print_info "Local environment detected - will show emulator window"
fi

# Parse arguments
TEST_CLASS=""
TEST_METHOD=""

while [ $# -gt 0 ]; do
    case "$1" in
        --avd)
            AVD_NAME="$2"
            shift 2
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            if [ -z "$TEST_CLASS" ]; then
                TEST_CLASS="$1"
            elif [ -z "$TEST_METHOD" ]; then
                TEST_METHOD="$1"
            else
                print_error "Unknown argument: $1"
                show_usage
                exit 1
            fi
            shift
            ;;
    esac
done

# Validate required arguments
if [ -z "$TEST_CLASS" ]; then
    print_error "Missing required argument: test_class"
    echo ""
    show_usage
    exit 1
fi

# Build the test target
if [ -n "$TEST_METHOD" ]; then
    TEST_TARGET="${TEST_CLASS}#${TEST_METHOD}"
    print_info "Running test: ${TEST_CLASS}#${TEST_METHOD}"
else
    TEST_TARGET="${TEST_CLASS}"
    print_info "Running all tests in: ${TEST_CLASS}"
fi

# Ensure ADB server is ready before checking devices
ensure_adb_server

# Check if device is already connected
if ! check_device_connected; then
    print_warning "No device connected, starting emulator..."
    
    # Determine which AVD to use
    if [ -z "$AVD_NAME" ]; then
        AVD_NAME=$(get_available_avd)
    fi
    
    # Start the emulator
    start_emulator "$AVD_NAME"
    
    # Wait for it to be ready
    wait_for_device
fi

# Run the test
print_info "Executing Gradle command..."
./gradlew :integration-tests:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class="${TEST_TARGET}" \
    --stacktrace

print_success "Tests completed successfully!"
