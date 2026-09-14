---
name: public-api
description: Protects the Iterable Android SDK's Java, Kotlin, Android, and Gradle consumer contracts. Use when changing declarations, dependencies, resources, manifests, callbacks, models, persistence, or behavior in iterableapi or iterableapi-ui.
---

# Public API

1. Identify the consumer-visible contract before editing:
   - Java/Kotlin source and binary signatures, visibility, nullability, and
     overloads
   - Android resources and manifest entries
   - `api` dependencies and transitive types
   - callback timing/threading, serialized data, and persisted state
2. Prefer an internal implementation change. Do not widen visibility for tests
   or sample code.
3. Check both Java and Kotlin callers. Pay special attention to default
   arguments, `@JvmOverloads`, SAM interfaces, generics, and platform types.
4. Preserve existing behavior unless the task explicitly changes the contract.
   Add a regression test at the library boundary.
5. Inspect the diff for accidental public declarations or leaked internal
   dependency types.
6. Run the library tests and sample consumer assemble from `AGENTS.md`.

There is currently no API dump or binary-compatibility task. State that
explicitly in verification; do not infer compatibility from a successful
assemble. If API tooling is introduced, run its check and review the dump diff
before declaring the change complete.
