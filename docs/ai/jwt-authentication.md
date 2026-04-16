# JWT Authentication — Iterable Android SDK

Set up JWT-based authentication for secure SDK communication with Iterable.

> **Prerequisites:** SDK must be initialized (see [integration-guide.md](integration-guide.md)). A server-side JWT generator must exist or be created.

---

## Business Context

JWT authentication adds a security layer between the SDK and Iterable's API. When enabled on your Iterable API key, every SDK request must include a valid JWT token — without one, ALL calls silently fail with zero errors.

JWT tokens must be generated **server-side** — the SDK requests tokens from your backend via the `onAuthTokenRequested` callback. The developer needs:

1. An Iterable API key that requires JWT (configured in Iterable dashboard)
2. A server endpoint that generates JWT tokens for a given email or userId
3. An auth handler wired into the SDK

> **Agent note — IMPORTANT decision point:**
> - If the app **already has a JWT generator** (server endpoint producing Iterable-compatible JWTs), wire it into `onAuthTokenRequested`.
> - If the app **does NOT have one** and JWT is required, **STOP and inform the developer**: a JWT generator must be implemented server-side before proceeding. Do NOT create a mock generator — it is insecure.
> - If JWT is **not required** (developer confirmed), skip this guide entirely and omit `.setAuthHandler()` from the config.

---

## Decision Points

| Decision | Tier | Default | Notes |
|----------|------|---------|-------|
| Is JWT required for this API key? | 🔴 Critical | N/A | Check with developer. If yes and no auth handler is set, ALL SDK calls silently fail. |
| Where are tokens generated? | 🔴 Critical | Server-side | Never generate production tokens locally. The developer needs a backend endpoint. |
| Token TTL / refresh lead time | 🟡 Relevant | Refresh 60s before expiry | `setExpiringAuthTokenRefreshPeriod` in seconds. Short-lived tokens (minutes to hours) are standard. |
| Retry policy on auth failure | 🟢 Optional | 10 retries, 6s linear backoff | Tune via `setAuthRetryPolicy` only if seeing issues. |
| What to do on persistent auth failure | 🟠 Important | Log the error | Ask developer: should the app force logout? Show an error? Retry silently? |

---

## Step 1: Auth Handler

> **Agent note:** The auth handler must read the current user's identity fresh on every call. Do NOT cache the email at startup — `onAuthTokenRequested` is called at unpredictable times (token refresh, retries, background). A stale email causes `AUTH_TOKEN_NULL`.

```kotlin
class AppAuthHandler(
    private val generateToken: () -> String?
) : IterableAuthHandler {

    override fun onAuthTokenRequested(): String? {
        // Must return a fresh JWT token for the current user
        // Read email/userId from the source of truth (DataStore, SharedPreferences, etc.)
        return generateToken()
    }

    override fun onTokenRegistrationSuccessful(authToken: String) {
        // WARNING: This is a LOCAL callback, NOT a server confirmation.
        // The server may still reject the token.
    }

    override fun onAuthFailure(authFailure: AuthFailure) {
        Log.e("IterableAuth", "Auth failure: ${authFailure.failureReason}")
        // 🟠 Ask developer what should happen here:
        // - Log and continue?
        // - Force re-login?
        // - Show error UI?
    }
}
```

Register in config:
```kotlin
.setAuthHandler(AppAuthHandler {
    generateJwtForEmail(getFreshEmail())  // Must read fresh email every time
})
```

---

## Step 2: Config Options

```kotlin
val config = IterableConfig.Builder()
    .setAuthHandler(appAuthHandler)
    .setExpiringAuthTokenRefreshPeriod(60)  // 🟡 Seconds before expiry to refresh. Default: 60.
    // .setAuthRetryPolicy(RetryPolicy(10, 6, RetryPolicy.Type.LINEAR))  // 🟢 Default is fine
    .build()
```

---

## Step 3: Token Flow

The SDK manages the token lifecycle automatically:

1. `setEmail` / `setUserId` triggers `onAuthTokenRequested`
2. SDK stores the token and uses it for all API requests
3. Before the token expires, SDK calls `onAuthTokenRequested` again (foreground only)
4. On 401 responses with JWT error codes, SDK retries with a fresh token
5. If retries are exhausted, `onAuthFailure` is called

> **Agent note:** Token refresh only runs while the app is in the foreground. When the app returns to the foreground, the SDK re-queues refresh if the token is expired or near expiry.

---

## JWT for Unknown User Activation

If the app uses Unknown User Activation (see [unknown-user-activation.md](unknown-user-activation.md)) with a JWT-enabled key:

- The JWT server must be able to issue tokens for **SDK-generated unknown user UUIDs** (e.g., `2411ce94-52df-47aa-98bc-7293ac951121`)
- `onAuthTokenRequested` is called after the SDK creates an unknown user — use `IterableApi.getInstance().getUserId()` to get the generated UUID
- The `onUnknownUserCreated` callback fires **before** the JWT request, giving you a chance to notify your server about the new userId

---

## Gotchas

- **JWT-required key with no auth handler** → ALL SDK calls silently fail. This is the #1 integration trap. Zero errors, zero logs.
- **JWT secret encoding** → If generating tokens locally (dev/test only): use the secret as raw UTF-8. Do NOT hex-decode or base64-decode.
- **Caching email at startup for the auth handler** → `AUTH_TOKEN_NULL`. The handler lambda must read from the source of truth every time.
- **`onTokenRegistrationSuccessful` is NOT a server confirmation** — it only means the SDK stored the token locally. Check `onAuthFailure` for server rejections.
- **After changing auth code, always logout and re-login** — the SDK caches auth state.
- **Don't fire setEmail + updateUser + registerForPush simultaneously** → auth race condition. Chain `updateUser` inside `setEmail`'s `onSuccess` callback.
- **`setExpiringAuthTokenRefreshPeriod` takes seconds** but is stored internally as milliseconds. The SDK handles the conversion.
