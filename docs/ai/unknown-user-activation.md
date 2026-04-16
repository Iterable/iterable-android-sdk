# Unknown User Activation — Iterable Android SDK

Track anonymous visitors, create unknown user profiles when they meet business criteria, and merge them into known profiles upon identification.

> **Prerequisites:** SDK must be initialized (see [integration-guide.md](integration-guide.md)). This is a beta feature — talk to your Iterable CSM about enabling it.

---

## Business Context

Unknown User Activation solves the "lost visitor" problem: a user browses your app, shows interest (views products, adds to cart), but never logs in. Without this feature, that behavioral data is lost.

With Unknown User Activation, the SDK:

1. **Buffers visitor data locally** (events, cart updates, user profile updates) — with user consent
2. **Evaluates criteria defined by the marketing team** in Iterable's dashboard (e.g., "has a `viewedProduct` event")
3. **Creates an unknown user profile in Iterable** when criteria are met — enabling push, in-app, and embedded messaging to that anonymous user
4. **Merges the unknown profile into a known profile** when the user eventually logs in

The key concept is the **three-stage user lifecycle**:

- **Visitor** → anonymous, data stored on-device only, no Iterable profile
- **Unknown user** → still anonymous but met business criteria, Iterable profile exists, can receive messages
- **Known user** → identified by email/userId, unknown profile merged into known profile

> **Agent note:** Profile creation criteria are configured by the marketing team in Iterable's UI — not in code. The SDK fetches and evaluates them automatically. The developer's role is to enable the feature, handle consent, and wire up the callbacks.

---

## Decision Points

| Decision | Tier | Default | Notes |
|----------|------|---------|-------|
| Whether to enable the feature | 🔴 Critical | `false` | Ask the developer. Requires coordination with marketing (criteria must be defined in Iterable). |
| User consent flow for data tracking | 🔴 Critical | N/A | Legal/compliance requirement. The developer must implement a consent mechanism before enabling `visitorUsageTracked`. |
| Profile creation criteria | 🔴 Critical (marketing) | N/A | Defined by marketing in Iterable's UI. The developer cannot set this in code — but should confirm criteria exist before enabling the feature. |
| JWT server support for unknown user UUIDs | 🔴 Critical (if JWT) | N/A | If using JWT-enabled API keys, the JWT server must issue tokens for SDK-generated UUIDs. This often requires backend work. |
| `onUnknownUserCreated` callback | 🟠 Important | No-op | Useful for notifying the backend about new unknown UUIDs (especially for JWT token issuance). Ask the developer if their server needs to know. |
| Event threshold limit | 🟡 Relevant | 100 events | Max buffered events before oldest are dropped. Increase if the app generates many events pre-login. |
| Replay on visitor-to-known | 🟡 Relevant | `true` | When a visitor (no unknown profile) logs in, replay buffered data to the known profile. Almost always desired. |
| Merge on unknown-to-known | 🟡 Relevant | `true` | When an unknown user logs in, merge the unknown profile into the known profile. Almost always desired. |
| Foreground criteria refresh | 🟢 Optional | `true` | Re-fetch criteria on app foreground (with 2-min cooldown). |

---

## Step 1: Config

```kotlin
val config = IterableConfig.Builder()
    .setEnableUnknownUserActivation(true)                          // 🔴 Enable the feature
    .setUnknownUserHandler(object : IterableUnknownUserHandler {
        override fun onUnknownUserCreated(userId: String) {
            // 🟠 Notify your backend about the new unknown userId
            // Especially important if using JWT — server needs to issue tokens for this UUID
            Log.d("Iterable", "Unknown user created: $userId")
        }
    })
    .setAuthHandler(appAuthHandler)                                // 🔴 If using JWT
    .setEventThresholdLimit(100)                                   // 🟡 Max buffered events
    .setIdentityResolution(IterableIdentityResolution(
        replayOnVisitorToKnown = true,                             // 🟡 Replay visitor data on login
        mergeOnUnknownToKnown = true                               // 🟡 Merge unknown into known on login
    ))
    // .setEnableForegroundCriteriaFetch(true)                     // 🟢 Default is true
    .build()
```

---

## Step 2: User Consent

> **Agent note:** This is 🔴 Critical — the developer MUST implement a consent mechanism. Do not enable tracking without user consent.

```kotlin
// When user gives consent to data tracking
IterableApi.getInstance().visitorUsageTracked = true

// When user revokes consent (clears all locally stored data)
IterableApi.getInstance().visitorUsageTracked = false
```

When `visitorUsageTracked` is set to `true`:
- The SDK fetches profile creation criteria from Iterable
- Subsequent `track`, `updateCart`, `trackPurchase`, and `updateUser` calls buffer data locally
- The SDK evaluates criteria against buffered data after each event

When set to `false`:
- All locally stored visitor data is cleared
- No further local storage until re-enabled

---

## Step 3: Track Events (pre-login)

Use the same tracking APIs as for identified users — the SDK handles local buffering automatically:

```kotlin
IterableApi.getInstance().track("viewedProduct", dataFields)
IterableApi.getInstance().trackPurchase(total, items)
IterableApi.getInstance().updateCart(items)
IterableApi.getInstance().updateUser(dataFields)
```

These calls are stored locally when the user is a visitor. When criteria are met and an unknown profile is created, the SDK replays them to Iterable.

---

## Step 4: Criteria Match and Profile Creation (automatic)

When buffered events satisfy the marketing team's criteria:

1. SDK generates a UUID as the unknown user's `userId`
2. `onUnknownUserCreated(userId)` callback fires
3. If JWT is enabled, `onAuthTokenRequested` is called for the new UUID
4. SDK calls Iterable's `unknownuser/events/session` endpoint
5. Buffered data is replayed to the new unknown profile
6. The user can now receive push, in-app, and embedded messages

---

## Step 5: Identify the User (login)

When the user logs in, call `setEmail` or `setUserId` as normal:

```kotlin
IterableApi.getInstance().setEmail(email)
// or
IterableApi.getInstance().setUserId(userId)
```

What happens depends on the user's current state and `IdentityResolution` settings:

**If the user is a visitor** (never met criteria, no unknown profile):
- `replayOnVisitorToKnown = true` → buffered data is sent to the known profile
- `replayOnVisitorToKnown = false` → buffered data is discarded

**If the user is unknown** (has an unknown profile):
- `mergeOnUnknownToKnown = true` → unknown profile is merged into the known profile (all data transfers)
- `mergeOnUnknownToKnown = false` → unknown profile remains separate, future events go to the known profile

> **Agent note:** You can override `IdentityResolution` per login call:
> ```kotlin
> IterableApi.getInstance().setEmail(email, IterableIdentityResolution(true, true))
> ```

---

## Gotchas

- **`enableUnknownUserActivation` is `false` by default.** Nothing happens until you enable it.
- **`visitorUsageTracked` gates everything.** Without setting it to `true`, no local buffering or criteria evaluation occurs — even if the feature is enabled.
- **Criteria are defined in Iterable's UI, not in code.** If marketing hasn't defined criteria, the SDK will buffer data but never create unknown profiles. Confirm with the developer that criteria are set up.
- **JWT servers must handle unknown UUIDs.** When an unknown profile is created, `onAuthTokenRequested` is called with the generated UUID. If the JWT server doesn't recognize it, auth fails silently.
- **`onUnknownUserCreated` fires BEFORE the JWT request.** Use this callback to notify your server about the new UUID so it can issue tokens.
- **Event threshold limit drops oldest events silently.** If a visitor triggers more than `eventThresholdLimit` events, the oldest are discarded without warning.
- **Replay does NOT include push token registration events.** After merge/replay, push token may need to be re-registered depending on your flow.
- **`setEnableForegroundCriteriaFetch` Javadoc is misleading.** The field comment and actual behavior say `true` enables foreground fetch (with 2-min cooldown). Trust the behavior, not the setter Javadoc.
- **Setting `visitorUsageTracked = false` clears everything** — session data, criteria cache, buffered events. This is a hard reset.
