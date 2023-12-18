# AnonymousUserManager Class

## Class Introduction

The `AnonymousUserManager` class is responsible for managing anonymous user sessions and tracking events in an Android application.
It includes methods for updating sessions, tracking events, and handling criteria for transitioning to known users.

## Class Structure

The `AnonymousUserManager` class includes the following key components:

- **Fields:**
    - `TAG`: A constant String for logging.

- **Methods:**
    - `updateAnonSession()`: Updates the anonymous user session.
    - `trackAnonEvent(String eventName, JSONObject dataFields)`: Tracks an anonymous event.
    - `trackAnonPurchaseEvent(double total, List<CommerceItem> items, JSONObject dataFields)`: Tracks an anonymous purchase event.
    - `trackAnonUpdateCart(List<CommerceItem> items)`: Tracks an anonymous cart update.
    - `getCriteria()`: Retrieves criteria for user transition.
    - `checkCriteriaCompletion()`: Checks if criteria for user transition are met.
    - `createKnownUser()`: Creates a known user after criteria are met.
    - `syncEvents()`: Syncs tracked events with the server.
    - `clearLocallyStoredData()`: Clears locally stored data.
    - `storeEventListToLocalStorage(JSONObject newDataObject)`: Stores event data in local storage.
    - `getEventListFromLocalStorage()`: Retrieves event data from local storage.
    - `getCurrentTime()`: Gets the current time in milliseconds.
    - `getCurrentDateTime()`: Gets the current date and time in UTC format.

## Methods Description

### `updateAnonSession()`

This method updates the anonymous user session. It does the following:

* Retrieves the previous session data from SharedPreferences.
* Increments the session number.
* Stores the updated session data in SharedPreferences.

### `trackAnonEvent(eventName, dataFields)`

This method tracks an anonymous event. It does the following:

* Creates a JSON object with event details, including the event name, timestamp, data fields, and tracking type.
* Stores the event data in local storage.
* Checks criteria completion and creates a known user if criteria are met.

### `trackAnonPurchaseEvent(total, items, dataFields)`

This method tracks an anonymous purchase event. It does the following:

* Converts the list of commerce items to JSON.
* Creates a JSON object with purchase event details, including items, total, timestamp, data fields, and tracking type.
* Stores the purchase event data in local storage.
* Checks criteria completion and creates a known user if criteria are met.

### `trackAnonUpdateCart(items)`

This method tracks an anonymous cart update. It does the following:

* Converts the list of commerce items to JSON.
* Creates a JSON object with cart update details, including items, timestamp, and tracking type.
* Stores the cart update data in local storage.
* Checks criteria completion and creates a known user if criteria are met.

### `getCriteria()`

This method is responsible for fetching criteria data. It simulates calling an API and saving data in SharedPreferences.

### `checkCriteriaCompletion()`

This private method checks if criteria for creating a known user are met. It compares stored event data with predefined criteria and returns `true` if criteria are completed.

### `createKnownUser()`

This  method is responsible for creating a known user in the Iterable API. It does the following:

* Sets a random user ID using a UUID (Universally Unique Identifier).
* Retrieves user session data from SharedPreferences.
* If user session data exists, it updates the user information in the Iterable API.
* Calls the syncEvents() method to synchronize tracked events.
* Finally, it clears locally stored data using the clearLocallyStoredData() method.

### `syncEvents()`

This method is used to synchronize tracked events stored in local storage with the Iterable API. It performs the following tasks:

* Retrieves the list of tracked events from local storage using the getEventListFromLocalStorage() method.
* Iterates through the list of events and processes each event based on its type.
* Supported event types include regular event tracking, purchase event tracking, and cart update tracking.
* For each event, it extracts relevant data, including event name, data fields, items (for purchase and cart update events), and timestamps.
* It uses the Iterable API to track these events or update the user's cart.
* After processing all events, it calls the clearLocallyStoredData() method to clear locally stored event data.

### `clearLocallyStoredData()`

This method is responsible for clearing locally stored data in SharedPreferences. It removes both the anonymous session data and the event list data, effectively cleaning up after successful synchronization with the Iterable API.

### `storeEventListToLocalStorage(JSONObject newDataObject)`

This method is used to add a new event to the list of events stored in local storage. It takes a JSON object representing the new event data and performs the following steps:

* Retrieves the existing list of events from local storage using the getEventListFromLocalStorage() method.
* Appends the new event data to the list.
* Updates the event list data in SharedPreferences.

### `getEventListFromLocalStorage()`

This method retrieves the list of tracked events from local storage in SharedPreferences. It returns a JSONArray containing the stored event data. If no data is found or an error occurs during retrieval, it returns an empty JSONArray.

### `getCurrentTime()`

This method returns the current timestamp as a long value, representing the number of milliseconds

## Usage

You can use the `AnonymousUserManager` class to manage anonymous user sessions, track events, and determine when to create known users based on predefined criteria.

```java
// Example usage
AnonymousUserManager userManager = new AnonymousUserManager();
userManager.updateAnonSession();
userManager.trackAnonEvent("UserLoggedIn", userData);
userManager.trackAnonPurchaseEvent(100.0, items, otherData);