# In-App Messages — Iterable Android SDK

Set up in-app message display and control how messages are shown to users.

> **Prerequisites:** SDK must be initialized with `IterableConfig` that includes an `inAppHandler` (see [integration-guide.md](integration-guide.md)).

---

## Business Context

In-app messages are configured and triggered in Iterable by the marketing team. The SDK fetches them automatically and displays them based on campaign rules. The developer's role is to:

1. Provide a handler that decides whether to show or skip each message
2. Optionally control display timing and pausing during sensitive flows

The actual message content, targeting, and triggers are managed in Iterable's dashboard — not in code.

---

## Decision Points

| Decision | Tier | Default | Notes |
|----------|------|---------|-------|
| Show vs skip logic (`onNewInApp`) | 🟠 Important | Show all | Should messages be suppressed during certain flows (checkout, onboarding, tutorials)? Ask the developer. Default: always show. |
| Display interval between auto-shown messages | 🟡 Relevant | 30 seconds | How aggressive should messaging feel? Lower = more frequent. |
| Pause auto-display on specific screens | 🟡 Relevant | Never paused | Useful for checkout flows, media playback, or forms where interruption is harmful. |
| In-memory vs file storage for messages | 🟢 Optional | File storage | Use in-memory only if privacy policy requires no message content on disk. |

---

## Step 1: InAppHandler

The SDK calls `onNewInApp` for each unread in-app message. Return `SHOW` to display it or `SKIP` to suppress it in this automatic cycle.

> **⚠️ CRITICAL IMPORT:** `InAppResponse` is a nested enum inside `IterableInAppHandler`. You MUST either:
> - Import it explicitly: `import com.iterable.iterableapi.IterableInAppHandler.InAppResponse`
> - Or use the fully qualified name: `IterableInAppHandler.InAppResponse.SHOW`
>
> Using bare `InAppResponse` without the import will cause "Unresolved reference" compile errors.

```kotlin
import com.iterable.iterableapi.IterableInAppHandler
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.IterableInAppHandler.InAppResponse  // ⚠️ REQUIRED — nested enum

class AppInAppHandler : IterableInAppHandler {
    override fun onNewInApp(message: IterableInAppMessage): InAppResponse {
        // 🟠 If the developer wants to suppress messages in certain situations,
        // add logic here. Otherwise, always show.
        return InAppResponse.SHOW
    }
}
```

> **Agent note:** If the developer said they want messages suppressed during checkout or similar flows, implement conditional logic using app state, message metadata (`message.customPayload`), or a shared flag. Example:
> ```kotlin
> if (AppState.isInCheckout) return InAppResponse.SKIP
> return InAppResponse.SHOW
> ```
> Messages that are skipped via `SKIP` are marked as processed and won't be shown again automatically in this cycle. The user can still see them in the Mobile Inbox if the message is inbox-enabled.

---

## Step 2: Config Options

These are set on `IterableConfig.Builder` during SDK initialization:

```kotlin
val config = IterableConfig.Builder()
    .setInAppHandler(AppInAppHandler())
    .setInAppDisplayInterval(30.0)   // 🟡 Seconds between auto-displayed messages
    // .setUseInMemoryStorageForInApps(true)  // 🟢 Only if privacy requires it
    .build()
```

---

## Step 3: Pausing Auto-Display (optional)

Use `setAutoDisplayPaused` to temporarily prevent the SDK from showing in-app messages. Useful for flows where interruption would be harmful.

```kotlin
// Pause before entering a sensitive flow
IterableApi.getInstance().inAppManager.setAutoDisplayPaused(true)

// Resume when the flow is complete
IterableApi.getInstance().inAppManager.setAutoDisplayPaused(false)
```

> **Agent note:** Only wrap in `onSDKInitialized` if this code runs early in the app lifecycle. If it runs on a user-initiated screen transition, the SDK is already initialized.

---

## Gotchas

- **`InAppResponse` is a nested enum** inside `IterableInAppHandler`, not a top-level class. Import: `com.iterable.iterableapi.IterableInAppHandler.InAppResponse`.
- **Messages marked as processed won't auto-show again.** Both `SHOW` and `SKIP` mark the message as processed. If you skip a message, it won't retry automatically — the user would need to open it from the inbox (if inbox-enabled).
- **Message priority is set in Iterable, not in code.** The SDK processes messages sorted by priority (lower number = higher priority). The app cannot override this order.
- **JSON-only messages are never displayed.** Messages with `contentType` JSON are automatically marked as read and consumed without UI. They're used for data-only payloads.
- **In-app display requires an active Activity.** If the app is in the background, messages wait until the app returns to the foreground.
