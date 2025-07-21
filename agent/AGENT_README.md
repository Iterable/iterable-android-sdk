# AGENT README - Iterable Android SDK

## Project Overview
This is the **Iterable Android SDK** for Android app integration. The SDK provides:
- Push notification handling (FCM/GCM)
- In-app messaging 
- Embedded messaging
- Event tracking
- User management
- Mobile Inbox functionality

## Key Architecture
- **Core SDK**: `iterableapi/` - Main SDK implementation
- **UI Components**: `iterableapi-ui/` - In-app and inbox UI components
- **Sample App**: `app/` - Example integration
- **Sample Apps**: `sample-apps/` - Additional example integrations

## Development Workflow

### 🔨 Building the SDK
```bash
# Fast incremental build (default)
./agent/agent_build.sh

# Clean build (slower, but ensures clean state)
./agent/agent_build.sh --clean
```
- Validates compilation for Android
- Uses incremental builds by default for speed
- Shows build errors with context
- Requires Android SDK and Java/Kotlin

### Listing All Available Tests
```bash
# List all available test suites
./agent/agent_test.sh --list
```

### 🧪 Running Tests  
```bash
# Run all tests
./agent/agent_test.sh

# Run specific test class
./agent/agent_test.sh IterableApiTest

# Run specific test method (dot notation - recommended)
./agent/agent_test.sh "IterableKeychainTest.testSaveAndGetEmail"

# Run any specific test with full path 
./agent/agent_test.sh ":iterableapi:testDebugUnitTest --tests com.iterable.iterableapi.IterableApiTest.testSetEmail"
```
- Executes unit tests with accurate pass/fail reporting
- Returns exit code 0 for success, 1 for failures
- Shows detailed test counts and failure information
- `--list` shows all test classes with test counts

## Project Structure
```
iterable-android-sdk/
├── iterableapi/                    # Main SDK module
│   ├── src/main/java/             # Main SDK source
│   │   └── com/iterable/iterableapi/
│   │       ├── IterableApi.java   # Main SDK interface
│   │       ├── IterableConfig.java # Configuration
│   │       ├── IterableInAppManager.java # In-app messaging
│   │       ├── IterableEmbeddedManager.java # Embedded messaging
│   │       └── util/              # Utility classes
│   ├── src/test/java/             # Unit tests
│   └── src/androidTest/java/      # Instrumentation tests
├── iterableapi-ui/                # UI components module
│   ├── src/main/java/             # UI source
│   │   └── com/iterable/iterableapi/ui/
│   │       ├── inbox/             # Mobile Inbox components
│   │       └── embedded/          # Embedded message views
├── app/                           # Sample application
├── sample-apps/                   # Additional examples
│   └── inbox-customization/       # Inbox customization examples
└── tools/                         # Build and development tools
```

## Key Classes
- **IterableApi**: Main SDK interface (singleton)
- **IterableConfig**: Configuration management
- **IterableInAppManager**: In-app message handling
- **IterableEmbeddedManager**: Embedded message handling
- **IterableApiClient**: Network communication
- **IterableAuthManager**: Authentication management

## Common Tasks

### Adding New Features
1. Build first: `./agent/agent_build.sh` (fast incremental)
2. Implement in `iterableapi/src/main/java/com/iterable/iterableapi/`
3. Add tests in `iterableapi/src/test/java/`
4. Verify: `./agent/agent_test.sh` (all tests) or `./agent/agent_test.sh IterableKeychainTest` (specific class)
5. For final verification: `./agent/agent_build.sh --clean` (ensures clean state)

### Debugging Build Issues
- Build script shows compilation errors with file paths
- Check Gradle dependencies in `build.gradle` files
- Verify Android SDK and build tools are installed
- Check for Kotlin/Java compatibility issues

### Test Failures
- Test script shows specific failures with line numbers and detailed error messages
- Run failing tests individually: `./agent/agent_test.sh "TestClass.testMethod"`
- Mock classes available in test directories
- Update method signatures when refactoring APIs

## Requirements
- **Android SDK**: API level 21+ (Android 5.0+)
- **Java**: JDK 17 or newer
- **Gradle**: 8.0+ (managed by wrapper)
- **Android Build Tools**: Latest stable version

## Quick Start for AI Agents
1. Run `./agent/agent_build.sh` to verify project builds (fast incremental)
2. Run `./agent/agent_test.sh` to check test health (or `./agent/agent_test.sh TestClass` for specific class)
3. Make changes to source files
4. Re-run both scripts to validate (incremental builds are fast)
5. Debug failing tests: `./agent/agent_test.sh "IterableKeychainTest.testSaveAndGetEmail"`
6. Before committing: `./agent/agent_build.sh --clean` to ensure clean state
7. Commit when both pass ✅

## Test Filtering Examples
```bash
# Debug specific failing tests
./agent/agent_test.sh "IterableKeychainTest.testSaveAndGetEmail"

# Run a problematic test class
./agent/agent_test.sh IterableInAppManagerTest

# Check auth-related tests
./agent/agent_test.sh IterableAuthManagerTest
```

## AI Agent Memory System

### 🧠 Update Instructions for AI Agents
**IMPORTANT**: When you discover something useful while working on this codebase, update this README to help future AI agents. Add learnings to the sections below.

### 📍 Code Location Map
- **Main API**: `iterableapi/src/main/java/com/iterable/iterableapi/IterableApi.java` (public interface)
- **Auth Logic**: `iterableapi/src/main/java/com/iterable/iterableapi/IterableAuthManager.java` (auth manager)
- **API Client**: `iterableapi/src/main/java/com/iterable/iterableapi/IterableApiClient.java` (network calls)
- **In-App Messages**: `iterableapi/src/main/java/com/iterable/iterableapi/IterableInAppManager.java`
- **Embedded Messages**: `iterableapi/src/main/java/com/iterable/iterableapi/IterableEmbeddedManager.java`
- **Models**: `iterableapi/src/main/java/com/iterable/iterableapi/` (CommerceItem, IterableInAppMessage, etc.)
- **UI Components**: `iterableapi-ui/src/main/java/com/iterable/iterableapi/ui/`
- **Constants**: `iterableapi/src/main/java/com/iterable/iterableapi/IterableConstants.java`

### 🛠️ Common Task Recipes

**Add New API Endpoint:**
1. Add endpoint constant to `IterableConstants.java`
2. Add method to `IterableApiClient.java`
3. Create request method in `IterableRequestTask.java`
4. Add public method to `IterableApi.java`

**Modify Auth Logic:**
- Main logic: `IterableAuthManager.java`
- Token storage: `IterableKeychain.java`
- Auth failures: Handle in API client response processing

**Add New Model:**
- Create in `iterableapi/src/main/java/com/iterable/iterableapi/YourModel.java`
- Implement `Parcelable` if it needs to be passed between components
- Add JSON serialization if it needs to be sent over network

### 🐛 Common Failure Solutions

**"Test X failed"** → Check test file in `iterableapi/src/test/java/` - often method signature mismatches after refactoring

**"Build failed: package not found"** → Check import statements and ensure all dependencies are in `build.gradle`

**"Auth token issues"** → Check `IterableAuthManager.java` and ensure JWT format is correct in tests

**"Network request fails"** → Check endpoint in `IterableConstants.java` and request creation in `IterableRequestTask.java`

**"UI component not found"** → Check if `iterableapi-ui` module is included in your dependencies

### 📱 Android-Specific Notes
- SDK supports Android API level 21+ (Android 5.0+)
- Uses AndroidX libraries for compatibility
- Push notifications require FCM setup
- In-app messages use WebView for rendering
- Embedded messages support custom UI components
- Mobile Inbox requires `iterableapi-ui` dependency

## Notes
- Always test builds after refactoring
- Method signature changes require test file updates
- Gradle sync may be needed after dependency changes
- Sample apps demonstrate SDK usage patterns
- UI components are in separate module for optional inclusion 