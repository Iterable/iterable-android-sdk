# Iterable Android SDK agent guide

## Purpose and priorities

This repository ships the Iterable Android SDK. Default work targets the
published libraries, their public API, Gradle configuration, and tests. Treat
apps as consumers used to validate the libraries; do not add sample screens or
sample-only behavior unless the task explicitly targets a sample.

## Repository map

- `iterableapi/`: published core library (`com.iterable:iterableapi`)
- `iterableapi-ui/`: published UI library (`com.iterable:iterableapi-ui`)
- `app/`: root test app and library consumer
- `integration-tests/`: BCIT application and instrumentation suite
- `sample-apps/inbox-customization/`: standalone consumer build with its own
  Gradle wrapper
- `docs/ai/`: integration guidance; read `docs/ai/integration-guide.md` before
  writing consumer integration code

## Public API versus internal implementation

- Assume public/protected Java and Kotlin declarations in published modules are
  consumer API unless clearly constrained by visibility.
- Also treat Android resources, manifests, dependency exposure (`api` versus
  `implementation`), callbacks, nullability, threading, and persisted/wire
  formats as compatibility-sensitive contracts.
- Prefer package-private, private, or module-internal implementation details.
  Do not expose a type merely to make a test or sample convenient.
- Before changing a public signature or behavior, identify Java and Kotlin
  source/binary compatibility impact and add consumer-facing tests.
- This repository currently has no API dump or binary-compatibility Gradle
  task. Do not claim an API check ran. If tooling is added, commit its baseline
  and make its check/update commands explicit here.

## Required local verification

Use JDK 17. Run commands from the repository root.

Library unit tests:

```bash
./gradlew --no-daemon :iterableapi:testDebugUnitTest :iterableapi-ui:testDebugUnitTest
```

Standalone sample consumer:

```bash
./sample-apps/inbox-customization/gradlew --no-daemon -p sample-apps/inbox-customization :app:assembleDebug
```

CI-equivalent static checks:

```bash
./gradlew --no-daemon :iterableapi:lintDebug :iterableapi:checkstyle :iterableapi-ui:assembleDebug
```

Root consumer tests used by CI:

```bash
./gradlew --no-daemon :app:testDebugUnitTest
```

Instrumentation requires an emulator and is not part of every local edit:

```bash
./gradlew --no-daemon :iterableapi:connectedCheck
```

For AGP, manifest, packaging, keep-rule, reflection, or shrinking changes, also
exercise the minified standalone consumer:

```bash
./sample-apps/inbox-customization/gradlew --no-daemon -p sample-apps/inbox-customization :app:assembleRelease
```

## Definition of done

At minimum, complete the library unit-test command and standalone sample
assemble command. Run the static checks and root consumer tests when their
scope is affected. Run the API check when one exists. Record any skipped check
and the concrete reason; a sample-only implementation is not a substitute for
library coverage.

When a root `TASK.md` exists, it is the current ticket contract. Read it before
work, keep its status and verification evidence current, and update it before
summarizing or handing off.
