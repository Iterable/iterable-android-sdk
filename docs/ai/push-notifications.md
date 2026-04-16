# Push Notifications — Iterable Android SDK

Set up Firebase Cloud Messaging (FCM) push notifications with the Iterable SDK.

> **Prerequisites:** SDK must be initialized (see [integration-guide.md](integration-guide.md)). Firebase project must exist with `google-services.json` placed in the app module directory (may not be `app/` — check `settings.gradle` for the actual app module name).

---

## Decision Points

| Decision | Tier | Default | Notes |
|----------|------|---------|-------|
| Push integration name | 🟠 Important | App package name | Must match Iterable dashboard exactly. The SDK uses `context.getPackageName()` if not set. Confirm with the developer. |
| Notification channel name | 🟡 Relevant | `"iterable channel"` | User-visible in Android Settings. Should be branded (e.g., "MyApp Notifications"). Set via `iterable_notification_channel_name` manifest meta. |
| Small notification icon | 🟡 Relevant | App launcher icon | Should be a white silhouette on transparent background per Android guidelines. Set via `iterable_notification_icon` manifest meta. |
| Notification accent color | 🟡 Relevant | System default | Set via `iterable_notification_color` manifest meta. |
| Notification badging | 🟢 Optional | `true` | Set via `iterable_notification_badging` manifest meta. |
| When to request POST_NOTIFICATIONS permission | 🟠 Important | On first relevant screen | Android 13+ requires runtime permission. Without it, push silently fails. Ask the developer where this fits in their UX. |
| Auto push registration | 🟢 Optional | `true` | Automatically registers token on `setEmail`/`setUserId` and disables on logout. Almost always correct. |
| Subclass SDK service vs custom + delegate | 🟢 Optional | Subclass | Simpler. Use custom only if the app has an existing FirebaseMessagingService that handles non-Iterable messages. |

---

## Step 1: FirebaseMessagingService

> **Agent note:** Replace `AppFirebaseMessagingService` with a name that fits the project's naming conventions.

```kotlin
class AppFirebaseMessagingService : IterableFirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
    }
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}
```

> **Agent note:** If the app already has a `FirebaseMessagingService`, don't create a second one. Instead, delegate to the SDK from the existing service:
> ```kotlin
> IterableFirebaseMessagingService.handleMessageReceived(this, remoteMessage)
> IterableFirebaseMessagingService.handleTokenRefresh()
> ```

---

## Step 2: AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service android:name=".AppFirebaseMessagingService" android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

### Notification channel and icon metadata (inside `<application>`):

```xml
<meta-data
    android:name="iterable_notification_channel_name"
    android:value="@string/notification_channel_name" />

<meta-data
    android:name="iterable_notification_icon"
    android:resource="@drawable/ic_notification" />

<meta-data
    android:name="iterable_notification_color"
    android:resource="@color/notification_color" />
```

> **Agent note:** If the developer doesn't have a notification icon ready, skip the icon meta for now — the app icon will be used as fallback. But warn them it may look wrong (launcher icons are often not proper notification silhouettes).

---

## Step 3: Runtime Permission (Android 13+)

> **Agent note:** This is 🟠 Important — ask the developer where in the app flow this permission request should appear. Without it, push is silently blocked on Android 13+.

```kotlin
// IMPORTANT: registerForActivityResult MUST be a class property, not called inside a function body.
// Calling it dynamically (e.g., inside onCreate or a helper function) causes an IllegalStateException.
private val notificationPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { granted ->
    Log.d("Iterable", "Notification permission granted: $granted")
}

private fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
```

Required imports:
```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
```

---

## Dashboard Setup (developer must do this)

- [ ] Upload **Firebase service account JSON** (NOT the deprecated Server Key) in Iterable under Integrations > Mobile Apps
- [ ] The push integration name must **EXACTLY** match `setPushIntegrationName()` value (or the app package name if not set)
- [ ] Create a **Message Type** for the Push channel before creating templates
- [ ] Ensure GCP IAM role is **"Firebase Cloud Messaging API Admin"** (not similarly named roles)

---

## Gotchas

- **Missing runtime POST_NOTIFICATIONS permission** → push silently fails on Android 13+. No error, no log, nothing.
- **`registerForActivityResult` must be a class property** — calling it inside `onCreate()`, a helper function, or an extension function causes `IllegalStateException` at runtime. Always declare it as a property on the Activity class itself.
- **Push integration name mismatch** → tokens register but notifications never arrive. The name in code and dashboard must be identical, including case.
- **Debug builds with `applicationIdSuffix`** — if the app uses `applicationIdSuffix ".debug"` (common), the debug package name differs from release. The push integration name in Iterable's dashboard must match the actual running package, or use `context.packageName` at runtime instead of hardcoding.
- **`google-services.json` must include the debug package** — if the app uses `applicationIdSuffix ".debug"`, the Firebase project must have BOTH the release and debug package names registered as Android apps. Otherwise the build fails with "No matching client found for package name."
- **Don't call `registerForPush()` explicitly** when `setAutoPushRegistration(true)` — it handles registration automatically on `setEmail`/`setUserId`.
- **Custom sounds create new notification channels** — channel names are derived from the sound filename and are visible to users in Android Settings. Use clean, branded filenames.
- **The SDK ships its own `IterableFirebaseMessagingService` in the manifest with priority `-1`** — if you declare your own without understanding manifest merge priorities, you may get duplicate handling or missed messages.
- **`com.google.android.c2dm.permission.RECEIVE` is deprecated** — this legacy GCM permission is no longer needed for FCM on modern Android (API 21+). Do not add it.
