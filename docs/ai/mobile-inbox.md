# Mobile Inbox — Iterable Android SDK

Set up and customize the Mobile Inbox — a persistent message center where users can browse saved in-app messages.

> **Prerequisites:** SDK must be initialized with the `iterableapi-ui` dependency (see [integration-guide.md](integration-guide.md)). In-app messaging must be configured (see [in-app-messages.md](in-app-messages.md)). Messages must be marked as "inbox-enabled" in Iterable campaigns.

---

## Business Context

Mobile Inbox is a message center that shows accumulated in-app messages. Unlike auto-displayed in-app messages (which appear once and disappear), inbox messages persist until the user reads or deletes them. The inbox content, message templates, and targeting are managed by the marketing team in Iterable. The developer's role is to:

1. Add the inbox UI to the app's navigation
2. Customize the look and feel to match the app's design
3. Optionally filter, sort, or format messages based on product requirements

---

## Decision Points

| Decision | Tier | Default | Notes |
|----------|------|---------|-------|
| Where in the app navigation to place the inbox | 🔴 Critical | N/A | Tab? Menu item? Profile screen? Must ask the developer. |
| POPUP vs ACTIVITY display mode | 🟠 Important | POPUP | POPUP shows the message as an overlay. ACTIVITY opens a dedicated screen. Ask developer preference. |
| Empty state copy (title and body when inbox is empty) | 🟠 Important | Blank (no text shown) | The default is a blank screen which looks broken. Ask the developer for copy, or suggest: "No messages" / "You're all caught up!" |
| Custom row layout | 🟡 Relevant | SDK default layout | Only needed if the app's design system requires specific styling. Default shows title, subtitle, date, icon, unread dot. |
| Message filtering | 🟡 Relevant | Show all inbox messages | Filter by message type, custom payload, or other criteria? Ask in Full control mode. |
| Sort order | 🟡 Relevant | Newest first | Alternative: oldest first, alphabetical, by priority. |
| Date format | 🟢 Optional | System locale (Medium date, Short time) | Relative time ("2 hours ago") is a common alternative. |
| Activity/screen title | 🟢 Optional | No title | Set via intent extra if using `IterableInboxActivity`. |

---

## Minimal Setup

The simplest inbox is a one-line subclass added to the app's navigation:

```kotlin
class InboxFragment : IterableInboxFragment()
```

Add this fragment to whatever navigation hosts the inbox (tab, menu action, etc.).

**Or launch as a standalone activity:**

```kotlin
startActivity(Intent(this, IterableInboxActivity::class.java))
```

---

## Customization

### Empty State

> **Agent note:** The default empty state shows blank text views — this looks broken. Always set empty state copy.

```kotlin
class InboxFragment : IterableInboxFragment() {
    companion object {
        fun newInstance() = IterableInboxFragment.newInstance(
            InboxMode.POPUP,
            0,  // 0 = default row layout
            "No messages",          // 🟠 Ask developer for copy
            "You're all caught up!" // 🟠 Ask developer for copy
        )
    }
}
```

### Filtering Messages

Implement `IterableInboxFilter` to control which messages appear. Common use: filter by message type from `customPayload`.

```kotlin
class InboxFragment : IterableInboxFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFilter { message ->
            // Example: only show messages with a specific type
            val payload = message.customPayload
            payload?.optString("messageType") != "hidden"
        }
    }
}
```

### Sorting Messages

```kotlin
setComparator { m1, m2 ->
    // Example: sort alphabetically by title
    val t1 = m1.inboxMetadata?.title ?: ""
    val t2 = m2.inboxMetadata?.title ?: ""
    t1.compareTo(t2)
}
```

Default: newest first (descending `createdAt`).

### Date Formatting

```kotlin
setDateMapper { message ->
    message.createdAt?.let {
        DateUtils.getRelativeTimeSpanString(
            it.time,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
    } ?: ""
}
```

Default: `DateFormat.getDateTimeInstance(MEDIUM, SHORT)`.

### Custom Row Layout

Provide a custom layout XML. The SDK binds to these view IDs automatically if present:

- `R.id.title` — message title
- `R.id.subtitle` — message subtitle
- `R.id.date` — formatted date
- `R.id.imageView` — message icon/thumbnail
- `R.id.unreadIndicator` — unread dot indicator

```kotlin
// Via fragment constructor
IterableInboxFragment.newInstance(InboxMode.POPUP, R.layout.custom_inbox_row)

// Or via activity intent
intent.putExtra("itemLayoutId", R.layout.custom_inbox_row)
```

### Advanced: Multiple Row Types with AdapterExtension

For different layouts per message type, implement `IterableInboxAdapterExtension`:

```kotlin
class InboxFragment : IterableInboxFragment(), IterableInboxAdapterExtension<Nothing> {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAdapterExtension(this)
    }

    override fun getItemViewType(message: IterableInAppMessage): Int {
        return when (message.customPayload?.optString("style")) {
            "promo" -> 1
            else -> 0
        }
    }

    override fun getLayoutForViewType(viewType: Int): Int {
        return when (viewType) {
            1 -> R.layout.promo_inbox_row
            else -> R.layout.default_inbox_row
        }
    }

    override fun createViewHolderExtension(view: View, viewType: Int): Nothing? = null
    override fun onBindViewHolder(holder: Nothing?, holderBinding: View, message: IterableInAppMessage) {}
}
```

> **Agent note:** The sample app at `sample-apps/inbox-customization/` has working examples of all these patterns. Reference it for implementation details.

---

## Gotchas

- **Empty state with no copy looks broken.** The layout always shows the empty state views — if you don't pass title/body text, users see a blank screen with no explanation.
- **Inbox messages are a subset of in-app messages.** Only messages marked as "Send to inbox" in Iterable campaigns appear in the inbox. Regular in-app messages don't show here.
- **`IterableInboxAdapterExtension` default binding runs first.** The SDK binds title/subtitle/date/icon using the standard view IDs, then your extension's `onBindViewHolder` runs. You can override what the default set, or bind additional views.
- **Swipe-to-delete is built in.** Left swipe deletes a message with `INBOX_SWIPE` delete type. This cannot be disabled without subclassing deeper.
- **Impression tracking is automatic.** The fragment tracks inbox session start/end and per-message impressions via `InboxSessionManager`. Don't strip `onResume`/`onPause` behavior when wrapping the fragment.
- **Custom row layouts must use the expected view IDs** (`title`, `subtitle`, `date`, `imageView`, `unreadIndicator`) for automatic binding. If your layout uses different IDs, the SDK won't crash but won't bind those fields — you'd need to handle binding in an `AdapterExtension`.
