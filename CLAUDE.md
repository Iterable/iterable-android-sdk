# Iterable Android SDK

This is the source repository for the Iterable Android SDK.

## SDK Version

To find the latest released version, read `CHANGELOG.md` — the first `## [x.y.z]` entry after `## [Unreleased]` is the latest stable release. Always use this version — never hardcode or assume a version number.

Maven coordinates (use the version from CHANGELOG.md):
- `com.iterable:iterableapi:<version>` — core SDK
- `com.iterable:iterableapi-ui:<version>` — UI components (inbox, in-app)

## For Agents Integrating the SDK

If you are integrating the Iterable Android SDK into an app, **read `docs/ai/integration-guide.md` before writing any code**. It contains step-by-step instructions, critical gotchas, and traps that will save hours of debugging.

## Project Structure

- `iterableapi/` — Core SDK module
- `iterableapi-ui/` — UI module (in-app, inbox, embedded)
- `sample-apps/` — Sample integrations
- `docs/ai/` — AI agent guides
