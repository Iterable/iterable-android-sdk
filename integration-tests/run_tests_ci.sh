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
    # Take final screenshot before shutdown (use output redirection)
    print_info "Taking final screenshot..."
    if adb shell screencap -p > /tmp/test-screenshots/screenshot_final.png 2>/dev/null; then
        print_success "Final screenshot captured"
    else
        print_warning "Could not capture final screenshot"
    fi
    
    # Stop screen recording and pull video
    if [ -n "$SCREENRECORD_PID" ]; then
        print_info "Stopping screen recording (PID: $SCREENRECORD_PID)..."
        
        # Kill screenrecord on device to properly finalize the video
        # Background adb shell processes need to be killed on device, not just the local PID
        adb shell "pkill -INT screenrecord" 2>/dev/null || true
        
        # Also try killing the local process
        kill -INT $SCREENRECORD_PID 2>/dev/null || true
        
        # Wait longer for file finalization
        print_info "Waiting for video finalization..."
        sleep 8
        
        # Pull the recording from /data/local/tmp (no permission issues)
        if adb pull /data/local/tmp/test-recording.mp4 /tmp/test-screenshots/ 2>/dev/null; then
            # Verify the file is playable
            local filesize=$(stat -f%z /tmp/test-screenshots/test-recording.mp4 2>/dev/null || echo "0")
            if [ "$filesize" -gt 1000 ]; then
                print_success "Screen recording saved to /tmp/test-screenshots/test-recording.mp4 (${filesize} bytes)"
            else
                print_warning "Screen recording file seems too small (${filesize} bytes)"
            fi
        else
            print_warning "Could not retrieve screen recording"
            # Debug: check if file exists on device
            adb shell ls -lh /data/local/tmp/test-recording.mp4 2>/dev/null || true
        fi
    fi
    
    # Capture logcat for debugging
    if [ -n "$CI" ]; then
        print_info "Capturing final logcat..."
        adb logcat -d > /tmp/test-screenshots/test-logcat.txt 2>/dev/null || true
    fi
    
    # List captured artifacts with better formatting
    print_info "Captured artifacts:"
    if [ -d "/tmp/test-screenshots" ] && [ "$(ls -A /tmp/test-screenshots 2>/dev/null)" ]; then
        ls -lh /tmp/test-screenshots/
        echo ""
        print_success "Artifacts ready for upload"
    else
        echo "  (no artifacts captured)"
    fi
    
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
    emulator_cmd="$emulator_cmd -feature -Bluetooth"  # Disable Bluetooth entirely
    
    # Increase resources to prevent ANRs (especially important for API 34)
    if [ -n "$CI" ]; then
        # CI has more resources available
        emulator_cmd="$emulator_cmd -memory 4096 -cores 4"
    else
        # Local - use moderate resources
        emulator_cmd="$emulator_cmd -memory 3072 -cores 3"
    fi
    
    emulator_cmd="$emulator_cmd -no-metrics -skip-adb-auth"
    emulator_cmd="$emulator_cmd -partition-size 4096"
    
    # Configure DNS to use Google's public DNS (fixes "Unable to resolve host" errors)
    emulator_cmd="$emulator_cmd -dns-server 8.8.8.8,8.8.4.4"
    
    # Force network to use user-mode networking with no delay
    emulator_cmd="$emulator_cmd -netdelay none -netspeed full"
    
    # TEMPORARY: Disable snapshots to debug network issues
    # Snapshots don't properly restore network state in GitHub Actions
    print_warning "Snapshots disabled temporarily - doing full boot to ensure network works"
    emulator_cmd="$emulator_cmd -no-snapshot-load"
    
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
    local timeout=900
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
    
    # Extra wait for system services to fully start (package manager, etc.)
    print_info "Waiting for system services to fully start..."
    sleep 30
    
    # Verify package manager is ready
    local pm_ready=0
    for i in {1..30}; do
        if adb shell pm list packages >/dev/null 2>&1; then
            pm_ready=1
            break
        fi
        sleep 2
    done
    
    if [ $pm_ready -eq 0 ]; then
        print_error "Package manager not ready after waiting"
        exit 1
    fi
    
    print_success "System services ready"
    
    # Unlock screen and disable animations
    print_info "Configuring device..."
    adb shell input keyevent 82 || true
    adb shell settings put global window_animation_scale 0 || true
    adb shell settings put global transition_animation_scale 0 || true
    adb shell settings put global animator_duration_scale 0 || true
    
    # Disable Bluetooth to prevent crashes (not needed for tests)
    print_info "Disabling Bluetooth..."
    adb shell svc bluetooth disable 2>/dev/null || true
    adb shell cmd bluetooth_manager disable 2>/dev/null || true
    
    # Stop Bluetooth service if it's causing issues
    adb shell "am force-stop com.android.bluetooth" 2>/dev/null || true
    
    # NOTE: We're NOT disabling Google Play Services as it might manage network connectivity
    # Even though it uses resources, networking is critical for API calls
    
    # CRITICAL: Disable system ANR dialogs completely
    print_info "Disabling system ANR dialogs..."
    adb shell settings put global anr_show_background false
    adb shell settings put secure anr_show_background false
    adb shell settings put global show_first_crash_dialog 0
    adb shell settings put global show_restart_in_crash_dialog 0
    adb shell settings put system show_error_dialogs 0
    
    # Increase ANR thresholds (default is 5000ms for input, 10000ms for broadcast)
    adb shell "setprop dalvik.vm.execution-mode int:fast" 2>/dev/null || true
    adb shell "setprop debug.choreographer.skipwarning 1" 2>/dev/null || true
    adb shell settings put global activity_manager_constants max_phantom_processes=2147483647
    
    # Wait for network to initialize (critical for API calls)
    print_info "Waiting for network stack to initialize..."
    
    # Explicitly start the emulator's network service
    print_info "Starting ranchu-net service..."
    adb shell "start ranchu-net" 2>/dev/null || true
    sleep 5
    
    # Bring up eth0 interface
    print_info "Bringing up network interface..."
    adb shell "ifconfig eth0 up" 2>/dev/null || true
    adb shell "ip link set eth0 up" 2>/dev/null || true
    sleep 5
    
    # Try to get DHCP lease
    print_info "Requesting DHCP..."
    adb shell "dhcpclient eth0" 2>/dev/null || adb shell "dhcpcd eth0" 2>/dev/null || true
    sleep 5
    
    # Check and log network interfaces
    print_info "Checking network interfaces..."
    adb shell "ip addr show" 2>/dev/null || true
    adb shell "getprop | grep -E 'net\.|dhcp'" 2>/dev/null || true
    
    # Check if wlan0 has an IP address, if not try to get one
    if ! adb shell "ip addr show wlan0" 2>/dev/null | grep -q "inet "; then
        print_warning "wlan0 is UP but has no IP address, requesting DHCP lease..."
        adb shell "dhcptool wlan0" 2>/dev/null || true
        sleep 3
        adb shell "ip addr show wlan0" 2>/dev/null || true
    fi
    
    # Manually set DNS servers if not set
    print_info "Configuring DNS servers..."
    adb shell "ndc resolver setnetdns 1 localdomain 8.8.8.8 8.8.4.4" 2>/dev/null || true
    adb shell "setprop net.dns1 8.8.8.8" 2>/dev/null || true
    adb shell "setprop net.dns2 8.8.4.4" 2>/dev/null || true
    sleep 2
    
    # Test network connectivity and DNS resolution
    print_info "Testing network connectivity..."
    local network_ok=false
    for i in {1..15}; do
        if adb shell "ping -c 1 -W 3 8.8.8.8" &>/dev/null; then
            print_success "Network connectivity OK (attempt $i)"
            network_ok=true
            break
        fi
        print_warning "Network test attempt $i failed, retrying..."
        sleep 3
    done
    
    if [ "$network_ok" = false ]; then
        print_error "Network connectivity test failed after 15 attempts"
        print_error "Trying to restart network services..."
        adb shell "svc wifi enable" 2>/dev/null || true
        adb shell "svc data enable" 2>/dev/null || true
        sleep 5
        
        # One more retry after restart
        if adb shell "ping -c 1 -W 3 8.8.8.8" &>/dev/null; then
            print_success "Network OK after service restart"
            network_ok=true
        fi
    fi
    
    # Test DNS resolution
    if [ "$network_ok" = true ]; then
        print_info "Testing DNS resolution for api.iterable.com..."
        local dns_ok=false
        for i in {1..10}; do
            if adb shell "ping -c 1 -W 5 api.iterable.com" &>/dev/null; then
                print_success "DNS resolution OK - api.iterable.com is reachable (attempt $i)"
                dns_ok=true
                break
            fi
            print_warning "DNS test attempt $i failed, retrying..."
            sleep 2
        done
        
        if [ "$dns_ok" = false ]; then
            print_warning "DNS resolution test failed - trying alternative check..."
            # Try using nslookup as fallback
            if adb shell "nslookup api.iterable.com 8.8.8.8" 2>/dev/null | grep -q "Address"; then
                print_success "DNS resolution OK via nslookup"
            else
                print_error "DNS resolution FAILED - API calls will likely fail"
                print_error "This is a critical issue that will cause test failures"
            fi
        fi
    else
        print_error "Skipping DNS test - no basic network connectivity"
    fi
    
    # Create screenshots directory
    mkdir -p /tmp/test-screenshots
    
    # Take initial screenshot (use output redirection to avoid permission issues)
    print_info "Taking initial screenshot..."
    if adb shell screencap -p > /tmp/test-screenshots/screenshot_initial.png 2>/dev/null; then
        print_success "Initial screenshot captured"
    else
        print_warning "Could not capture initial screenshot (non-critical)"
    fi
}

start_screen_recording() {
    # Start screen recording for debugging (local and CI)
    print_info "Starting screen recording..."
    mkdir -p /tmp/test-screenshots
    
    # Use /data/local/tmp instead of /sdcard to avoid permission issues on API 34+
    adb shell screenrecord --verbose --time-limit 900 /data/local/tmp/test-recording.mp4 &
    SCREENRECORD_PID=$!
    sleep 3  # Let recording initialize properly
    print_success "Screen recording started (PID: $SCREENRECORD_PID)"
}

# Detect environment
if [ -n "$CI" ]; then
    print_info "CI environment detected - will run emulator headless"
else
    print_info "Local environment detected - will show emulator window"
fi

# Setup local.properties with credentials (for CI)
setup_local_properties() {
    print_info "Setting up local.properties for CI environment..."
    
    # Determine project root (this script is in integration-tests/)
    local project_root="$(cd "$(dirname "$0")/.." && pwd)"
    local local_props="${project_root}/local.properties"
    
    # Read from BCIT_ prefixed secrets (GitHub Actions) or fallback to non-prefixed
    local api_key="${BCIT_ITERABLE_API_KEY:-$ITERABLE_API_KEY}"
    local server_api_key="${BCIT_ITERABLE_SERVER_API_KEY:-$ITERABLE_SERVER_API_KEY}"
    local test_user_email="${BCIT_ITERABLE_TEST_USER_EMAIL:-$ITERABLE_TEST_USER_EMAIL}"
    
    # Check if we need to add Iterable API keys
    if [ -n "$api_key" ] || [ -n "$server_api_key" ] || [ -n "$test_user_email" ]; then
        print_info "Updating local.properties with Iterable API credentials from environment..."
        
        # Create or update local.properties
        if [ ! -f "$local_props" ]; then
            print_info "Creating new local.properties file..."
            touch "$local_props"
        fi
        
        # Remove existing Iterable keys if present
        grep -v "ITERABLE_API_KEY" "$local_props" > "${local_props}.tmp" 2>/dev/null || touch "${local_props}.tmp"
        grep -v "ITERABLE_SERVER_API_KEY" "${local_props}.tmp" > "${local_props}.tmp2" 2>/dev/null || touch "${local_props}.tmp2"
        grep -v "ITERABLE_TEST_USER_EMAIL" "${local_props}.tmp2" > "${local_props}.tmp3" 2>/dev/null || touch "${local_props}.tmp3"
        mv "${local_props}.tmp3" "$local_props"
        rm -f "${local_props}.tmp" "${local_props}.tmp2"
        
        # Add Iterable API credentials section if not present
        if ! grep -q "# Iterable API Keys" "$local_props"; then
            echo "" >> "$local_props"
            echo "# Iterable API Keys for Integration Tests" >> "$local_props"
        fi
        
        # Helper function to obfuscate value (show first 2 and last 2 chars)
        obfuscate_value() {
            local val="$1"
            local len=${#val}
            if [ $len -le 4 ]; then
                echo "****"
            else
                local first="${val:0:2}"
                local last="${val: -2}"
                echo "${first}****${last}"
            fi
        }
        
        # Add the credentials (write to local.properties without BCIT_ prefix)
        if [ -n "$api_key" ]; then
            echo "ITERABLE_API_KEY=$api_key" >> "$local_props"
            print_success "Added ITERABLE_API_KEY=$(obfuscate_value "$api_key") to local.properties (from BCIT_ITERABLE_API_KEY)"
        fi
        
        if [ -n "$server_api_key" ]; then
            echo "ITERABLE_SERVER_API_KEY=$server_api_key" >> "$local_props"
            print_success "Added ITERABLE_SERVER_API_KEY=$(obfuscate_value "$server_api_key") to local.properties (from BCIT_ITERABLE_SERVER_API_KEY)"
        fi
        
        if [ -n "$test_user_email" ]; then
            echo "ITERABLE_TEST_USER_EMAIL=$test_user_email" >> "$local_props"
            print_success "Added ITERABLE_TEST_USER_EMAIL=$(obfuscate_value "$test_user_email") to local.properties (from BCIT_ITERABLE_TEST_USER_EMAIL)"
        fi
        
        print_success "local.properties updated successfully"
    else
        print_info "No Iterable credentials in environment variables, will use defaults from build.gradle"
    fi
}

# Setup local.properties if in CI
if [ -n "$CI" ]; then
    setup_local_properties
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

# Print local properties with obfuscated values
print_local_properties() {
    # Helper function to obfuscate value (show first 2 and last 2 chars)
    obfuscate_display() {
        local val="$1"
        local len=${#val}
        if [ $len -le 4 ]; then
            echo "****"
        else
            local first="${val:0:2}"
            local last="${val: -2}"
            echo "${first}****${last}"
        fi
    }
    
    # Check multiple possible locations for local.properties
    local properties_files=("../local.properties" "./local.properties" "$(pwd)/local.properties" "$(pwd)/../local.properties")
    local found=false
    local properties_file=""
    
    # Find the first existing properties file
    for file in "${properties_files[@]}"; do
        if [ -f "$file" ]; then
            properties_file="$file"
            found=true
            break
        fi
    done
    
    print_info "Local properties file check:"
    
    if [ "$found" = true ]; then
        print_info "Found local.properties at: $properties_file"
        print_info "Contents (obfuscated):"
        echo "----------------------------------------"
        while IFS= read -r line || [ -n "$line" ]; do
            # Skip empty lines
            if [ -z "$line" ]; then
                echo "$line"
            # Handle commented lines that contain key=value
            elif [[ "$line" =~ ^[[:space:]]*#.*= ]]; then
                # Obfuscate values in commented lines too
                if [[ "$line" =~ ^([[:space:]]*#[^=]+)=(.+) ]]; then
                    prefix="${BASH_REMATCH[1]}"
                    value="${BASH_REMATCH[2]}"
                    echo "$prefix=$(obfuscate_display "$value")"
                else
                    echo "$line"
                fi
            # Handle regular comments without key=value
            elif [[ "$line" =~ ^[[:space:]]*# ]]; then
                echo "$line"
            # Handle active key=value lines
            elif [[ "$line" =~ ^([^=]+)=(.*)$ ]]; then
                key="${BASH_REMATCH[1]}"
                value="${BASH_REMATCH[2]}"
                echo "$key=$(obfuscate_display "$value")"
            else
                # Any other line format, print as is
                echo "$line"
            fi
        done < "$properties_file"
        echo "----------------------------------------"
        
        # Check if sdk.dir exists in the file and verify the directory
        if grep -q "sdk.dir" "$properties_file"; then
            sdk_dir=$(grep "sdk.dir" "$properties_file" | cut -d'=' -f2)
            if [ ! -d "$sdk_dir" ]; then
                print_warning "WARNING: sdk.dir in local.properties points to non-existent directory: $sdk_dir"
            else
                print_info "sdk.dir directory exists: $sdk_dir"
            fi
        else
            print_warning "No sdk.dir property found in local.properties"
        fi
    else
        print_warning "No local.properties file found. Checked:"
        for file in "${properties_files[@]}"; do
            print_warning "  - $file"
        done
        
        # Check environment variables as fallback
        if [ -n "$ANDROID_SDK_ROOT" ]; then
            print_info "Using ANDROID_SDK_ROOT environment variable: $ANDROID_SDK_ROOT"
        elif [ -n "$ANDROID_HOME" ]; then
            print_info "Using ANDROID_HOME environment variable: $ANDROID_HOME"
        else
            print_warning "No Android SDK location found in environment variables"
        fi
    fi
}

# Ensure ADB server is ready before checking devices
ensure_adb_server

# Print local properties with obfuscated values
print_local_properties

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

# Build the test APKs first (before recording starts)
print_info "Building test APKs..."
./gradlew :integration-tests:assembleDebug :integration-tests:assembleDebugAndroidTest --no-daemon

# Start screen recording right before test execution
start_screen_recording

# Run the test (APKs are already built, Gradle will skip build and go straight to install+run)
print_info "Installing and running test..."
TEST_EXIT_CODE=0
./gradlew :integration-tests:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class="${TEST_TARGET}" \
    --no-daemon \
    --stacktrace || TEST_EXIT_CODE=$?

if [ $TEST_EXIT_CODE -eq 0 ]; then
    print_success "Tests completed successfully!"
else
    print_error "Tests failed with exit code: $TEST_EXIT_CODE"
    print_info "Cleanup will still run to capture artifacts..."
    exit $TEST_EXIT_CODE
fi
