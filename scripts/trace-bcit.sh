#!/usr/bin/env bash
# trace-bcit.sh — End-to-end path tracing harness for SDK-170.
#
# Captures 10 checkpoints along the path:
#   ① emulator subprocess
#   ② host→guest network bring-up
#   ③ guest network state (ConnectivityService, netd, wifi)
#   ④ DNS reachability of api.iterable.com
#   ⑤ TCP/TLS reachability of api.iterable.com:443
#   ⑥ build-time inputs (BuildConfig.ITERABLE_API_KEY length only — never the value)
#   ⑦ SDK init order
#   ⑧ SDK HTTP request lifecycle (logcat)
#   ⑨ test outcome (junit reports)
#   ⑩ device screenshot at end
#
# Designed to run both locally (macOS) and inside a GitHub Actions step
# wrapped by ReactiveCircus/android-emulator-runner. When the env var
# TRACE_MODE=ci-script is set, the script assumes the emulator is already
# running (the action launched it) and skips its own emulator lifecycle.
#
# All output is written to a single timestamped folder under TRACE_OUT_ROOT
# (default: /tmp). Nothing is written outside the repo or that folder.
#
# Exit codes:
#   0  — full trace completed (test may have passed or failed; see 08-summary.md)
#   2  — fatal setup error (e.g. emulator wouldn't boot, can't write artifacts)

set -uo pipefail

# ------------------------------------------------------------------------------
# Config
# ------------------------------------------------------------------------------

readonly REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly TRACE_MODE="${TRACE_MODE:-local}"            # local | ci-script
readonly API_LEVEL="${API_LEVEL:-34}"
readonly TARGET="${TARGET:-google_apis}"
readonly ARCH="${ARCH:-arm64-v8a}"                    # arm64-v8a locally, x86_64 in CI
readonly AVD_NAME="${AVD_NAME:-bcit_trace_avd}"
readonly EMULATOR_PORT="${EMULATOR_PORT:-5554}"
readonly EMULATOR_SERIAL="emulator-${EMULATOR_PORT}"
readonly TEST_CLASS="com.iterable.integration.tests.InAppMessageIntegrationTest#testInAppMessageMVP"
readonly TRACE_OUT_ROOT="${TRACE_OUT_ROOT:-/tmp}"
readonly TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
readonly TRACE_DIR="${TRACE_OUT_ROOT}/sdk-170-trace-${TIMESTAMP}"

# Probe targets
readonly TARGET_HOST="api.iterable.com"
readonly TARGET_PORT="443"

# ------------------------------------------------------------------------------
# Logging
# ------------------------------------------------------------------------------

log()  { printf '\033[1;34m[trace]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[warn]\033[0m  %s\n' "$*" >&2; }
err()  { printf '\033[1;31m[err]\033[0m   %s\n' "$*" >&2; }
die()  { err "$*"; exit 2; }

run_step() {
  # run_step <step-name> <output-file> -- <cmd...>
  # Records the command, captures stdout+stderr, never aborts the harness.
  local name="$1"; shift
  local out="$1"; shift
  [[ "$1" == "--" ]] && shift
  printf '# %s\n# cmd: %s\n# t: %s\n\n' "$name" "$*" "$(date -u +%FT%TZ)" > "$out"
  if "$@" >> "$out" 2>&1; then
    log "  ✓ ${name}"
  else
    warn "  ✗ ${name} (exit $?). See ${out#"${TRACE_DIR}/"}"
  fi
}

adb_shell() {
  "$ADB" -s "${EMULATOR_SERIAL}" shell "$@"
}

# ------------------------------------------------------------------------------
# Tool resolution
# ------------------------------------------------------------------------------

find_tools() {
  local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-${HOME}/Library/Android/sdk}}"
  [[ -d "$sdk" ]] || die "ANDROID_HOME not set and ${sdk} doesn't exist"

  EMULATOR="${sdk}/emulator/emulator"
  ADB="${sdk}/platform-tools/adb"
  AVDMANAGER="${sdk}/cmdline-tools/latest/bin/avdmanager"
  SDKMANAGER="${sdk}/cmdline-tools/latest/bin/sdkmanager"

  for tool in "$EMULATOR" "$ADB"; do
    [[ -x "$tool" ]] || die "missing tool: $tool"
  done
  if [[ "$TRACE_MODE" == "local" ]]; then
    [[ -x "$AVDMANAGER" ]] || warn "avdmanager not at $AVDMANAGER (local mode needs it)"
    [[ -x "$SDKMANAGER" ]] || warn "sdkmanager not at $SDKMANAGER (local mode needs it)"
  fi

  log "ANDROID_HOME = $sdk"
  log "emulator     = $("$EMULATOR" -version 2>&1 | head -1)"
  log "adb          = $("$ADB" version | head -1)"
}

# ------------------------------------------------------------------------------
# Step 0 — environment snapshot
# ------------------------------------------------------------------------------

step_00_env() {
  log "Step 0 — environment snapshot"
  local out="${TRACE_DIR}/00-env.txt"
  {
    echo "# Trace mode: $TRACE_MODE"
    echo "# Trace dir:  $TRACE_DIR"
    echo "# Timestamp:  $TIMESTAMP"
    echo
    echo "## Host"
    echo "uname -a:    $(uname -a)"
    if command -v sw_vers >/dev/null 2>&1; then
      echo "sw_vers:     $(sw_vers -productName) $(sw_vers -productVersion)"
    fi
    echo "arch:        $(uname -m)"
    echo "user:        ${USER:-?}"
    echo "shell:       ${SHELL:-?}"
    echo
    echo "## Android SDK"
    echo "ANDROID_HOME=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-${HOME}/Library/Android/sdk}}"
    "$EMULATOR" -version 2>&1 | head -2
    "$ADB" version | head -2
    echo
    echo "## Trace target"
    echo "API_LEVEL=$API_LEVEL TARGET=$TARGET ARCH=$ARCH"
    echo "AVD=$AVD_NAME PORT=$EMULATOR_PORT"
    echo "TEST_CLASS=$TEST_CLASS"
    echo
    echo "## CI variables (presence only)"
    for v in GITHUB_ACTIONS GITHUB_RUN_ID GITHUB_SHA RUNNER_OS RUNNER_ARCH; do
      eval "val=\${$v:-}"
      printf '%s=%s\n' "$v" "${val:+SET}"
    done
    echo
    echo "## Iterable secrets (presence + length only)"
    for v in ITERABLE_API_KEY ITERABLE_SERVER_API_KEY ITERABLE_TEST_USER_EMAIL; do
      eval "val=\${$v:-}"
      printf '%-32s present=%s length=%s\n' "$v" "$([[ -n "$val" ]] && echo yes || echo no)" "${#val}"
    done
  } > "$out"
}

# ------------------------------------------------------------------------------
# Step 1 — emulator launch (local only; in CI the action handles it)
# ------------------------------------------------------------------------------

ensure_system_image() {
  local pkg="system-images;android-${API_LEVEL};${TARGET};${ARCH}"
  if "$SDKMANAGER" --list_installed 2>/dev/null | grep -q "$pkg"; then
    log "system image already installed: $pkg"
    return 0
  fi
  log "installing system image: $pkg (this may take a few minutes)"
  yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true
  "$SDKMANAGER" "$pkg" 2>&1 | tail -5 || die "failed to install $pkg"
}

ensure_avd() {
  if "$EMULATOR" -list-avds 2>/dev/null | grep -qx "$AVD_NAME"; then
    log "AVD already exists: $AVD_NAME (deleting for clean baseline)"
    "$AVDMANAGER" delete avd -n "$AVD_NAME" >/dev/null 2>&1 || true
  fi
  local pkg="system-images;android-${API_LEVEL};${TARGET};${ARCH}"
  log "creating AVD: $AVD_NAME ($pkg, profile=pixel_6)"
  echo "no" | "$AVDMANAGER" create avd \
    -n "$AVD_NAME" \
    -k "$pkg" \
    -d "pixel_6" \
    --force >/dev/null
  # Match CI sizing: ram-size 3072M, heap-size 576M (set in config.ini).
  local cfg="${HOME}/.android/avd/${AVD_NAME}.avd/config.ini"
  if [[ -f "$cfg" ]]; then
    {
      echo "hw.ramSize=3072"
      echo "vm.heapSize=576"
      echo "disk.dataPartition.size=6000M"
      echo "hw.keyboard=yes"
    } >> "$cfg"
  fi
}

step_01_launch_emulator() {
  if [[ "$TRACE_MODE" != "local" ]]; then
    log "Step 1 — emulator already running (CI mode)"
    return 0
  fi
  log "Step 1 — launching emulator"
  ensure_system_image
  ensure_avd
  local emu_log="${TRACE_DIR}/01-emulator.log"
  # CI flags: -no-window -no-snapshot -gpu swiftshader_indirect -no-boot-anim -camera-back none -partition-size 6000
  nohup "$EMULATOR" \
    -avd "$AVD_NAME" \
    -port "$EMULATOR_PORT" \
    -no-window \
    -no-snapshot \
    -no-boot-anim \
    -gpu swiftshader_indirect \
    -camera-back none \
    -partition-size 6000 \
    -no-audio \
    >"$emu_log" 2>&1 &
  EMU_PID=$!
  echo "$EMU_PID" > "${TRACE_DIR}/.emulator.pid"
  log "  emulator PID=$EMU_PID, port=$EMULATOR_PORT, log=01-emulator.log"
  trap 'cleanup' EXIT
}

cleanup() {
  if [[ -f "${TRACE_DIR}/.emulator.pid" ]]; then
    local pid
    pid="$(cat "${TRACE_DIR}/.emulator.pid")"
    log "stopping emulator PID=$pid"
    "$ADB" -s "${EMULATOR_SERIAL}" emu kill 2>/dev/null || true
    sleep 2
    kill "$pid" 2>/dev/null || true
  fi
}

# ------------------------------------------------------------------------------
# Step 2 — boot
# ------------------------------------------------------------------------------

step_02_boot() {
  log "Step 2 — wait for boot"
  mkdir -p "${TRACE_DIR}/02-boot"
  local boot_log="${TRACE_DIR}/02-boot/poll.txt"
  : > "$boot_log"

  log "  waiting for adb to detect device (timeout 90s)..."
  local t=0
  while ! "$ADB" devices | grep -q "${EMULATOR_SERIAL}.*device"; do
    sleep 2; t=$((t+2))
    [[ $t -ge 90 ]] && { warn "  device not detected after 90s"; "$ADB" devices >> "$boot_log"; return 1; }
  done

  log "  device detected, polling sys.boot_completed (timeout 300s)..."
  t=0
  while :; do
    local boot trace
    boot="$(adb_shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    trace="$(adb_shell getprop dev.bootcomplete 2>/dev/null | tr -d '\r')"
    printf 't=%ds sys.boot_completed=%q dev.bootcomplete=%q\n' "$t" "$boot" "$trace" >> "$boot_log"
    [[ "$boot" == "1" ]] && break
    sleep 3; t=$((t+3))
    if [[ $t -ge 300 ]]; then
      warn "  boot timeout after 300s"
      adb_shell logcat -d -t 200 > "${TRACE_DIR}/02-boot/logcat-during-boot.txt" 2>&1 || true
      return 1
    fi
  done

  log "  booted at t=${t}s"
  echo "boot_seconds=${t}" >> "$boot_log"
  # Match CI: disable animations
  for k in window_animation_scale transition_animation_scale animator_duration_scale; do
    adb_shell settings put global "$k" 0 || true
  done

  # SDK-170: sys.boot_completed=1 is NOT a sufficient signal — Wi-Fi association
  # via the virtio-wifi/netsim stack lags boot by 20-90s. Poll for an active
  # default network and reachable internet before we declare the emulator ready.
  log "  waiting for default network (timeout 180s)..."
  local n=0
  local default_net="none"
  while [[ "$default_net" == "none" ]]; do
    default_net="$(adb_shell 'dumpsys connectivity | grep -E "^Active default network" | head -1' 2>/dev/null | sed 's/.*: //;s/[[:space:]]*$//' | tr -d '\r')"
    printf 't=%ds active_default_network=%q\n' "$n" "$default_net" >> "$boot_log"
    [[ "$default_net" != "none" && -n "$default_net" ]] && break
    sleep 3; n=$((n+3))
    if [[ $n -ge 180 ]]; then
      warn "  default network never came up within 180s"
      echo "default_network_timeout=true" >> "$boot_log"
      break
    fi
  done
  log "  default network up at t=${n}s after boot (default=${default_net})"
  echo "default_network_seconds=${n}" >> "$boot_log"

  log "  waiting for internet reachability (ping 8.8.8.8 succeeds; timeout 60s)..."
  local p=0
  while ! adb_shell 'ping -c 1 -W 2 8.8.8.8 >/dev/null 2>&1'; do
    sleep 2; p=$((p+2))
    if [[ $p -ge 60 ]]; then
      warn "  internet unreachable after 60s of polling"
      echo "internet_timeout=true" >> "$boot_log"
      return 0
    fi
  done
  log "  internet reachable at t=${p}s after default network"
  echo "internet_seconds=${p}" >> "$boot_log"
  return 0
}

# ------------------------------------------------------------------------------
# Step 3 — guest network state
# ------------------------------------------------------------------------------

step_03_guest_network() {
  log "Step 3 — guest network state"
  mkdir -p "${TRACE_DIR}/03-network-guest"
  local d="${TRACE_DIR}/03-network-guest"

  run_step "ifconfig"        "$d/ifconfig.txt"     -- adb_shell ifconfig
  run_step "ip-route"        "$d/ip-route.txt"     -- adb_shell ip route
  run_step "ip-addr"         "$d/ip-addr.txt"      -- adb_shell ip addr
  run_step "getprop-net"     "$d/getprop-net.txt"  -- adb_shell "getprop | grep -E 'net\\.|wifi\\.'"
  run_step "connectivity"    "$d/connectivity.txt" -- adb_shell dumpsys connectivity
  run_step "wifi"            "$d/wifi.txt"         -- adb_shell dumpsys wifi
  run_step "netd-resolver"   "$d/netd.txt"         -- adb_shell ndc resolver dump
  run_step "init.svc.netd"   "$d/init-netd.txt"    -- adb_shell getprop init.svc.netd
}

# ------------------------------------------------------------------------------
# Step 4 — DNS + Step 5 — TCP/TLS reachability of api.iterable.com
# ------------------------------------------------------------------------------

step_04_dns_tcp() {
  log "Step 4+5 — DNS / TCP / TLS reachability of ${TARGET_HOST}"
  mkdir -p "${TRACE_DIR}/04-dns-tcp"
  local d="${TRACE_DIR}/04-dns-tcp"

  run_step "ping-iterable" "$d/ping.txt"     -- adb_shell ping -c 5 -W 3 "$TARGET_HOST"
  run_step "ping-8.8.8.8"  "$d/ping-dns.txt" -- adb_shell ping -c 3 -W 3 8.8.8.8
  run_step "nslookup"      "$d/nslookup.txt" -- adb_shell nslookup "$TARGET_HOST"
  run_step "getent-hosts"  "$d/getent.txt"   -- adb_shell getent hosts "$TARGET_HOST"

  # Java-level probe via a tiny inline script that uses the SDK's classloader.
  # We skip that here and rely on the test step (07/08) to surface in-app HTTP.
  # Instead, run a host-side curl so we know whether the network exit is reachable
  # from the runner (CI baseline).
  run_step "host-curl-iterable" "$d/host-curl.txt" -- bash -c \
    "curl -sS -o /dev/null -w 'http=%{http_code}\\ntcp=%{time_connect}\\ntls=%{time_appconnect}\\ntotal=%{time_total}\\nremoteip=%{remote_ip}\\n' --max-time 10 'https://${TARGET_HOST}/api/inApp/getMessages?email=trace@iterable.com'"
}

# ------------------------------------------------------------------------------
# Step 6 — build with diagnostics
# ------------------------------------------------------------------------------

step_06_build() {
  log "Step 6 — build APK + capture BuildConfig (key length only)"
  mkdir -p "${TRACE_DIR}/05-apk"
  local d="${TRACE_DIR}/05-apk"

  pushd "$REPO_ROOT" >/dev/null
  run_step "gradle-version"    "$d/gradle-version.txt"     -- ./gradlew --version
  run_step "gradle-printBuildConfig" "$d/printBuildConfig.txt" -- \
    ./gradlew :integration-tests:printBuildConfig -q
  run_step "gradle-assemble"   "$d/gradle-assemble.txt"    -- \
    ./gradlew :integration-tests:assembleDebug :integration-tests:assembleDebugAndroidTest --no-daemon
  popd >/dev/null

  # APK introspection
  local apk
  apk="$(ls -t "$REPO_ROOT"/integration-tests/build/outputs/apk/debug/*.apk 2>/dev/null | head -1 || true)"
  if [[ -n "$apk" ]]; then
    cp "$apk" "$d/installed.apk" 2>/dev/null || true
    if command -v aapt2 >/dev/null 2>&1; then
      run_step "aapt2-dump" "$d/aapt2-dump.txt" -- aapt2 dump badging "$apk"
    elif command -v aapt >/dev/null 2>&1; then
      run_step "aapt-dump" "$d/aapt-dump.txt" -- aapt dump badging "$apk"
    fi
  else
    warn "  APK not found after build"
  fi
}

# ------------------------------------------------------------------------------
# Step 7+8+9+10 — install, run test, capture
# ------------------------------------------------------------------------------

step_07_install_run() {
  log "Step 7-9 — install + run instrumented test"
  mkdir -p "${TRACE_DIR}/06-test-run/junit-reports"
  mkdir -p "${TRACE_DIR}/06-test-run/screenshots"
  mkdir -p "${TRACE_DIR}/07-logcat"
  local d="${TRACE_DIR}/06-test-run"

  # Start logcat in background
  local logcat_pid_file="${TRACE_DIR}/.logcat.pid"
  "$ADB" -s "${EMULATOR_SERIAL}" logcat -c >/dev/null 2>&1 || true
  "$ADB" -s "${EMULATOR_SERIAL}" logcat > "${TRACE_DIR}/07-logcat/full.txt" 2>&1 &
  echo $! > "$logcat_pid_file"

  # Pre-test screenshot
  adb_shell screencap -p /sdcard/pre-test.png >/dev/null 2>&1 || true
  "$ADB" -s "${EMULATOR_SERIAL}" pull /sdcard/pre-test.png "$d/screenshots/pre-test.png" >/dev/null 2>&1 || true

  pushd "$REPO_ROOT" >/dev/null
  log "  running test: ${TEST_CLASS}"
  ./gradlew :integration-tests:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class="${TEST_CLASS}" \
    --stacktrace --no-daemon \
    > "$d/gradle.log" 2>&1
  local rc=$?
  echo "$rc" > "$d/gradle.exit-code"
  log "  test gradle exit code: $rc"
  popd >/dev/null

  # Post-test screenshot
  adb_shell screencap -p /sdcard/post-test.png >/dev/null 2>&1 || true
  "$ADB" -s "${EMULATOR_SERIAL}" pull /sdcard/post-test.png "$d/screenshots/post-test.png" >/dev/null 2>&1 || true

  # Grab the UiAutomator hierarchy dump if the test left one behind (SDK-170 diag)
  "$ADB" -s "${EMULATOR_SERIAL}" pull /sdcard/Download/uiautomator-after-launch.xml \
    "$d/uiautomator-after-launch.xml" >/dev/null 2>&1 || true

  # Also dump current window hierarchy for post-mortem
  adb_shell uiautomator dump /sdcard/post-test-hierarchy.xml >/dev/null 2>&1 || true
  "$ADB" -s "${EMULATOR_SERIAL}" pull /sdcard/post-test-hierarchy.xml \
    "$d/post-test-hierarchy.xml" >/dev/null 2>&1 || true

  # Stop logcat
  if [[ -f "$logcat_pid_file" ]]; then
    kill "$(cat "$logcat_pid_file")" 2>/dev/null || true
  fi

  # Copy junit reports
  cp -R "$REPO_ROOT/integration-tests/build/reports/androidTests/connected/." \
        "$d/junit-reports/" 2>/dev/null || true

  # Filtered logcats
  grep -E 'IterableApi|IterableRequest|IterableInApp|IterableEmbedded|IntegrationMainActivity|BaseIntegrationTest' \
    "${TRACE_DIR}/07-logcat/full.txt" > "${TRACE_DIR}/07-logcat/iterable.txt" 2>/dev/null || true
  grep -E 'AndroidRuntime|FATAL|ANR|SIGSEGV|System\.err' \
    "${TRACE_DIR}/07-logcat/full.txt" > "${TRACE_DIR}/07-logcat/crashes.txt" 2>/dev/null || true

  return 0
}

# ------------------------------------------------------------------------------
# Step 11 — summary
# ------------------------------------------------------------------------------

# helper: print first non-empty grep match or "—"
peek() { grep -m1 -E "$1" "$2" 2>/dev/null | sed 's/[[:space:]]\+$//' || echo "—"; }

step_99_summary() {
  log "Writing 08-summary.md"
  local s="${TRACE_DIR}/08-summary.md"
  local d_net="${TRACE_DIR}/03-network-guest"
  local d_dns="${TRACE_DIR}/04-dns-tcp"
  local d_apk="${TRACE_DIR}/05-apk"
  local d_test="${TRACE_DIR}/06-test-run"
  local d_log="${TRACE_DIR}/07-logcat"

  local boot_seconds default_net_seconds internet_seconds
  boot_seconds="$(grep '^boot_seconds=' "${TRACE_DIR}/02-boot/poll.txt" 2>/dev/null | cut -d= -f2 || echo '?')"
  default_net_seconds="$(grep '^default_network_seconds=' "${TRACE_DIR}/02-boot/poll.txt" 2>/dev/null | cut -d= -f2 || echo '?')"
  internet_seconds="$(grep '^internet_seconds=' "${TRACE_DIR}/02-boot/poll.txt" 2>/dev/null | cut -d= -f2 || echo '?')"
  local key_len
  key_len="$(grep -E 'ITERABLE_API_KEY' "$d_apk/printBuildConfig.txt" 2>/dev/null | head -1 || echo '—')"
  local ping_loss
  ping_loss="$(grep -Eo '[0-9]+% packet loss' "$d_dns/ping.txt" 2>/dev/null | head -1 || echo '—')"
  local host_http
  host_http="$(grep -E '^http=' "$d_dns/host-curl.txt" 2>/dev/null | head -1 || echo '—')"
  local test_rc
  test_rc="$(cat "$d_test/gradle.exit-code" 2>/dev/null || echo '?')"

  {
    echo "# SDK-170 trace summary"
    echo
    echo "- timestamp: \`${TIMESTAMP}\`"
    echo "- mode: \`${TRACE_MODE}\`"
    echo "- arch: \`$(uname -m)\` API \`${API_LEVEL}\` ${TARGET}/${ARCH}"
    echo
    echo "## Checkpoints"
    echo
    echo "| # | Checkpoint                                  | Result |"
    echo "|---|---------------------------------------------|--------|"
    echo "| ① | emulator launched                           | $([[ -s "${TRACE_DIR}/01-emulator.log" || "$TRACE_MODE" == ci-script ]] && echo PASS || echo FAIL) |"
    echo "| ② | boot completed (sys.boot_completed=1)       | ${boot_seconds}s |"
    echo "| ② | default network up (after boot)             | +${default_net_seconds}s |"
    echo "| ② | internet reachable (ping 8.8.8.8)            | +${internet_seconds}s |"
    echo "| ③ | wlan0 has IP                                | $(peek '^[[:space:]]*inet ' "$d_net/ifconfig.txt") |"
    echo "| ③ | default route                                | $(peek 'default' "$d_net/ip-route.txt") |"
    echo "| ③ | net.dns1                                    | $(peek '^net\\.dns1' "$d_net/getprop-net.txt") |"
    echo "| ④ | DNS api.iterable.com                        | $(peek 'address|server can' "$d_dns/nslookup.txt") |"
    echo "| ⑤ | ping api.iterable.com loss                  | ${ping_loss} |"
    echo "| ⑤ | host curl https://api.iterable.com          | ${host_http} |"
    echo "| ⑥ | BuildConfig key                             | ${key_len} |"
    echo "| ⑦ | SDK init in logcat                          | $([[ -s "$d_log/iterable.txt" ]] && grep -m1 -c 'Iterable SDK initialized' "$d_log/iterable.txt" || echo 0) hits |"
    echo "| ⑧ | iterable HTTP req in logcat                 | $([[ -s "$d_log/iterable.txt" ]] && grep -c -E 'Sending request|response code' "$d_log/iterable.txt" || echo 0) hits |"
    echo "| ⑨ | gradle test exit code                       | ${test_rc} |"
    echo "| ⑩ | post-test screenshot                        | $([[ -f "$d_test/screenshots/post-test.png" ]] && echo present || echo missing) |"
    echo
    echo "## Top crashes (if any)"
    echo
    echo '```'
    head -30 "$d_log/crashes.txt" 2>/dev/null || echo "(none)"
    echo '```'
    echo
    echo "## Files"
    echo
    find "$TRACE_DIR" -maxdepth 2 -type f | sed "s|${TRACE_DIR}/||" | sort
  } > "$s"
  log "summary written: $s"
}

# ------------------------------------------------------------------------------
# Main
# ------------------------------------------------------------------------------

main() {
  mkdir -p "$TRACE_DIR" || die "cannot create $TRACE_DIR"
  log "Trace output: $TRACE_DIR"
  find_tools
  step_00_env
  step_01_launch_emulator || die "emulator launch failed"
  step_02_boot             || warn "boot incomplete; continuing for partial trace"
  step_03_guest_network
  step_04_dns_tcp
  step_06_build
  step_07_install_run
  step_99_summary
  log "Done. Trace: $TRACE_DIR"
  log "  open ${TRACE_DIR}/08-summary.md"
}

main "$@"
