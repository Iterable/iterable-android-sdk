#!/bin/bash

# This script is to be used by LLMs and AI agents to run tests for the Iterable Android SDK.
# It uses Gradle to run tests and provides clean output with filtering capabilities.

set -e

# Parse command line arguments
FILTER=""
LIST_TESTS=false

if [[ $# -eq 1 ]]; then
    if [[ "$1" == "--list" ]]; then
        LIST_TESTS=true
    else
        FILTER="$1"
        echo "🎯 Running tests with filter: $FILTER"
    fi
elif [[ $# -gt 1 ]]; then
    echo "❌ Usage: $0 [filter|--list]"
    echo "   filter: Test class name (e.g., 'IterableApiTest')"
    echo "           or specific test (e.g., 'IterableApiTest.testSetEmail')"
    echo "           or module:test (e.g., 'iterableapi:testDebugUnitTest --tests IterableApiTest')"
    echo "   --list: List all available test classes"
    exit 1
fi

# Handle test listing
if [[ "$LIST_TESTS" == true ]]; then
    echo "📋 Listing available test classes..."
    
    echo "📦 Available Test Classes:"
    
    # List test classes from iterableapi module
    echo ""
    echo "🔧 iterableapi module:"
    if [[ -d "iterableapi/src/test/java/com/iterable/iterableapi" ]]; then
        find iterableapi/src/test/java/com/iterable/iterableapi -name "*.java" -o -name "*.kt" | while read test_file; do
            test_class=$(basename "$test_file" | sed 's/\.[^.]*$//')
            # Count test methods in each file
            test_count=$(grep -c "fun test\|@Test" "$test_file" 2>/dev/null || echo "0")
            echo "  • $test_class ($test_count tests)"
        done
    fi
    
    # List test classes from iterableapi-ui module
    echo ""
    echo "🎨 iterableapi-ui module:"
    if [[ -d "iterableapi-ui/src/test" ]]; then
        find iterableapi-ui/src/test -name "*.java" -o -name "*.kt" 2>/dev/null | while read test_file; do
            test_class=$(basename "$test_file" | sed 's/\.[^.]*$//')
            test_count=$(grep -c "fun test\|@Test" "$test_file" 2>/dev/null || echo "0")
            echo "  • $test_class ($test_count tests)"
        done
    else
        echo "  (No unit tests found)"
    fi
    
    # List test classes from app module
    echo ""
    echo "📱 app module:"
    if [[ -d "app/src/test" ]]; then
        find app/src/test -name "*.java" -o -name "*.kt" 2>/dev/null | while read test_file; do
            test_class=$(basename "$test_file" | sed 's/\.[^.]*$//')
            test_count=$(grep -c "fun test\|@Test" "$test_file" 2>/dev/null || echo "0")
            echo "  • $test_class ($test_count tests)"
        done
    else
        echo "  (No unit tests found)"
    fi
    
    echo ""
    echo "🔍 Example Usage:"
    echo "  ./agent/test.sh IterableKeychainTest"
    echo "  ./agent/test.sh \"IterableKeychainTest.testSaveAndGetEmail\""
    echo "  ./agent/test.sh \":iterableapi:testDebugUnitTest --tests com.iterable.iterableapi.IterableKeychainTest\""
    
    exit 0
fi

echo "Running Iterable Android SDK tests..."

# Build the gradle command
if [[ -n "$FILTER" ]]; then
    # If filter contains a colon, use it as-is (already in module:task format)
    if [[ "$FILTER" == *":"* ]]; then
        GRADLE_CMD="./gradlew $FILTER --no-daemon --console=plain --rerun-tasks"
    # If filter contains a dot, convert TestClass.testMethod to wildcard format
    elif [[ "$FILTER" == *"."* ]]; then
        GRADLE_CMD="./gradlew :iterableapi:testDebugUnitTest --tests \"*$FILTER*\" --no-daemon --console=plain --rerun-tasks"
    # Otherwise, assume it's just a test class name and use wildcard
    else
        GRADLE_CMD="./gradlew :iterableapi:testDebugUnitTest --tests \"*$FILTER*\" --no-daemon --console=plain --rerun-tasks"
    fi
else
    # Run all tests with detailed test output (force execution to see individual tests)
    GRADLE_CMD="./gradlew :iterableapi:testDebugUnitTest --no-daemon --console=plain --rerun-tasks"
fi

echo "🧪 Running: $GRADLE_CMD"
echo "📊 Real-time progress:"
echo "─────────────────────────────────────────────────────"

# Create a temporary file for capturing output while showing progress
TEMP_OUTPUT=$(mktemp)

# Function to clean up temp file on exit
cleanup() {
    rm -f "$TEMP_OUTPUT"
}
trap cleanup EXIT

# Run the tests with real-time output parsing
eval $GRADLE_CMD 2>&1 | tee "$TEMP_OUTPUT" | while IFS= read -r line; do
    # Pretty print different types of Gradle output
    case "$line" in
        *"> Task "*)
            # Task execution - only show important ones, simplify build tasks
            task_name=$(echo "$line" | sed 's/.*> Task //' | sed 's/ .*//')
            case "$task_name" in
                *"compile"*|*"build"*|*"generate"*|*"process"*|*"merge"*|*"package"*)
                    # Only show building status once per batch of build tasks
                    if [[ ! -f "/tmp/gradle_building_shown" ]]; then
                        echo "🔨 Building..."
                        touch "/tmp/gradle_building_shown"
                    fi
                    ;;
            esac
            ;;
        *"BUILD SUCCESSFUL"*)
            rm -f "/tmp/gradle_building_shown" 2>/dev/null
            echo "✅ Build completed successfully!"
            ;;
        *"BUILD FAILED"*)
            rm -f "/tmp/gradle_building_shown" 2>/dev/null
            echo "❌ Build failed!"
            ;;
        # JUnit test execution patterns
        *"Test"*"started"*|*"Test"*"STARTED"*)
            test_name=$(echo "$line" | sed 's/.*Test //' | sed 's/ started.*//' | sed 's/ STARTED.*//')
            echo "🔄 $test_name"
            ;;
        *"Test"*"passed"*|*"Test"*"PASSED"*)
            test_name=$(echo "$line" | sed 's/.*Test //' | sed 's/ passed.*//' | sed 's/ PASSED.*//')
            echo "✅ $test_name"
            ;;
        *"Test"*"failed"*|*"Test"*"FAILED"*)
            test_name=$(echo "$line" | sed 's/.*Test //' | sed 's/ failed.*//' | sed 's/ FAILED.*//')
            echo "❌ $test_name"
            ;;
        *"Test"*"skipped"*|*"Test"*"SKIPPED"*)
            test_name=$(echo "$line" | sed 's/.*Test //' | sed 's/ skipped.*//' | sed 's/ SKIPPED.*//')
            echo "⏭️ $test_name"
            ;;
        # Gradle test output patterns
        *" > "*)
            if [[ "$line" == *"STARTED"* ]]; then
                test_name=$(echo "$line" | sed 's/.*> //' | sed 's/ STARTED.*//')
                echo "🔄 $test_name"
            elif [[ "$line" == *"PASSED"* ]]; then
                test_name=$(echo "$line" | sed 's/.*> //' | sed 's/ PASSED.*//')
                echo "✅ $test_name"
            elif [[ "$line" == *"FAILED"* ]]; then
                test_name=$(echo "$line" | sed 's/.*> //' | sed 's/ FAILED.*//')
                echo "❌ $test_name"
            elif [[ "$line" == *"SKIPPED"* ]]; then
                test_name=$(echo "$line" | sed 's/.*> //' | sed 's/ SKIPPED.*//')
                echo "⏭️ $test_name"
            fi
            ;;
        # Also catch the full line format for context
        *".* > "*)
            if [[ "$line" == *"STARTED"* ]]; then
                class_and_method=$(echo "$line" | sed 's/ STARTED.*//')
                method_name=$(echo "$class_and_method" | sed 's/.*> //')
                echo "🔄 $method_name"
            elif [[ "$line" == *"PASSED"* ]]; then
                class_and_method=$(echo "$line" | sed 's/ PASSED.*//')
                method_name=$(echo "$class_and_method" | sed 's/.*> //')
                echo "✅ $method_name"
            elif [[ "$line" == *"FAILED"* ]]; then
                class_and_method=$(echo "$line" | sed 's/ FAILED.*//')
                method_name=$(echo "$class_and_method" | sed 's/.*> //')
                echo "❌ $method_name"
            elif [[ "$line" == *"SKIPPED"* ]]; then
                class_and_method=$(echo "$line" | sed 's/ SKIPPED.*//')
                method_name=$(echo "$class_and_method" | sed 's/.*> //')
                echo "⏭️ $method_name"
            fi
            ;;
        # Test summary and counts
        *"tests completed"*|*"test passed"*|*"test failed"*|*"test skipped"*)
            echo "📊 $line"
            ;;
        # Method level test info
        *"testPassed"*|*"testFailed"*|*"testSkipped"*|*"testStarted"*)
            method_name=$(echo "$line" | sed 's/.*testMethod=//' | sed 's/,.*//')
            if [[ "$line" == *"testPassed"* ]]; then
                echo "✅ $method_name"
            elif [[ "$line" == *"testFailed"* ]]; then
                echo "❌ $method_name"
            elif [[ "$line" == *"testSkipped"* ]]; then
                echo "⏭️ $method_name"
            elif [[ "$line" == *"testStarted"* ]]; then
                echo "🔄 $method_name"
            fi
            ;;
        # General failures and errors
        *"FAILED"*|*"ERROR"*|*"Exception"*|*"error"*)
            echo "❌ $line"
            ;;
        # Success indicators for individual tests
        *"PASSED"*)
            if [[ "$line" == *"test"* ]] || [[ "$line" == *"Test"* ]]; then
                echo "✅ $line"
            fi
            ;;
        # Timing for tests only
        *"seconds"*|*"ms"*)
            if [[ "$line" == *"test"* ]] || [[ "$line" == *"Test"* ]]; then
                echo "⏱️  $line"
            fi
            ;;
        *)
            # Catch specific test method patterns
            if [[ "$line" == *"test"* ]] && ([[ "$line" == *"pass"* ]] || [[ "$line" == *"fail"* ]] || [[ "$line" == *"start"* ]] || [[ "$line" == *"complete"* ]]); then
                echo "🧪 $line"
            fi
            ;;
    esac
done

# Get the actual exit status from the temp file
if grep -q "BUILD SUCCESSFUL" "$TEMP_OUTPUT"; then
    echo "─────────────────────────────────────────────────────"
    echo "✅ All tests completed successfully!"
    
    # Try to extract test summary
    if grep -q "tests completed" "$TEMP_OUTPUT"; then
        echo "📊 Test Summary:"
        grep "tests completed\|test.*passed\|test.*failed\|test.*skipped" "$TEMP_OUTPUT" | tail -5
    fi
    
    exit 0
elif grep -q "BUILD FAILED" "$TEMP_OUTPUT"; then
    echo "─────────────────────────────────────────────────────"
    echo "❌ Tests failed!"
    echo ""
    echo "🔍 Failure Summary:"
    
    # Show specific test failures
    grep -A 3 -B 1 "FAILED\|Failed\|failed.*test" "$TEMP_OUTPUT" | head -15
    
    # Show build failures if no test failures
    if ! grep -q "test.*FAILED\|test.*failed" "$TEMP_OUTPUT"; then
        echo ""
        echo "📋 Build Error Details:"
        grep -A 5 -B 2 "BUILD FAILED\|FAILURE:" "$TEMP_OUTPUT" | head -10
    fi
    
    exit 1
else
    echo "─────────────────────────────────────────────────────"
    echo "❌ Test execution failed"
    echo ""
    echo "🔍 Error details:"
    grep -E "error:|Error:|FAILURE:|Failed|Exception:" "$TEMP_OUTPUT" | head -10
    exit 1
fi 