---
name: android-cli
description: Runs Iterable Android SDK Gradle and emulator checks without IDE-only steps. Use when validating builds in terminals or CI, configuring Android SDK paths, using adb/emulator, or diagnosing unattended Android command failures.
---

# Android CLI

- Use JDK 17 and the checked-in Gradle wrappers; do not require Android Studio.
- Run root tasks with `./gradlew --no-daemon`.
- Run the standalone sample with its wrapper and explicit project directory:

```bash
./sample-apps/inbox-customization/gradlew --no-daemon -p sample-apps/inbox-customization <task>
```

- Prefer `ANDROID_HOME`/`ANDROID_SDK_ROOT`. If Gradle cannot locate the SDK,
  create an uncommitted `local.properties` containing `sdk.dir=<absolute path>`.
- Check tools with `command -v adb`, `command -v emulator`, and
  `command -v sdkmanager`. Do not install or accept licenses without permission.
- Before instrumentation, confirm a booted target with `adb devices` and
  `adb shell getprop sys.boot_completed`.
- Keep commands non-interactive and preserve full failure output. Never report
  an emulator or API level as tested unless that target actually ran.

Use the verification commands in `AGENTS.md` as the repository source of truth.
