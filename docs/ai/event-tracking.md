# Event Tracking — Iterable Android SDK

Track custom events, purchases, and cart updates to power Iterable campaigns and segmentation.

> **Prerequisites:** SDK must be initialized and user identified with `setEmail` or `setUserId` (see [integration-guide.md](integration-guide.md)).

---

## Business Context

Events are the foundation of Iterable's targeting and automation. The marketing team uses events to:

- Trigger journeys (e.g., "user viewed product → send follow-up email")
- Build segments (e.g., "users who purchased in the last 7 days")
- Personalize content (e.g., "show recently viewed items")

The developer's role is to:

1. Instrument the app with event tracking calls at the right moments
2. Decide what data to include in each event
3. Coordinate with the marketing team on event names and data fields so they align with Iterable campaign logic

---

## Decision Points

| Decision | Tier | Default | Notes |
|----------|------|---------|-------|
| Which events to track | 🔴 Critical | N/A | Entirely a business decision. Must ask the developer what user actions matter. Common: viewed product, added to cart, completed purchase, signed up, searched. |
| Event names | 🔴 Critical | N/A | Must align with what the marketing team expects in Iterable. Ask the developer or suggest a naming convention (e.g., camelCase: `viewedProduct`, `completedCheckout`). |
| Data fields per event | 🟠 Important | Minimal | What metadata to attach? Only fields useful for segmentation/personalization. Ask the developer what's available and relevant. |
| `updateCart` vs `trackPurchase` | 🟠 Important | Both | `updateCart` for cart state changes. `trackPurchase` for completed orders. Ask if the app has a cart concept. |
| Campaign/template attribution | 🟢 Optional | Omit | Only needed when attributing events to specific Iterable campaigns. Most apps don't need this. |

---

## Custom Events

> **Agent note:** Adapt event names and data fields to the app's domain. These are examples — ask the developer (🔴 Critical) what events and fields matter.

```kotlin
// Simple event
IterableApi.getInstance().track("viewedProduct")

// Event with data fields
val dataFields = JSONObject().apply {
    put("productName", product.name)
    put("productId", product.id)
    put("category", product.category)
    put("price", product.price)
}
IterableApi.getInstance().track("viewedProduct", dataFields)
```

### Method signatures:

```kotlin
track(eventName: String)
track(eventName: String, dataFields: JSONObject)
track(eventName: String, campaignId: Int, templateId: Int)
track(eventName: String, campaignId: Int, templateId: Int, dataFields: JSONObject)
```

---

## Purchase Tracking

```kotlin
val items = cartItems.map { item ->
    CommerceItem(
        item.id.toString(),   // id (required)
        item.name,            // name (required)
        item.price,           // price (required)
        item.quantity          // quantity (required)
        // Optional: sku, description, url, imageUrl, categories, dataFields
    )
}
IterableApi.getInstance().trackPurchase(orderTotal, items)

// With additional order data fields:
val orderFields = JSONObject().apply {
    put("orderId", order.id)
    put("paymentMethod", "credit_card")
}
IterableApi.getInstance().trackPurchase(orderTotal, items, orderFields)
```

### CommerceItem fields:

| Field | Required | Notes |
|-------|----------|-------|
| `id` | Yes | Product identifier |
| `name` | Yes | Product name |
| `price` | Yes | Unit price |
| `quantity` | Yes | Quantity purchased |
| `sku` | No | Stock keeping unit |
| `description` | No | Product description |
| `url` | No | Product page URL |
| `imageUrl` | No | Product image URL |
| `categories` | No | List of category strings |
| `dataFields` | No | Additional custom fields as JSONObject |

---

## Cart Updates

Use `updateCart` to track the current state of the user's cart. This is separate from `trackPurchase` — cart updates represent intent, purchases represent completion.

```kotlin
val cartItems = listOf(
    CommerceItem("prod-1", "T-Shirt", 29.99, 2),
    CommerceItem("prod-2", "Jeans", 59.99, 1)
)
IterableApi.getInstance().updateCart(cartItems)
```

> **Agent note:** `updateCart` replaces the entire cart state — it's not additive. Send the full current cart contents each time.

---

## Gotchas

- **`updateUser` is not `track`.** Profile data fields go through `updateUser` (see [user-profiles.md](user-profiles.md)). Event tracking is for behavioral data (actions the user took).
- **Wrap tracking calls in `onSDKInitialized` when called early in the app lifecycle.** If using `initializeInBackground`, tracking calls made before init completes should be wrapped in `IterableApi.onSDKInitialized { }` to ensure the SDK is ready.
- **Event names should be consistent.** `viewedProduct` and `ViewedProduct` are different events in Iterable. Agree on a convention with the marketing team.
- **Keep data fields lean.** Only send fields that will be used in segments, campaigns, or personalization. Dumping the entire app state wastes bandwidth and clutters the Iterable project.
- **Unknown user buffering:** If `enableUnknownUserActivation` is on, events tracked before user identification are stored locally and replayed when the user becomes known (see [unknown-user-activation.md](unknown-user-activation.md)).
