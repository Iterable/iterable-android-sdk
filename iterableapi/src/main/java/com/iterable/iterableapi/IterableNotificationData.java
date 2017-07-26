package com.iterable.iterableapi;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by davidtruong on 5/23/16.
 */
class IterableNotificationData {
    static final String TAG = "IterableNoticationData";

    private int campaignId;
    private int templateId;
    private String messageId;
    private boolean isGhostPush;

    /**
     * Creates the notification data from a string
     * @param data
     */
    IterableNotificationData(String data){
        try {
            JSONObject iterableJson = new JSONObject(data);
            if (iterableJson.has(IterableConstants.KEY_CAMPAIGN_ID)){
                campaignId = iterableJson.getInt(IterableConstants.KEY_CAMPAIGN_ID);
            }

            if (iterableJson.has(IterableConstants.KEY_TEMPLATE_ID)) {
                templateId = iterableJson.getInt(IterableConstants.KEY_TEMPLATE_ID);
            }

            if (iterableJson.has(IterableConstants.KEY_MESSAGE_ID)) {
                messageId = iterableJson.getString(IterableConstants.KEY_MESSAGE_ID);
            }

            if (iterableJson.has(IterableConstants.IS_GHOST_PUSH)) {
                isGhostPush = iterableJson.getBoolean(IterableConstants.IS_GHOST_PUSH);
            }
        } catch (JSONException e) {
            IterableLogger.e(TAG, e.toString());
        }
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
}

