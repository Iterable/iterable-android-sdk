package com.iterable.iterableapi

import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList

/**
 * Created by davidtruong on 5/23/16.
 */
class IterableNotificationData {
    
    companion object {
        const val TAG = "IterableNoticationData"
    }

    internal var campaignId: Int = 0
    internal var templateId: Int = 0
    internal var messageId: String? = null
    internal var isGhostPush: Boolean = false
    internal var defaultAction: IterableAction? = null
    internal var actionButtons: List<Button>? = null

    /**
     * Creates the notification data from a string
     * @param data
     */
    constructor(data: String?) {
        try {
            val iterableJson = JSONObject(data ?: "")
            campaignId = iterableJson.optInt(IterableConstants.KEY_CAMPAIGN_ID)
            templateId = iterableJson.optInt(IterableConstants.KEY_TEMPLATE_ID)
            messageId = iterableJson.optString(IterableConstants.KEY_MESSAGE_ID)
            isGhostPush = iterableJson.optBoolean(IterableConstants.IS_GHOST_PUSH)

            // Default action
            defaultAction = IterableAction.from(iterableJson.optJSONObject(IterableConstants.ITERABLE_DATA_DEFAULT_ACTION))

            // Action buttons
            val actionButtonsJson = iterableJson.optJSONArray(IterableConstants.ITERABLE_DATA_ACTION_BUTTONS)
            if (actionButtonsJson != null) {
                actionButtons = ArrayList()
                for (i in 0 until actionButtonsJson.length()) {
                    val button = actionButtonsJson.getJSONObject(i)
                    (actionButtons as ArrayList).add(Button(button))
                }
            }
        } catch (e: JSONException) {
            IterableLogger.e(TAG, e.toString())
        }
    }

    constructor(@NonNull extras: Bundle) : this(extras.getString(IterableConstants.ITERABLE_DATA_KEY))

    /**
     * Returns the campaignId
     * @return
     */
    fun getCampaignId(): Int {
        return this.campaignId
    }

    /**
     * Returns the templateId
     * @return
     */
    fun getTemplateId(): Int {
        return this.templateId
    }

    /**
     * Returns the messageId
     * @return
     */
    fun getMessageId(): String? {
        return this.messageId
    }

    /**
     * Returns if the notification is a ghost/silent push notification
     * @return
     */
    fun getIsGhostPush(): Boolean {
        return this.isGhostPush
    }

    @Nullable
    fun getDefaultAction(): IterableAction? {
        return defaultAction
    }

    @Nullable
    fun getActionButtons(): List<Button>? {
        return actionButtons
    }

    @Nullable
    fun getActionButton(actionIdentifier: String): Button? {
        if (actionButtons != null) {
            for (button in actionButtons!!) {
                if (button.identifier == actionIdentifier)
                    return button
            }
        }
        return null
    }

    class Button(@NonNull buttonData: JSONObject) {
        
        companion object {
            const val BUTTON_TYPE_DEFAULT = "default"
            const val BUTTON_TYPE_DESTRUCTIVE = "destructive"
            const val BUTTON_TYPE_TEXT_INPUT = "textInput"
        }

        val identifier: String = buttonData.optString(IterableConstants.ITBL_BUTTON_IDENTIFIER)
        val title: String = buttonData.optString(IterableConstants.ITBL_BUTTON_TITLE)
        val buttonType: String = buttonData.optString(IterableConstants.ITBL_BUTTON_TYPE, BUTTON_TYPE_DEFAULT)
        val openApp: Boolean = buttonData.optBoolean(IterableConstants.ITBL_BUTTON_OPEN_APP, true)
        val requiresUnlock: Boolean = buttonData.optBoolean(IterableConstants.ITBL_BUTTON_REQUIRES_UNLOCK, true)
        val buttonIcon: Int = buttonData.optInt(IterableConstants.ITBL_BUTTON_ICON, 0)
        val inputPlaceholder: String = buttonData.optString(IterableConstants.ITBL_BUTTON_INPUT_PLACEHOLDER)
        val inputTitle: String = buttonData.optString(IterableConstants.ITBL_BUTTON_INPUT_TITLE)
        val action: IterableAction? = IterableAction.from(buttonData.optJSONObject(IterableConstants.ITBL_BUTTON_ACTION))
    }
}

