# User Profiles — Iterable Android SDK

Update user profile data, change email addresses, and manage subscription preferences.

> **Prerequisites:** SDK must be initialized and user identified with `setEmail` or `setUserId` (see [integration-guide.md](integration-guide.md)).

---

## Business Context

User profiles in Iterable store attributes used for segmentation and personalization. The marketing team uses profile data to:

- Segment users (e.g., "users with plan = premium")
- Personalize messages (e.g., "Hi {{firstName}}")
- Manage channel subscriptions (e.g., email opt-in lists, push channels)

The developer's role is to:

1. Send relevant profile data to Iterable at the right moments (login, profile update, settings change)
2. Manage subscription preferences based on the app's preference UI
3. Handle email changes securely

---

## Decision Points

| Decision | Tier | Default | Notes |
|----------|------|---------|-------|
| `setEmail` vs `setUserId` | 🔴 Critical | Depends on Iterable project type | Email-based projects use `setEmail`. UserId-based use `setUserId`. Hybrid projects can use either. Must match the Iterable project configuration. |
| What profile data fields to send | 🟠 Important | Minimal | Only fields used in campaigns/segments. Ask the developer what user attributes are available and relevant. |
| When to call `updateUser` | 🟠 Important | After login + on profile changes | Chain after `setEmail` success (especially with JWT). Also call when the user updates their profile in the app. |
| `mergeNestedObjects` true vs false | 🟡 Relevant | `false` | `false`: replaces nested objects entirely. `true`: deep merges them. Use `true` only when updating a subset of a nested object without losing other fields. |
| Subscription list/channel management | 🟡 Relevant | Driven by app's preference UI | Must align with Iterable's list/channel/message-type structure. Ask the developer if the app has a notification preferences screen. |
| null vs empty array in `updateSubscriptions` | 🟢 Optional | `null` for unchanged | `null` = don't change that list. Empty array = clear it entirely. Agent should use `null` for any axis not being modified. |

---

## Updating User Data Fields

```kotlin
val dataFields = JSONObject().apply {
    put("firstName", user.firstName)
    put("lastName", user.lastName)
    put("plan", user.subscriptionPlan)
}
IterableApi.getInstance().updateUser(dataFields)

// With nested object merge:
IterableApi.getInstance().updateUser(dataFields, true)  // mergeNestedObjects = true
```

> **Agent note:** `updateUser` is NOT queued by background init. Always call after `onSDKInitialized` and after `setEmail` success (especially with JWT). Without a valid user identity, the call either silently no-ops or buffers to unknown user storage if `enableUnknownUserActivation` is on.

---

## Changing Email Address

```kotlin
IterableApi.getInstance().updateEmail(
    newEmail,
    { // onSuccess
        Log.d("Iterable", "Email updated successfully")
    },
    { reason, _ -> // onFailure
        Log.e("Iterable", "Email update failed: $reason")
    }
)
```

`updateEmail` is queued by background init (unlike `updateUser`). On success, it:

1. Updates the internal email in the SDK
2. Triggers a JWT token refresh (if JWT is enabled)
3. Persists the new auth data

---

## Updating Subscriptions

Manage list membership and channel/message-type preferences:

```kotlin
IterableApi.getInstance().updateSubscriptions(
    arrayOf(12345),      // emailListIds to subscribe to (null = don't change)
    arrayOf(67890),      // unsubscribedChannelIds (null = don't change)
    arrayOf(11111),      // unsubscribedMessageTypeIds (null = don't change)
)

// Extended form with subscribedMessageTypeIds and attribution:
IterableApi.getInstance().updateSubscriptions(
    emailListIds,
    unsubscribedChannelIds,
    unsubscribedMessageTypeIds,
    subscribedMessageTypeIds,
    campaignId,
    templateId
)
```

### Semantics:
- **`null`** = don't modify this list
- **Empty array `arrayOf()`** = clear all entries in this list
- **Non-empty array** = set these specific IDs

> **Agent note:** The list/channel/message-type IDs come from the Iterable project. Ask the developer (🟡 Relevant) what their preference UI looks like and which IDs correspond to which options.

---

## Gotchas

- **`updateUser` is NOT queued** by background init. It returns silently if the SDK is not initialized with a valid identity. Chain it after `setEmail` success.
- **`updateEmail` IS queued** by background init, but requires the SDK to be initialized with email or userId. It fails with a callback if not.
- **`updateSubscriptions` is NOT queued** and returns silently if not initialized. No failure callback.
- **Setting email clears userId and vice versa.** `setEmail` nulls out `_userId`; `setUserId` nulls out `_email`. Don't call both for the same user — use whichever matches your Iterable project type.
- **`mergeNestedObjects = false` (default) replaces entire nested objects.** If you have `{"address": {"city": "SF", "state": "CA"}}` and update with `{"address": {"city": "LA"}}`, the `state` field is lost. Use `true` to preserve unmodified nested fields.
- **`updateUser` with unknown user activation:** If `enableUnknownUserActivation` is on and no user is identified, `updateUser` stores the data locally via `UnknownUserManager.trackUnknownUpdateUser` for later replay.
