package com.iterable.iterableapi

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import org.json.JSONException
import org.json.JSONObject

/**
 *  [IterableAction] represents an action defined as a response to user events.
 *  It is currently used in push notification actions (open push &amp; action buttons).
 */
class IterableAction private constructor(config: JSONObject?) {

    companion object {
        /** Open the URL or deep link */
        const val ACTION_TYPE_OPEN_URL = "openUrl"

        @Nullable
        fun from(@Nullable config: JSONObject?): IterableAction? {
            return if (config != null) {
                IterableAction(config)
            } else {
                null
            }
        }

        @Nullable
        fun actionOpenUrl(@Nullable url: String?): IterableAction? {
            return if (url != null) {
                try {
                    val config = JSONObject()
                    config.put("type", ACTION_TYPE_OPEN_URL)
                    config.put("data", url)
                    IterableAction(config)
                } catch (ignored: JSONException) {
                    null
                }
            } else {
                null
            }
        }

        @Nullable
        fun actionCustomAction(@NonNull customActionName: String): IterableAction? {
            return try {
                val config = JSONObject()
                config.put("type", customActionName)
                IterableAction(config)
            } catch (ignored: JSONException) {
                null
            }
        }
    }

    private val config: JSONObject = config ?: JSONObject()

    /** The text response typed by the user */
    @Nullable
    var userInput: String? = null

    /**
     * If [ACTION_TYPE_OPEN_URL], the SDK will call [IterableUrlHandler] and then try to
     * open the URL if the delegate returned `false` or was not set.
     *
     * For other types, [IterableCustomActionHandler] will be called.
     * @return Action type
     */
    @Nullable
    fun getType(): String? {
        return config.optString("type", null)
    }

    /**
     * Additional data, its content depends on the action type
     * @return Additional data
     */
    @Nullable
    fun getData(): String? {
        return config.optString("data", null)
    }

    /**
     * Checks whether this action is of a specific type
     * @param type Action type to match against
     * @return Boolean indicating whether the action type matches the one passed to this method
     */
    fun isOfType(@NonNull type: String): Boolean {
        return this.getType() != null && this.getType() == type
    }
}
