package com.iterable.iterableapi;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by davidtruong on 5/23/16.
 */
class IterableNotificationData {
        private int campaignId;
        private int templateId;
        private boolean isGhostPush;

    IterableNotificationData(String data){
        try {
            JSONObject iterableJson = new JSONObject(data);
            if (iterableJson.has(IterableConstants.KEY_CAMPAIGNID)){
                campaignId = iterableJson.getInt(IterableConstants.KEY_CAMPAIGNID);
            }

            if (iterableJson.has(IterableConstants.KEY_TEMPLATE_ID)) {
                templateId = iterableJson.getInt(IterableConstants.KEY_TEMPLATE_ID);
            }

            if (iterableJson.has(IterableConstants.IS_GHOST_PUSH)) {
                isGhostPush = iterableJson.getBoolean(IterableConstants.IS_GHOST_PUSH);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public int getCampaignId()
    {
        return this.campaignId;
    }

    public int getTemplateId()
    {
        return this.templateId;
    }

    public boolean getIsGhostPush()
    {
        return this.isGhostPush;
    }
}

