# CLAUDE.md - Iterable Android SDK

## Project Overview

The Iterable Android SDK integrates Android apps with [Iterable](https://www.iterable.com), a growth marketing platform. The SDK provides push notifications (FCM), in-app messaging, embedded messaging, mobile inbox, event tracking, deep linking, and user management.

**Current version:** 3.7.0 | **Min SDK:** 21 (Android 5.0+) | **Language:** Java (75%) + Kotlin (25%)

## Quick Reference

### Build & Test

```bash
# Build (incremental, fast)
./build.sh

# Build (clean)
./build.sh --clean

# Run all unit tests
./test.sh

# Run a specific test class
./test.sh IterableApiTest

# Run a specific test method
./test.sh "IterableApiTest.testSetEmail"

# List all available test classes
./test.sh --list
```

The wrapper scripts (`build.sh`, `test.sh`) provide formatted output and error summaries. You can also use Gradle directly:

```bash
./gradlew build -x test                                                    # build
./gradlew :iterableapi:testDebugUnitTest --tests "*IterableApiTest*"       # test
```

### Requirements

- JDK 17+
- Android SDK with compileSdk 34
- Gradle 8.0+ (use the wrapper: `./gradlew`)

## Project Structure

```
iterableapi/              # Core SDK module (main deliverable)
iterableapi-ui/           # UI components module (inbox, embedded views)
app/                      # Internal test application for SDK integration testing
sample-apps/              # Example apps demonstrating SDK usage (inbox-customization)
integration-tests/        # End-to-end integration tests (requires emulator)
tools/                    # CI/CD utilities (emulator wait script)
```

## Module Details

### iterableapi (core SDK)
- **Source:** `iterableapi/src/main/java/com/iterable/iterableapi/`
- **Tests:** `iterableapi/src/test/java/com/iterable/iterableapi/`
- ~94 source files, ~48 test files
- Mostly Java with Kotlin for newer features (embedded messaging, encryption, keychain)

### iterableapi-ui (UI components)
- **Source:** `iterableapi-ui/src/main/java/com/iterable/iterableapi/ui/`
- Inbox UI (Java) and Embedded views (Kotlin)
- No unit tests in this module

## Key Classes

| Class | Purpose |
|-------|---------|
| `IterableApi.java` | Main SDK interface (singleton entry point) |
| `IterableConfig.java` | SDK configuration builder |
| `IterableApiClient.java` | Network communication / API calls |
| `IterableAuthManager.java` | JWT authentication management |
| `IterableKeychain.kt` | Secure token/credential storage |
| `IterableInAppManager.java` | In-app message lifecycle management |
| `IterableEmbeddedManager.kt` | Embedded message management |
| `IterableConstants.java` | API endpoint paths and constants |
| `IterableRequestTask.java` | Network request execution |
| `IterableFirebaseMessagingService.java` | FCM push notification handling |
| `IterableDeeplinkManager.java` | Deep link resolution |
| `IterableNotificationHelper.java` | Push notification display |

## Common Development Tasks

### Adding a new API endpoint
1. Add endpoint path constant to `IterableConstants.java`
2. Add request method to `IterableApiClient.java`
3. Add public-facing method to `IterableApi.java`
4. Add tests in `iterableapi/src/test/java/`

### Modifying authentication
- Auth flow: `IterableAuthManager.java`
- Token storage: `IterableKeychain.kt`
- Auth failure handling: `AuthFailure.java`, `AuthFailureReason.java`

### Adding a new model class
- Create in `iterableapi/src/main/java/com/iterable/iterableapi/`
- Implement `Parcelable` if passed between components
- Add JSON serialization for network transport

## Code Style

- Checkstyle enforced (see `checkstyle.xml`)
- No star imports
- Max method length: 200 lines
- Standard Java naming conventions (camelCase methods/vars, PascalCase types)
- New features tend to use Kotlin; existing Java code stays Java unless refactored

## CI/CD

GitHub Actions workflows in `.github/workflows/`:
- `build.yml` - Main build and test
- `integration-tests.yml` - Full integration test suite
- `inapp-e2e-tests.yml` - E2E tests for in-app messaging
- `codeql.yml` - Code quality analysis
- `prepare-release.yml` / `validate-release.yml` / `publish.yml` - Release pipeline

## Testing Notes

- Unit tests use Robolectric for Android framework simulation
- Network tests use OkHttp MockWebServer
- Mocking via Mockito (core + inline)
- JSON assertions via JSONAssert
- Test base class: `BaseTest.java`
- Test utilities: `IterableTestUtils.java`, `InAppTestUtils.java`, `EmbeddedTestUtils.java`
