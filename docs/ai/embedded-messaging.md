# Embedded Messaging — Iterable Android SDK

Display persistent, native embedded messages within app screens using placements configured in Iterable.

> **Prerequisites:** SDK must be initialized with `setEnableEmbeddedMessaging(true)` (see [integration-guide.md](integration-guide.md)). Dependency: `iterableapi-ui` for OOTB views.

---

## Business Context

Embedded messages are persistent content blocks that live inside app screens — unlike in-app messages (which are overlays) or push (which is external). The marketing team creates campaigns and assigns them to **placements** in Iterable's dashboard. Each placement has an auto-generated numeric ID.

The developer's role is to:

1. Enable embedded messaging in the SDK config
2. Get placement IDs from the marketing team (or Iterable dashboard)
3. Register update listeners on screens that show embedded content
4. Render the messages using OOTB views or custom UI

---

## Decision Points

| Decision | Tier | Default | Notes |
|----------|------|---------|-------|
| Enable embedded messaging | 🟠 Important | `false` | Only enable if the app uses embedded placements. Ask the developer. |
| Placement IDs | 🔴 Critical | N/A | Auto-generated in Iterable dashboard. Must ask the developer — never assume a value. |
| Which screens show embedded content | 🔴 Critical | N/A | App-specific. Ask the developer which screens should display embedded messages. |
| OOTB view type (Banner, Card, Notification) vs custom UI | 🟡 Relevant | OOTB Card | Start with OOTB for speed. Go custom when the design system requires it. |
| Which message to show when placement returns multiple | 🟡 Relevant | First in list | Confirm with the developer this is intentional. Marketing may expect priority-based selection. |
| OOTB styling (colors, border, radius) | 🟢 Optional | SDK defaults | Customize via `IterableEmbeddedViewConfig` to match app theme. |

---

## Step 1: Enable in Config

```kotlin
val config = IterableConfig.Builder()
    .setEnableEmbeddedMessaging(true)  // 🟠 Only if using embedded messages
    .build()
```

---

## Step 2: Register Update Listener

> **Agent note:** Replace `PLACEMENT_ID` with the actual placement ID from the developer (🔴 Critical — never assume a value).

```kotlin
import com.iterable.iterableapi.IterableEmbeddedUpdateHandler

class YourFragment : Fragment(), IterableEmbeddedUpdateHandler {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        IterableApi.onSDKInitialized {
            IterableApi.getInstance().embeddedManager.addUpdateListener(this)
            activity?.runOnUiThread { displayEmbeddedMessages() }
        }
    }

    override fun onMessagesUpdated() {
        activity?.runOnUiThread { displayEmbeddedMessages() }
    }

    override fun onEmbeddedMessagingDisabled() {
        // Hide embedded UI — messaging was disabled server-side or due to auth issues
    }

    private fun displayEmbeddedMessages() {
        val messages = IterableApi.getInstance().embeddedManager.getMessages(PLACEMENT_ID)
        if (messages.isNullOrEmpty()) {
            // Hide embedded UI container
            return
        }
        val message = messages.first()
        // Render using OOTB view or custom UI (see below)
    }

    override fun onDestroyView() {
        try { IterableApi.getInstance().embeddedManager.removeUpdateListener(this) }
        catch (_: Exception) { }
    }
}
```

---

## Step 3a: OOTB Views

The SDK provides three view types: `BANNER`, `CARD`, and `NOTIFICATION`.

```kotlin
// newInstance takes (viewType, message, config?) — NO context parameter
val embeddedView = IterableEmbeddedView.newInstance(
    IterableEmbeddedViewType.CARD,   // or BANNER, NOTIFICATION
    message,
    null                              // optional IterableEmbeddedViewConfig
)
yourContainer.addView(embeddedView)
```

**Custom styling:**

> **Agent note:** `IterableEmbeddedViewConfig` is a data class with NO default values on its parameters. You must provide ALL 10 fields — use `null` for any you don't want to customize.

```kotlin
val config = IterableEmbeddedViewConfig(
    backgroundColor = Color.WHITE,
    borderColor = Color.LTGRAY,
    borderWidth = 1,
    borderCornerRadius = 8f,
    primaryBtnBackgroundColor = Color.BLUE,
    primaryBtnTextColor = Color.WHITE,
    secondaryBtnBackgroundColor = null,
    secondaryBtnTextColor = null,
    titleTextColor = null,
    bodyTextColor = null
)
```

> **Agent note:** OOTB views handle both `handleEmbeddedClick` (navigation) and `trackEmbeddedClick` (analytics) automatically on button and default action taps.

---

## Step 3b: Custom UI

If the design requires a fully custom layout, use the message data model directly:

```kotlin
val title = message.elements?.title
val body = message.elements?.body
val imageUrl = message.elements?.mediaUrl
val buttons = message.elements?.buttons  // List<EmbeddedMessageElementsButton>
val defaultAction = message.elements?.defaultAction

// On button click — MUST call both:
IterableApi.getInstance().embeddedManager.handleEmbeddedClick(message, button.id, clickedUrl)
IterableApi.getInstance().trackEmbeddedClick(message, button.id, clickedUrl)
```

> **Agent note:** `handleEmbeddedClick` does NOT call `trackEmbeddedClick` automatically. Custom UI must call both or clicks won't be tracked. The OOTB views handle this internally.

---

## Dashboard Setup

- [ ] **Embedded placement** must be created in Iterable (auto-generated ID)
- [ ] **Message Type** for Embedded channel must exist before creating templates
- [ ] **Campaign** must be active and targeting the user

---

## Gotchas

- **Accessing `embeddedManager` before SDK init throws RuntimeException.** Always wrap in `onSDKInitialized`.
- **Placement IDs are auto-generated** by Iterable — never hardcode `1` or any assumed value. Ask the developer for the actual ID from the dashboard.
- **`getMessages()` returns `List?` (nullable).** Always use `isNullOrEmpty()`.
- **Messages sync on foreground switch.** After creating a campaign, background and foreground the app to trigger a sync.
- **`onEmbeddedMessagingDisabled` fires when messaging is disabled** server-side or due to subscription/auth issues. Hide the embedded UI when this is called.
- **Create a Message Type for the Embedded channel** in the Iterable dashboard before creating templates. Without it, campaigns can't be created.
