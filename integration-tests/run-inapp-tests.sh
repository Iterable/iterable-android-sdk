#!/bin/bash

# In-App Integration Tests Runner Script
# This script provides easy access to run in-app functionality tests

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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
    echo "  -h, --help              Show this help message"
    echo "  -a, --all               Run all in-app tests"
    echo "  -s, --silent-push       Run silent push test only"
    echo "  -d, --display           Run in-app display test only"
    echo "  -m, --metrics           Run metrics tracking test only"
    echo "  -l, --deep-link         Run deep linking test only"
    echo "  -c, --action-handlers   Run action handlers test only"
    echo "  -e, --end-to-end        Run complete end-to-end test only"
    echo "  -l, --lifecycle         Run message lifecycle test only"
    echo "  --coverage              Run tests with coverage report"
    echo "  --install               Install the app before running tests"
    echo "  --clean                 Clean build before running tests"
    echo ""
    echo "Examples:"
    echo "  $0 -a                    # Run all tests"
    echo "  $0 -s                    # Run silent push test only"
    echo "  $0 --coverage            # Run with coverage"
    echo "  $0 --install --clean     # Clean, install, and run all tests"
}

# Function to check if we're in the right directory
check_directory() {
    if [ ! -f "build.gradle" ] || [ ! -d "src" ]; then
        print_error "This script must be run from the integration-tests directory"
        print_error "Current directory: $(pwd)"
        exit 1
    fi
}

# Function to clean build
clean_build() {
    print_status "Cleaning build..."
    ./gradlew clean
    print_success "Build cleaned successfully"
}

# Function to install app
install_app() {
    print_status "Installing app..."
    ./gradlew installDebug
    print_success "App installed successfully"
}

# Function to run specific test
run_test() {
    local test_name="$1"
    local test_class="$2"
    
    print_status "Running test: $test_name"
    print_status "Test class: $test_class"
    
    ./gradlew connectedCheck -Pandroid.testInstrumentationRunnerArguments.class="$test_class"
    
    if [ $? -eq 0 ]; then
        print_success "Test '$test_name' completed successfully"
    else
        print_error "Test '$test_name' failed"
        exit 1
    fi
}

# Function to run all tests
run_all_tests() {
    print_status "Running all in-app integration tests..."
    
    local test_class="com.iterable.integration.tests.InAppMessageIntegrationTest"
    
    ./gradlew connectedCheck -Pandroid.testInstrumentationRunnerArguments.class="$test_class"
    
    if [ $? -eq 0 ]; then
        print_success "All in-app tests completed successfully"
    else
        print_error "Some in-app tests failed"
        exit 1
    fi
}

# Function to run tests with coverage
run_with_coverage() {
    print_status "Running tests with coverage..."
    
    ./gradlew jacocoIntegrationTestReport
    
    if [ $? -eq 0 ]; then
        print_success "Coverage report generated successfully"
        print_status "Coverage report location: build/reports/jacoco/integrationTest/html/index.html"
    else
        print_error "Failed to generate coverage report"
        exit 1
    fi
}

# Main script logic
main() {
    # Check if we're in the right directory
    check_directory
    
    # Parse command line arguments
    local run_all=false
    local run_coverage=false
    local install_app_flag=false
    local clean_build_flag=false
    local specific_test=""
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_usage
                exit 0
                ;;
            -a|--all)
                run_all=true
                shift
                ;;
            -s|--silent-push)
                specific_test="testSilentPushFunctionality"
                shift
                ;;
            -d|--display)
                specific_test="testInAppMessageDisplay"
                shift
                ;;
            -m|--metrics)
                specific_test="testInAppMetricsTracking"
                shift
                ;;
            -l|--deep-link)
                specific_test="testInAppDeepLinking"
                shift
                ;;
            -c|--action-handlers)
                specific_test="testInAppActionHandlers"
                shift
                ;;
            -e|--end-to-end)
                specific_test="testCompleteInAppEndToEndFlow"
                shift
                ;;
            --lifecycle)
                specific_test="testInAppMessageLifecycle"
                shift
                ;;
            --coverage)
                run_coverage=true
                shift
                ;;
            --install)
                install_app_flag=true
                shift
                ;;
            --clean)
                clean_build_flag=true
                shift
                ;;
            *)
                print_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    # If no specific test is specified, run all
    if [ -z "$specific_test" ] && [ "$run_all" = false ]; then
        run_all=true
    fi
    
    # Execute requested actions
    if [ "$clean_build_flag" = true ]; then
        clean_build
    fi
    
    if [ "$install_app_flag" = true ]; then
        install_app
    fi
    
    if [ "$run_coverage" = true ]; then
        run_with_coverage
    elif [ "$run_all" = true ]; then
        run_all_tests
    elif [ -n "$specific_test" ]; then
        local test_class="com.iterable.integration.tests.InAppMessageIntegrationTest#$specific_test"
        run_test "$specific_test" "$test_class"
    fi
    
    print_success "Script execution completed successfully"
}

# Run main function with all arguments
main "$@"
