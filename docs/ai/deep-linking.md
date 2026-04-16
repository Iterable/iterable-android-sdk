# Deep Linking — Iterable Android SDK

Handle deep links and custom actions from push notifications, in-app messages, and embedded messages.

> **Prerequisites:** SDK must be initialized (see [integration-guide.md](integration-guide.md)).

---

## Business Context

Deep links connect Iterable campaigns (push, in-app, embedded) to specific screens in the app. The marketing team sets link URLs in Iterable templates. The developer's role is to:

1. Define how URLs map to app screens
2. Register the URL handler with the SDK
3. Configure which URL schemes the SDK is allowed to open

Custom actions (`action://` URLs) are an alternative to deep links — they trigger app-specific logic (dismiss UI, toggle features, fire analytics) without navigating to a URL.

---

## Decision Points

| Decision | Tier | Default | Notes |
|----------|------|---------|-------|
| Does the app use a custom URI scheme? | 🟠 Important | No (HTTPS only) | e.g., `myapp://`. Must be registered with `setAllowedProtocols`. Ask the developer. |
| App Links (HTTPS verified) vs custom scheme | 🟡 Relevant | Custom scheme (simpler) | App Links require hosting `assetlinks.json` on the domain. Better for shared web/app URLs. |
| URL-to-screen mapping | 🔴 Critical | N/A | How do URLs map to app screens? The developer must define this — it's app-specific. |
| Implement `IterableCustomActionHandler`? | 🟡 Relevant | No | Only needed if Iterable campaigns use `action://` URLs. Ask the developer if marketing uses custom actions. |
| Behavior based on action source (push vs in-app vs embedded) | 🟢 Optional | Same for all sources | `IterableActionContext.source` tells you where the link came from. Most apps don't differentiate. |

---

## Step 1: URL Handler

> **Agent note:** Ask the developer (🔴 Critical) what URL patterns the app uses and how they map to screens. Adapt the routing logic below.

```kotlin
class AppUrlHandler(private val appContext: Context) : IterableUrlHandler {
    override fun handleIterableURL(uri: Uri, actionContext: IterableActionContext): Boolean {
        if (uri.scheme == "yourscheme" && uri.host == "detail") {
            val itemId = uri.pathSegments.firstOrNull()?.toIntOrNull() ?: return false
            appContext.startActivity(Intent(appContext, DetailActivity::class.java).apply {
                putExtra("item_id", itemId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            return true
        }
        // Return false for URLs you don't handle — the SDK will try to open them with the system
        return false
    }
}
```

Register in `IterableConfig.Builder`:
```kotlin
.setUrlHandler(AppUrlHandler(this))
.setAllowedProtocols(arrayOf("yourscheme"))  // 🟠 Only if using custom schemes
```

---

## Step 2: AndroidManifest Intent Filters (optional)

> **Agent note:** Manifest intent filters are only needed if deep links arrive from OUTSIDE the Iterable SDK (e.g., from a browser, email, or other app). Links from Iterable push notifications, in-app messages, and embedded messages are routed through the `UrlHandler` registered in Step 1 — they do NOT need manifest intent filters.
>
> Adding intent filters for the same scheme to multiple activities will cause Android to show a disambiguation dialog. Only add them if the developer specifically needs system-level deep link handling.

For the system to route URLs to the app (outside of the SDK handler), declare intent filters:

### Custom scheme:
```xml
<activity android:name=".MainActivity" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="yourscheme" />
    </intent-filter>
</activity>
```

### App Links (HTTPS verified):
```xml
<activity android:name=".MainActivity" android:exported="true">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="https" android:host="yourdomain.com" />
    </intent-filter>
</activity>
```

> **Agent note:** App Links require hosting a `/.well-known/assetlinks.json` file on the domain. This is usually a web team or DevOps task — inform the developer if they choose this path.

---

## Step 3: Custom Action Handler (optional)

Only implement this if campaigns use `action://` URLs. The SDK routes any non-`openUrl` action type here.

```kotlin
class AppCustomActionHandler : IterableCustomActionHandler {
    override fun handleIterableCustomAction(action: IterableAction, actionContext: IterableActionContext): Boolean {
        when (action.type) {
            "dismiss" -> { /* dismiss current screen */ }
            "showPromo" -> { /* navigate to promo screen */ }
            else -> return false
        }
        return true
    }
}
```

Register in `IterableConfig.Builder`:
```kotlin
.setCustomActionHandler(AppCustomActionHandler())
```

> **Agent note:** Custom actions also apply to embedded message clicks. URLs starting with `action://` or `itbl://` in embedded messages are routed through this handler.

---

## Gotchas

- **Custom schemes are blocked by default.** The SDK only allows `https` URLs. You MUST add `.setAllowedProtocols(arrayOf("yourscheme"))` or custom scheme links are silently dropped with a debug log.
- **Return `true` only when you fully handled the URL.** Returning `true` tells the SDK to stop processing. If you return `false`, the SDK tries to resolve the URL with `ACTION_VIEW` intent (system/browser).
- **`handleIterableURL` receives links from all channels** — push opens, in-app button taps, embedded message clicks. Use `actionContext.source` if you need to differentiate.
- **The SDK processes push open URLs through `IterableTrampolineActivity`.** This is an internal activity that handles the action and finishes. Don't interfere with it in your manifest.
- **`handleEmbeddedClick` does NOT call `trackEmbeddedClick` automatically.** If you build custom embedded UI, you must call both `handleEmbeddedClick` (for routing) and `trackEmbeddedClick` (for analytics) yourself. The OOTB embedded views handle both.
