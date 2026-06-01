#!/usr/bin/env bash
# .github/scripts/run-e2e.sh
#
# Runs the BCIT integration-test suite under ReactiveCircus/android-emulator-runner
# and captures diagnostics that survive the action's emulator-kill on exit.
#
# Why this is an external script and not inline YAML:
#   The action runs each line of `script:` in a fresh `/bin/sh -c`, so cross-line
#   variables and shell functions don't survive. We need a single bash process for
#   the trap + variable + function semantics.
#
# Inputs (env, all set by the workflow step):
#   ITERABLE_API_KEY            — set as buildConfigField at runtime; not echoed.
#   ITERABLE_SERVER_API_KEY     — set as buildConfigField at runtime; not echoed.
#   ITERABLE_TEST_USER_EMAIL    — used by tests; not echoed (length only).
#   GITHUB_WORKSPACE            — set by the runner; root for diagnostics output.
#
# Outputs:
#   $GITHUB_WORKSPACE/integration-tests/build/diagnostics/
#     hierarchy.xml    — UiAutomator dump at the moment of test exit
#     screenshot.png   — device screenshot at the moment of test exit
#     logcat.txt       — full device logcat from start of test invocation
#
# Exit code:
#   The gradle test task's exit code, propagated.
#
# This script writes nothing outside $GITHUB_WORKSPACE/integration-tests/build/.

set -uo pipefail

readonly DIAG_DIR="${GITHUB_WORKSPACE:?GITHUB_WORKSPACE must be set}/integration-tests/build/diagnostics"
readonly TEST_PACKAGE="com.iterable.integration.tests"

mkdir -p "$DIAG_DIR"

log()  { printf '\033[1;34m[e2e]\033[0m %s\n' "$*"; }

log "Running BCIT integration-test suite: package=$TEST_PACKAGE"
log "Diagnostics will be written to: $DIAG_DIR"

# Sanity-check env: don't echo secret values, only their lengths. The workflow's
# env: block guarantees these vars exist; ${#VAR} of an empty string is 0.
log "ITERABLE_API_KEY length:        ${#ITERABLE_API_KEY}"
log "ITERABLE_SERVER_API_KEY length: ${#ITERABLE_SERVER_API_KEY}"
log "ITERABLE_TEST_USER_EMAIL length: ${#ITERABLE_TEST_USER_EMAIL}"

# Fail fast if a BCIT_* secret didn't resolve. Without this, an empty secret falls
# through to the gradle default 'test_api_key' (integration-tests/build.gradle) and
# the suite fails later with opaque 401s instead of a clear configuration error.
if [[ "${CI:-false}" == "true" ]]; then
  if [[ -z "${ITERABLE_API_KEY:-}" || -z "${ITERABLE_SERVER_API_KEY:-}" || -z "${ITERABLE_TEST_USER_EMAIL:-}" ]]; then
    log "::error::One or more BCIT_* secrets are empty. Configure BCIT_ITERABLE_API_KEY, BCIT_ITERABLE_SERVER_API_KEY, BCIT_ITERABLE_TEST_USER_EMAIL on this branch/repo."
    exit 1
  fi
fi

# Grant permissions; ignore failures (the package may not be installed yet,
# in which case AGP will install + auto-grant during the test step).
for perm in POST_NOTIFICATIONS INTERNET ACCESS_NETWORK_STATE WAKE_LOCK; do
  adb shell pm grant "$TEST_PACKAGE" "android.permission.$perm" >/dev/null 2>&1 || true
done

# Stream full logcat to the workspace so the artifact upload always has it.
adb logcat -c >/dev/null 2>&1 || true
adb logcat > "$DIAG_DIR/logcat.txt" &
LOGCAT_PID=$!

# Capture diagnostics that depend on a live emulator. Called from EXIT trap so
# we always run, whether tests passed, failed, or the runner timed out.
#
# SDK-170: every adb call here is wrapped with `timeout` so an unresponsive
# emulator (e.g. on test failure) can't make the diagnostic capture itself
# hang the 6-hour job timeout — which would replace the useful gradle
# failure output with an opaque cancelled-job. 10s per command is generous
# (uiautomator dump usually finishes in <2s on a healthy device).
ADB_TIMEOUT="${ADB_TIMEOUT:-10}"

capture_post_test() {
  log "Capturing post-test diagnostics..."

  # Stop logcat first so the file isn't being appended to mid-copy.
  if [[ -n "${LOGCAT_PID:-}" ]]; then
    kill "$LOGCAT_PID" 2>/dev/null || true
    wait "$LOGCAT_PID" 2>/dev/null || true
  fi

  # UiAutomator hierarchy — answers "what was UiAutomator looking at?"
  if timeout "$ADB_TIMEOUT" adb shell uiautomator dump /sdcard/hierarchy.xml >/dev/null 2>&1; then
    timeout "$ADB_TIMEOUT" adb pull /sdcard/hierarchy.xml "$DIAG_DIR/hierarchy.xml" >/dev/null 2>&1 || true
    timeout "$ADB_TIMEOUT" adb shell rm -f /sdcard/hierarchy.xml >/dev/null 2>&1 || true
  else
    log "uiautomator dump unavailable (emulator unresponsive or no device)"
  fi

  # Screenshot — answers "what was actually on the screen?"
  if timeout "$ADB_TIMEOUT" adb shell screencap -p /sdcard/screenshot.png >/dev/null 2>&1; then
    timeout "$ADB_TIMEOUT" adb pull /sdcard/screenshot.png "$DIAG_DIR/screenshot.png" >/dev/null 2>&1 || true
    timeout "$ADB_TIMEOUT" adb shell rm -f /sdcard/screenshot.png >/dev/null 2>&1 || true
  else
    log "screencap unavailable (emulator unresponsive or no device)"
  fi

  log "Diagnostics captured:"
  ls -la "$DIAG_DIR" || true
}
trap capture_post_test EXIT

# Run the test. Don't `set -e`; we want to capture diagnostics on failure and
# propagate the original exit code at the end.
gradle_exit=0
./gradlew :integration-tests:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package="$TEST_PACKAGE" \
  --stacktrace --no-daemon || gradle_exit=$?

if [[ "$gradle_exit" -ne 0 ]]; then
  log "::error::Gradle test task failed with exit code $gradle_exit — see bcit-integration-diagnostics artifact"
fi

# capture_post_test runs via EXIT trap; just propagate the exit code.
exit "$gradle_exit"
