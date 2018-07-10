package com.iterable.iterableapi;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by davidtruong on 5/23/16.
 */
class IterableNotificationData {
    static final String TAG = "IterableNoticationData";

    private int campaignId;
    private int templateId;
    private String messageId;
    private boolean isGhostPush;
    private IterableAction defaultAction;
    private List<Button> actionButtons;

    /**
     * Creates the notification data from a string
     * @param data
     */
    IterableNotificationData(@Nullable String data){
        try {
            JSONObject iterableJson = new JSONObject(data);
            campaignId = iterableJson.optInt(IterableConstants.KEY_CAMPAIGN_ID);
            templateId = iterableJson.optInt(IterableConstants.KEY_TEMPLATE_ID);
            messageId = iterableJson.optString(IterableConstants.KEY_MESSAGE_ID);
            isGhostPush = iterableJson.optBoolean(IterableConstants.IS_GHOST_PUSH);

            // Default action
            defaultAction = IterableAction.from(iterableJson.optJSONObject(IterableConstants.ITERABLE_DATA_DEFAULT_ACTION));

            // Action buttons
            JSONArray actionButtonsJson = iterableJson.optJSONArray(IterableConstants.ITERABLE_DATA_ACTION_BUTTONS);
            if (actionButtonsJson != null) {
                actionButtons = new ArrayList<Button>();
                for (int i = 0; i < actionButtonsJson.length(); i++) {
                    JSONObject button = actionButtonsJson.getJSONObject(i);
                    actionButtons.add(new Button(button));
                }
            }
        } catch (JSONException e) {
            IterableLogger.e(TAG, e.toString());
        }
    }

    IterableNotificationData(@NonNull Bundle extras) {
        this(extras.getString(IterableConstants.ITERABLE_DATA_KEY));
    }

    /**
     * Returns the campaignId
     * @return
     */
    public int getCampaignId()
    {
        return this.campaignId;
    }

    /**
     * Returns the templateId
     * @return
     */
    public int getTemplateId()
    {
        return this.templateId;
    }

    /**
     * Returns the messageId
     * @return
     */
    public String getMessageId() { return this.messageId; }

    /**
     * Returns if the notification is a ghost/silent push notification
     * @return
     */
    public boolean getIsGhostPush()
    {
        return this.isGhostPush;
    }

    public @Nullable IterableAction getDefaultAction() {
        return defaultAction;
    }

    public @Nullable List<Button> getActionButtons() {
        return actionButtons;
    }

    public @Nullable Button getActionButton(String actionIdentifier) {
        for (Button button : actionButtons) {
            if (button.identifier.equals(actionIdentifier))
                return button;
        }
        return null;
    }

    public static class Button {
        public static final String BUTTON_TYPE_DEFAULT = "default";
        public static final String BUTTON_TYPE_DESTRUCTIVE = "destructive";
        public static final String BUTTON_TYPE_TEXT_INPUT = "textInput";

        public final String identifier;
        public final String title;
        public final String buttonType;
        public final boolean openApp;
        public final boolean requiresUnlock;
        public final int buttonIcon;
        public final String inputPlaceholder;
        public final String inputTitle;
        public final IterableAction action;

        public Button(@NonNull JSONObject buttonData) {
            identifier = buttonData.optString(IterableConstants.ITBL_BUTTON_IDENTIFIER);
            title = buttonData.optString(IterableConstants.ITBL_BUTTON_TITLE);
            buttonType = buttonData.optString(IterableConstants.ITBL_BUTTON_TYPE, BUTTON_TYPE_DEFAULT);
            openApp = buttonData.optBoolean(IterableConstants.ITBL_BUTTON_OPEN_APP, true);
            requiresUnlock = buttonData.optBoolean(IterableConstants.ITBL_BUTTON_REQUIRES_UNLOCK, true);
            buttonIcon = buttonData.optInt(IterableConstants.ITBL_BUTTON_ICON, 0);
            inputPlaceholder = buttonData.optString(IterableConstants.ITBL_BUTTON_INPUT_PLACEHOLDER);
            inputTitle = buttonData.optString(IterableConstants.ITBL_BUTTON_INPUT_TITLE);
            action = IterableAction.from(buttonData.optJSONObject(IterableConstants.ITBL_BUTTON_ACTION));
        }
    }
}

