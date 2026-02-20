package com.iterable.iterableapi.ui.embedded

import android.os.Bundle
import com.iterable.iterableapi.IterableEmbeddedMessage
import com.iterable.iterableapi.IterableLogger
import org.json.JSONException
import org.json.JSONObject

internal object IterableEmbeddedViewArguments {
    
    private const val TAG = "IterableEmbeddedViewArgs"
    
    // Argument keys
    private const val KEY_VIEW_TYPE = "view_type"
    private const val KEY_MESSAGE_JSON = "message_json"
    private const val KEY_BG_COLOR = "bg_color"
    private const val KEY_BORDER_COLOR = "border_color"
    private const val KEY_BORDER_WIDTH = "border_width"
    private const val KEY_BORDER_RADIUS = "border_radius"
    private const val KEY_PRIMARY_BTN_BG = "primary_btn_bg"
    private const val KEY_PRIMARY_BTN_TEXT = "primary_btn_text"
    private const val KEY_SECONDARY_BTN_BG = "secondary_btn_bg"
    private const val KEY_SECONDARY_BTN_TEXT = "secondary_btn_text"
    private const val KEY_TITLE_COLOR = "title_color"
    private const val KEY_BODY_COLOR = "body_color"

    fun toBundle(
        viewType: IterableEmbeddedViewType,
        message: IterableEmbeddedMessage,
        config: IterableEmbeddedViewConfig?
    ): Bundle {
        return Bundle().apply {
            putString(KEY_VIEW_TYPE, viewType.name)
            putString(KEY_MESSAGE_JSON, IterableEmbeddedMessage.toJSONObject(message).toString())
            putConfig(config)
        }
    }

    fun getViewType(arguments: Bundle): IterableEmbeddedViewType {
        val viewTypeName = arguments.getString(KEY_VIEW_TYPE)
        return viewTypeName?.let {
            try {
                IterableEmbeddedViewType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                IterableLogger.e(TAG, "Invalid view type: $it, defaulting to BANNER")
                IterableEmbeddedViewType.BANNER
            }
        } ?: IterableEmbeddedViewType.BANNER
    }

    fun getMessage(arguments: Bundle): IterableEmbeddedMessage {
        val messageJsonString = arguments.getString(KEY_MESSAGE_JSON)
        return if (messageJsonString != null) {
            try {
                val messageJson = JSONObject(messageJsonString)
                IterableEmbeddedMessage.fromJSONObject(messageJson)
            } catch (e: JSONException) {
                IterableLogger.e(TAG, "Failed to parse message JSON", e)
                throw IllegalStateException(
                    "IterableEmbeddedView failed to restore message from saved state. Use newInstance() factory method to create this fragment."
                )
            }
        } else {
            throw IllegalStateException(
                "IterableEmbeddedView requires a message argument. Use newInstance() factory method to create this fragment."
            )
        }
    }

    fun getConfig(arguments: Bundle): IterableEmbeddedViewConfig? {
        // Check if any config properties exist
        val hasConfig = arguments.containsKey(KEY_BG_COLOR) ||
                       arguments.containsKey(KEY_BORDER_COLOR) ||
                       arguments.containsKey(KEY_BORDER_WIDTH) ||
                       arguments.containsKey(KEY_BORDER_RADIUS) ||
                       arguments.containsKey(KEY_PRIMARY_BTN_BG) ||
                       arguments.containsKey(KEY_PRIMARY_BTN_TEXT) ||
                       arguments.containsKey(KEY_SECONDARY_BTN_BG) ||
                       arguments.containsKey(KEY_SECONDARY_BTN_TEXT) ||
                       arguments.containsKey(KEY_TITLE_COLOR) ||
                       arguments.containsKey(KEY_BODY_COLOR)

        return if (hasConfig) {
            IterableEmbeddedViewConfig(
                backgroundColor = arguments.getIntOrNull(KEY_BG_COLOR),
                borderColor = arguments.getIntOrNull(KEY_BORDER_COLOR),
                borderWidth = arguments.getIntOrNull(KEY_BORDER_WIDTH),
                borderCornerRadius = arguments.getFloatOrNull(KEY_BORDER_RADIUS),
                primaryBtnBackgroundColor = arguments.getIntOrNull(KEY_PRIMARY_BTN_BG),
                primaryBtnTextColor = arguments.getIntOrNull(KEY_PRIMARY_BTN_TEXT),
                secondaryBtnBackgroundColor = arguments.getIntOrNull(KEY_SECONDARY_BTN_BG),
                secondaryBtnTextColor = arguments.getIntOrNull(KEY_SECONDARY_BTN_TEXT),
                titleTextColor = arguments.getIntOrNull(KEY_TITLE_COLOR),
                bodyTextColor = arguments.getIntOrNull(KEY_BODY_COLOR)
            )
        } else {
            null
        }
    }

    private fun Bundle.putConfig(config: IterableEmbeddedViewConfig?) {
        config?.let { cfg ->
            cfg.backgroundColor?.let { putInt(KEY_BG_COLOR, it) }
            cfg.borderColor?.let { putInt(KEY_BORDER_COLOR, it) }
            cfg.borderWidth?.let { putInt(KEY_BORDER_WIDTH, it) }
            cfg.borderCornerRadius?.let { putFloat(KEY_BORDER_RADIUS, it) }
            cfg.primaryBtnBackgroundColor?.let { putInt(KEY_PRIMARY_BTN_BG, it) }
            cfg.primaryBtnTextColor?.let { putInt(KEY_PRIMARY_BTN_TEXT, it) }
            cfg.secondaryBtnBackgroundColor?.let { putInt(KEY_SECONDARY_BTN_BG, it) }
            cfg.secondaryBtnTextColor?.let { putInt(KEY_SECONDARY_BTN_TEXT, it) }
            cfg.titleTextColor?.let { putInt(KEY_TITLE_COLOR, it) }
            cfg.bodyTextColor?.let { putInt(KEY_BODY_COLOR, it) }
        }
    }

    private fun Bundle.getIntOrNull(key: String): Int? {
        return if (containsKey(key)) getInt(key) else null
    }

    private fun Bundle.getFloatOrNull(key: String): Float? {
        return if (containsKey(key)) getFloat(key) else null
    }
}
