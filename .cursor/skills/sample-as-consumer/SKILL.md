---
name: sample-as-consumer
description: Keeps Iterable Android sample and test apps as consumers of the published libraries. Use when work touches app, integration-tests, sample-apps, demos, or asks for proof that SDK library behavior integrates correctly.
---

# Sample as consumer

- Implement product behavior in `iterableapi/` or `iterableapi-ui/` first.
- Use `app/`, `integration-tests/`, and `sample-apps/` to exercise public SDK
  behavior. Do not reach into internals or widen library visibility for them.
- Do not invent activities, fragments, views, navigation, or sample UX unless
  the task explicitly targets a sample.
- Keep sample changes minimal and representative of a real integrating app.
- A passing sample does not replace library tests.

Verify the published modules first, then assemble the standalone sample using
the exact commands in `AGENTS.md`. For packaging or shrinking work, assemble
its release variant too.
