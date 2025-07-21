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
    echo "  ./agent/agent_test.sh IterableApiTest"
    echo "  ./agent/agent_test.sh \"IterableApiTest.testSetEmail\""
    echo "  ./agent/agent_test.sh \"iterableapi:testDebugUnitTest --tests IterableApiTest\""
    
    exit 0
fi

echo "Running Iterable Android SDK tests..."

# Create a temporary file for the test output
TEMP_OUTPUT=$(mktemp)

# Function to clean up temp file on exit
cleanup() {
    rm -f "$TEMP_OUTPUT"
}
trap cleanup EXIT

# Build the gradle command
if [[ -n "$FILTER" ]]; then
    # If filter contains a colon, use it as-is (already in module:task format)
    if [[ "$FILTER" == *":"* ]]; then
        GRADLE_CMD="./gradlew $FILTER --no-daemon --console=plain"
    # If filter contains a dot, convert TestClass.testMethod to --tests format
    elif [[ "$FILTER" == *"."* ]]; then
        GRADLE_CMD="./gradlew iterableapi:testDebugUnitTest --tests \"$FILTER\" --no-daemon --console=plain"
    # Otherwise, assume it's just a test class name
    else
        GRADLE_CMD="./gradlew iterableapi:testDebugUnitTest --tests \"$FILTER\" --no-daemon --console=plain"
    fi
else
    # Run all tests
    GRADLE_CMD="./gradlew test --no-daemon --console=plain"
fi

echo "🧪 Running: $GRADLE_CMD"

# Run the tests and capture all output
eval $GRADLE_CMD > "$TEMP_OUTPUT" 2>&1

# Check the exit status
TEST_STATUS=$?

# Parse test results from Gradle output
TOTAL_TESTS=0
FAILED_TESTS=0
PASSED_TESTS=0
SKIPPED_TESTS=0

# Extract test summary from output
while IFS= read -r line; do
    if [[ "$line" =~ ([0-9]+)\ tests\ completed,\ ([0-9]+)\ failed,\ ([0-9]+)\ skipped ]]; then
        TOTAL_TESTS=${BASH_REMATCH[1]}
        FAILED_TESTS=${BASH_REMATCH[2]}
        SKIPPED_TESTS=${BASH_REMATCH[3]}
        PASSED_TESTS=$(($TOTAL_TESTS - $FAILED_TESTS - $SKIPPED_TESTS))
        break
    elif [[ "$line" =~ ([0-9]+)\ tests\ completed ]]; then
        TOTAL_TESTS=${BASH_REMATCH[1]}
        FAILED_TESTS=0
        SKIPPED_TESTS=0
        PASSED_TESTS=$TOTAL_TESTS
        break
    fi
done < "$TEMP_OUTPUT"

# Show test results
if [ "$FAILED_TESTS" -eq 0 ] && [ "$TOTAL_TESTS" -gt 0 ]; then
    echo "✅ All tests passed! ($TOTAL_TESTS tests, $SKIPPED_TESTS skipped)"
    FINAL_STATUS=0
elif [ "$FAILED_TESTS" -gt 0 ]; then
    echo "❌ Tests failed: $FAILED_TESTS failed, $PASSED_TESTS passed, $SKIPPED_TESTS skipped ($TOTAL_TESTS total)"
    echo ""
    echo "🔍 Test failures:"
    grep -A 5 -B 2 "FAILED\|FAILURE" "$TEMP_OUTPUT" | head -20
    FINAL_STATUS=1
elif [ "$TEST_STATUS" -ne 0 ]; then
    echo "❌ Test execution failed with status $TEST_STATUS"
    echo ""
    echo "🔍 Error details:"
    grep -E "error:|Error:|FAILURE:|Failed|Exception:" "$TEMP_OUTPUT" | head -10
    FINAL_STATUS=$TEST_STATUS
else
    echo "⚠️  No test results found"
    FINAL_STATUS=$TEST_STATUS
fi

exit $FINAL_STATUS 