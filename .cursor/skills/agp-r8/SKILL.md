---
name: agp-r8
description: Validates Android Gradle Plugin, packaging, manifest, dependency, ProGuard, and R8 changes for the Iterable Android libraries and their consumers. Use for Gradle upgrades, minification, reflection, serialization, keep rules, resources, or release-build failures.
---

# AGP and R8

Treat shrinking as a consumer concern: library `minifyEnabled` does not prove
that an app consuming the AAR can shrink successfully.

1. Identify which published AAR, manifest, resource, dependency, or reflected
   type is affected.
2. Prefer narrowly scoped consumer rules only when the SDK requires them.
   Do not add broad `-keep class com.iterable.** { *; }` rules.
3. Verify reflection and serialization entry points and review merged manifest
   or dependency output when relevant.
4. Run the normal library tests.
5. Run both standalone sample builds:

```bash
./sample-apps/inbox-customization/gradlew --no-daemon -p sample-apps/inbox-customization :app:assembleDebug
./sample-apps/inbox-customization/gradlew --no-daemon -p sample-apps/inbox-customization :app:assembleRelease
```

The repository currently documents that no ProGuard/R8 consumer rules are
required. Treat any change to that claim as a public integration change and
update documentation and tests together.
