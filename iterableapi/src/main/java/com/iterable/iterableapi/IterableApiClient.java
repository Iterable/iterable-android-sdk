package com.iterable.iterableapi;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import com.iterable.iterableapi.util.DeviceInfoUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

class IterableApiClient {
    private static final String TAG = "IterableApiClient";
    private final @NonNull AuthProvider authProvider;
    private RequestProcessor requestProcessor;

    interface AuthProvider {
        @Nullable
        String getEmail();
        @Nullable
        String getUserId();
        @Nullable
        String getUserIdAnon();
        @Nullable
        String getAuthToken();
        @Nullable
        String getApiKey();
        @Nullable
        String getDeviceId();
        @Nullable
        Context getContext();
        void resetAuth();
    }

    IterableApiClient(@NonNull AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    private RequestProcessor getRequestProcessor() {
        if (requestProcessor == null) {
            requestProcessor = new OnlineRequestProcessor();
        }
        return requestProcessor;
    }

    void setOfflineProcessingEnabled(boolean offlineMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (offlineMode) {
                if (this.requestProcessor == null || this.requestProcessor.getClass() != OfflineRequestProcessor.class) {
                    this.requestProcessor = new OfflineRequestProcessor(authProvider.getContext());
                }
            } else {
                if (this.requestProcessor == null || this.requestProcessor.getClass() != OnlineRequestProcessor.class) {
                    this.requestProcessor = new OnlineRequestProcessor();
                }
            }
        }
    }

    void getRemoteConfiguration(IterableHelper.IterableActionHandler actionHandler) {
        JSONObject requestJSON = new JSONObject();
        try {
            requestJSON.putOpt(IterableConstants.KEY_PLATFORM, IterableConstants.ITBL_PLATFORM_ANDROID);
            requestJSON.putOpt(IterableConstants.DEVICE_APP_PACKAGE_NAME, authProvider.getContext().getPackageName());
            requestJSON.put(IterableConstants.ITBL_KEY_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER);
            requestJSON.put(IterableConstants.ITBL_SYSTEM_VERSION, Build.VERSION.RELEASE);
            sendGetRequest(IterableConstants.ENDPOINT_GET_REMOTE_CONFIGURATION, requestJSON, actionHandler);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void track(@NonNull String eventName, int campaignId, int templateId, @Nullable JSONObject dataFields) {
        JSONObject requestJSON = new JSONObject();
        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_EVENT_NAME, eventName);

            if (campaignId != 0) {
                requestJSON.put(IterableConstants.KEY_CAMPAIGN_ID, campaignId);
            }
            if (templateId != 0) {
                requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId);
            }
            requestJSON.put(IterableConstants.KEY_DATA_FIELDS, dataFields);

            sendPostRequest(IterableConstants.ENDPOINT_TRACK, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void track(@NonNull String eventName, int campaignId, int templateId, @Nullable JSONObject dataFields, String createdAt) {
        JSONObject requestJSON = new JSONObject();
        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_EVENT_NAME, eventName);

            if (campaignId != 0) {
                requestJSON.put(IterableConstants.KEY_CAMPAIGN_ID, campaignId);
            }
            if (templateId != 0) {
                requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId);
            }
            if (dataFields != null) {
                dataFields.remove(IterableConstants.SHARED_PREFS_EVENT_TYPE);
                dataFields.remove(IterableConstants.KEY_EVENT_NAME);
            }
            requestJSON.put(IterableConstants.KEY_CREATED_AT, createdAt);
            requestJSON.put(IterableConstants.KEY_DATA_FIELDS, dataFields);
            requestJSON.put(IterableConstants.KEY_CREATE_NEW_FIELDS, true);
            sendPostRequest(IterableConstants.ENDPOINT_TRACK, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateCart(@NonNull List<CommerceItem> items) {
        JSONObject requestJSON = new JSONObject();

        try {
            JSONArray itemsArray = new JSONArray();
            for (CommerceItem item : items) {
                itemsArray.put(item.toJSONObject());
            }

            JSONObject userObject = new JSONObject();
            addEmailOrUserIdToJson(userObject);
            requestJSON.put(IterableConstants.KEY_USER, userObject);

            requestJSON.put(IterableConstants.KEY_ITEMS, itemsArray);

            sendPostRequest(IterableConstants.ENDPOINT_UPDATE_CART, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateCart(@NonNull List<CommerceItem> items, long createdAt) {
        JSONObject requestJSON = new JSONObject();

        try {
            JSONArray itemsArray = new JSONArray();
            for (CommerceItem item : items) {
                itemsArray.put(item.toJSONObject());
            }

            JSONObject userObject = new JSONObject();
            addEmailOrUserIdToJson(userObject);
            userObject.put(IterableConstants.KEY_PREFER_USER_ID, true);
            requestJSON.put(IterableConstants.KEY_USER, userObject);
            requestJSON.put(IterableConstants.KEY_ITEMS, itemsArray);
            requestJSON.put(IterableConstants.KEY_CREATED_AT, createdAt);

            sendPostRequest(IterableConstants.ENDPOINT_UPDATE_CART, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void trackPurchase(double total, @NonNull List<CommerceItem> items, @Nullable JSONObject dataFields, @Nullable IterableAttributionInfo attributionInfo) {
        JSONObject requestJSON = new JSONObject();
        try {
            JSONArray itemsArray = new JSONArray();
            for (CommerceItem item : items) {
                itemsArray.put(item.toJSONObject());
            }

            JSONObject userObject = new JSONObject();
            addEmailOrUserIdToJson(userObject);
            requestJSON.put(IterableConstants.KEY_USER, userObject);

            requestJSON.put(IterableConstants.KEY_ITEMS, itemsArray);
            requestJSON.put(IterableConstants.KEY_TOTAL, total);
            if (dataFields != null) {
                requestJSON.put(IterableConstants.KEY_DATA_FIELDS, dataFields);
            }

            if (attributionInfo != null) {
                requestJSON.putOpt(IterableConstants.KEY_CAMPAIGN_ID, attributionInfo.campaignId);
                requestJSON.putOpt(IterableConstants.KEY_TEMPLATE_ID, attributionInfo.templateId);
            }

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_PURCHASE, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void trackPurchase(double total, @NonNull List<CommerceItem> items, @Nullable JSONObject dataFields, @NonNull long createdAt) {
        JSONObject requestJSON = new JSONObject();
        try {
            JSONArray itemsArray = new JSONArray();
            for (CommerceItem item : items) {
                itemsArray.put(item.toJSONObject());
            }

            JSONObject userObject = new JSONObject();
            addEmailOrUserIdToJson(userObject);
            userObject.put(IterableConstants.KEY_PREFER_USER_ID, true);
            requestJSON.put(IterableConstants.KEY_USER, userObject);

            requestJSON.put(IterableConstants.KEY_ITEMS, itemsArray);
            requestJSON.put(IterableConstants.KEY_TOTAL, total);
            requestJSON.put(IterableConstants.KEY_CREATED_AT, createdAt);
            if (dataFields != null) {
                dataFields.remove(IterableConstants.SHARED_PREFS_EVENT_TYPE);
                requestJSON.put(IterableConstants.KEY_DATA_FIELDS, dataFields);
            }

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_PURCHASE, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateEmail(final @NonNull String newEmail, final @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
        JSONObject requestJSON = new JSONObject();

        try {
            if (authProvider.getEmail() != null) {
                requestJSON.put(IterableConstants.KEY_CURRENT_EMAIL, authProvider.getEmail());
            } else {
                requestJSON.put(IterableConstants.KEY_CURRENT_USERID, authProvider.getUserId());
            }
            requestJSON.put(IterableConstants.KEY_NEW_EMAIL, newEmail);

            sendPostRequest(IterableConstants.ENDPOINT_UPDATE_EMAIL, requestJSON, successHandler, failureHandler);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateUser(@NonNull JSONObject dataFields, Boolean mergeNestedObjects) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);

            // Create the user by userId if it doesn't exist
            if (authProvider.getEmail() == null && (authProvider.getUserIdAnon() != null || authProvider.getUserId() != null)) {
                requestJSON.put(IterableConstants.KEY_PREFER_USER_ID, true);
            }

            requestJSON.put(IterableConstants.KEY_DATA_FIELDS, dataFields);
            requestJSON.put(IterableConstants.KEY_MERGE_NESTED_OBJECTS, mergeNestedObjects);

            sendPostRequest(IterableConstants.ENDPOINT_UPDATE_USER, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateSubscriptions(@Nullable Integer[] emailListIds, @Nullable Integer[] unsubscribedChannelIds, @Nullable Integer[] unsubscribedMessageTypeIds, @Nullable Integer[] subscribedMessageTypeIDs, Integer campaignId, Integer templateId) {
        JSONObject requestJSON = new JSONObject();
        addEmailOrUserIdToJson(requestJSON);

        tryAddArrayToJSON(requestJSON, IterableConstants.KEY_EMAIL_LIST_IDS, emailListIds);
        tryAddArrayToJSON(requestJSON, IterableConstants.KEY_UNSUB_CHANNEL, unsubscribedChannelIds);
        tryAddArrayToJSON(requestJSON, IterableConstants.KEY_UNSUB_MESSAGE, unsubscribedMessageTypeIds);
        tryAddArrayToJSON(requestJSON, IterableConstants.KEY_SUB_MESSAGE, subscribedMessageTypeIDs);
        try {
            if (campaignId != null && campaignId != 0) {
                requestJSON.putOpt(IterableConstants.KEY_CAMPAIGN_ID, campaignId);
            }
            if (templateId != null && templateId != 0) {
                requestJSON.putOpt(IterableConstants.KEY_TEMPLATE_ID, templateId);
            }
        } catch (JSONException e) {
            IterableLogger.e(TAG, e.toString());
        }
        sendPostRequest(IterableConstants.ENDPOINT_UPDATE_USER_SUBS, requestJSON);
    }

    public void getInAppMessages(int count, @NonNull IterableHelper.IterableActionHandler onCallback) {
        JSONObject requestJSON = new JSONObject();
        addEmailOrUserIdToJson(requestJSON);
        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.ITERABLE_IN_APP_COUNT, count);
            requestJSON.put(IterableConstants.KEY_PLATFORM, DeviceInfoUtils.isFireTV(authProvider.getContext().getPackageManager()) ? IterableConstants.ITBL_PLATFORM_OTT : IterableConstants.ITBL_PLATFORM_ANDROID);
            requestJSON.put(IterableConstants.ITBL_KEY_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER);
            requestJSON.put(IterableConstants.ITBL_SYSTEM_VERSION, Build.VERSION.RELEASE);
            requestJSON.put(IterableConstants.KEY_PACKAGE_NAME, authProvider.getContext().getPackageName());

            sendGetRequest(IterableConstants.ENDPOINT_GET_INAPP_MESSAGES, requestJSON, onCallback);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void getEmbeddedMessages(@Nullable Long[] placementIds, @NonNull IterableHelper.IterableActionHandler onCallback) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_PLATFORM, IterableConstants.ITBL_PLATFORM_ANDROID);
            requestJSON.put(IterableConstants.ITBL_KEY_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER);
            requestJSON.put(IterableConstants.ITBL_SYSTEM_VERSION, Build.VERSION.RELEASE);
            requestJSON.put(IterableConstants.KEY_PACKAGE_NAME, authProvider.getContext().getPackageName());

            if (placementIds != null) {
                StringBuilder pathBuilder = new StringBuilder(IterableConstants.ENDPOINT_GET_EMBEDDED_MESSAGES + "?");

                for (Long placementId : placementIds) {
                    pathBuilder.append("&placementIds=").append(placementId);
                }

                String path = pathBuilder.toString();
                sendGetRequest(path, requestJSON, onCallback);
            } else {
                sendGetRequest(IterableConstants.ENDPOINT_GET_EMBEDDED_MESSAGES, requestJSON, onCallback);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void getEmbeddedMessages(@Nullable Long[] placementIds, @NonNull IterableHelper.SuccessHandler onSuccess, @NonNull IterableHelper.FailureHandler onFailure) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_PLATFORM, IterableConstants.ITBL_PLATFORM_ANDROID);
            requestJSON.put(IterableConstants.ITBL_KEY_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER);
            requestJSON.put(IterableConstants.ITBL_SYSTEM_VERSION, Build.VERSION.RELEASE);
            requestJSON.put(IterableConstants.KEY_PACKAGE_NAME, authProvider.getContext().getPackageName());

            if (placementIds != null) {
                StringBuilder pathBuilder = new StringBuilder(IterableConstants.ENDPOINT_GET_EMBEDDED_MESSAGES + "?");

                for (Long placementId : placementIds) {
                    pathBuilder.append("&placementIds=").append(placementId);
                }

                String path = pathBuilder.toString();
                sendGetRequest(path, requestJSON, onSuccess, onFailure);
            } else {
                sendGetRequest(IterableConstants.ENDPOINT_GET_EMBEDDED_MESSAGES, requestJSON, onSuccess, onFailure);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void trackInAppOpen(@NonNull String messageId) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId);

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_OPEN, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void trackInAppOpen(@NonNull IterableInAppMessage message, @NonNull IterableInAppLocation location, @Nullable String inboxSessionId) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, message.getMessageId());
            requestJSON.put(IterableConstants.KEY_MESSAGE_CONTEXT, getInAppMessageContext(message, location));
            requestJSON.put(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson());
            if (location == IterableInAppLocation.INBOX) {
                addInboxSessionID(requestJSON, inboxSessionId);
            }
            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_OPEN, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void trackInAppClick(@NonNull String messageId, @NonNull String clickedUrl) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId);
            requestJSON.put(IterableConstants.ITERABLE_IN_APP_CLICKED_URL, clickedUrl);

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void trackInAppClick(@NonNull IterableInAppMessage message, @NonNull String clickedUrl, @NonNull IterableInAppLocation clickLocation, @Nullable String inboxSessionId) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, message.getMessageId());
            requestJSON.put(IterableConstants.ITERABLE_IN_APP_CLICKED_URL, clickedUrl);
            requestJSON.put(IterableConstants.KEY_MESSAGE_CONTEXT, getInAppMessageContext(message, clickLocation));
            requestJSON.put(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson());
            if (clickLocation == IterableInAppLocation.INBOX) {
                addInboxSessionID(requestJSON, inboxSessionId);
            }
            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void trackEmbeddedClick(@NonNull IterableEmbeddedMessage message, @Nullable String buttonIdentifier, @Nullable String clickedUrl) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, message.getMetadata().getMessageId());
            requestJSON.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_IDENTIFIER, buttonIdentifier);
            requestJSON.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_TARGET_URL, clickedUrl);
            requestJSON.put(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson());

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_EMBEDDED_CLICK, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void trackInAppClose(@NonNull IterableInAppMessage message, @Nullable String clickedURL, @NonNull IterableInAppCloseAction closeAction, @NonNull IterableInAppLocation clickLocation, @Nullable String inboxSessionId) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
//            requestJSON.put(IterableConstants.KEY_EMAIL, authProvider.getEmail()); // not needed due to addEmailOrUserIdToJson(requestJSON)?
//            requestJSON.put(IterableConstants.KEY_USER_ID, authProvider.getUserId()); // not needed due to addEmailOrUserIdToJson(requestJSON)?
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, message.getMessageId());
            requestJSON.putOpt(IterableConstants.ITERABLE_IN_APP_CLICKED_URL, clickedURL);
            requestJSON.put(IterableConstants.ITERABLE_IN_APP_CLOSE_ACTION, closeAction.toString());
            requestJSON.put(IterableConstants.KEY_MESSAGE_CONTEXT, getInAppMessageContext(message, clickLocation));
            requestJSON.put(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson());

            if (clickLocation == IterableInAppLocation.INBOX) {
                addInboxSessionID(requestJSON, inboxSessionId);
            }

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_CLOSE, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void trackInAppDelivery(@NonNull IterableInAppMessage message) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, message.getMessageId());
            requestJSON.put(IterableConstants.KEY_MESSAGE_CONTEXT, getInAppMessageContext(message, null));
            requestJSON.put(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson());

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_DELIVERY, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void trackEmbeddedMessageReceived(@NonNull IterableEmbeddedMessage message) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, message.getMetadata().getMessageId());
            requestJSON.put(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson());
            sendPostRequest(IterableConstants.ENDPOINT_TRACK_EMBEDDED_RECEIVED, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void inAppConsume(@NonNull IterableInAppMessage message, @Nullable IterableInAppDeleteActionType source, @Nullable IterableInAppLocation clickLocation, @Nullable String inboxSessionId, @Nullable final IterableHelper.SuccessHandler successHandler, @Nullable final IterableHelper.FailureHandler failureHandler) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, message.getMessageId());
            if (source != null) {
                requestJSON.put(IterableConstants.ITERABLE_IN_APP_DELETE_ACTION, source.toString());
            }

            if (clickLocation != null) {
                requestJSON.put(IterableConstants.KEY_MESSAGE_CONTEXT, getInAppMessageContext(message, clickLocation));
                requestJSON.put(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson());
            }

            if (clickLocation == IterableInAppLocation.INBOX) {
                addInboxSessionID(requestJSON, inboxSessionId);
            }

            sendPostRequest(IterableConstants.ENDPOINT_INAPP_CONSUME, requestJSON, successHandler, failureHandler);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void trackInboxSession(@NonNull IterableInboxSession session, @Nullable String inboxSessionId) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);

            requestJSON.put(IterableConstants.ITERABLE_INBOX_SESSION_START, session.sessionStartTime.getTime());
            requestJSON.put(IterableConstants.ITERABLE_INBOX_SESSION_END, session.sessionEndTime.getTime());
            requestJSON.put(IterableConstants.ITERABLE_INBOX_START_TOTAL_MESSAGE_COUNT, session.startTotalMessageCount);
            requestJSON.put(IterableConstants.ITERABLE_INBOX_START_UNREAD_MESSAGE_COUNT, session.startUnreadMessageCount);
            requestJSON.put(IterableConstants.ITERABLE_INBOX_END_TOTAL_MESSAGE_COUNT, session.endTotalMessageCount);
            requestJSON.put(IterableConstants.ITERABLE_INBOX_END_UNREAD_MESSAGE_COUNT, session.endUnreadMessageCount);

            if (session.impressions != null) {
                JSONArray impressionsJsonArray = new JSONArray();
                for (IterableInboxSession.Impression impression : session.impressions) {
                    JSONObject impressionJson = new JSONObject();
                    impressionJson.put(IterableConstants.KEY_MESSAGE_ID, impression.messageId);
                    impressionJson.put(IterableConstants.ITERABLE_IN_APP_SILENT_INBOX, impression.silentInbox);
                    impressionJson.put(IterableConstants.ITERABLE_INBOX_IMP_DISPLAY_COUNT, impression.displayCount);
                    impressionJson.put(IterableConstants.ITERABLE_INBOX_IMP_DISPLAY_DURATION, impression.duration);
                    impressionsJsonArray.put(impressionJson);
                }
                requestJSON.put(IterableConstants.ITERABLE_INBOX_IMPRESSIONS, impressionsJsonArray);
            }

            requestJSON.putOpt(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson());
            addInboxSessionID(requestJSON, inboxSessionId);

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INBOX_SESSION, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void trackEmbeddedSession(@NonNull IterableEmbeddedSession session) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);

            JSONObject sessionJson = new JSONObject();
            if (session.getId() != null) {
                sessionJson.put(IterableConstants.KEY_EMBEDDED_SESSION_ID, session.getId());
            }
            sessionJson.put(IterableConstants.ITERABLE_EMBEDDED_SESSION_START, session.getStart().getTime());
            sessionJson.put(IterableConstants.ITERABLE_EMBEDDED_SESSION_END, session.getEnd().getTime());

            requestJSON.put(IterableConstants.ITERABLE_EMBEDDED_SESSION, sessionJson);

            if (session.getImpressions() != null) {
                JSONArray impressionsJsonArray = new JSONArray();
                for (IterableEmbeddedImpression impression : session.getImpressions()) {
                    JSONObject impressionJson = new JSONObject();
                    impressionJson.put(IterableConstants.KEY_MESSAGE_ID, impression.getMessageId());
                    impressionJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENT_ID, impression.getPlacementId());
                    impressionJson.put(IterableConstants.ITERABLE_EMBEDDED_IMP_DISPLAY_COUNT, impression.getDisplayCount());
                    impressionJson.put(IterableConstants.ITERABLE_EMBEDDED_IMP_DISPLAY_DURATION, impression.getDuration());
                    impressionsJsonArray.put(impressionJson);
                }
                requestJSON.put(IterableConstants.ITERABLE_EMBEDDED_IMPRESSIONS, impressionsJsonArray);
            }

            requestJSON.putOpt(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson());

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_EMBEDDED_SESSION, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void trackPushOpen(int campaignId, int templateId, @NonNull String messageId, @Nullable JSONObject dataFields) {
        JSONObject requestJSON = new JSONObject();

        try {
            if (dataFields == null) {
                dataFields = new JSONObject();
            }

            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_CAMPAIGN_ID, campaignId);
            requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId);
            requestJSON.putOpt(IterableConstants.KEY_DATA_FIELDS, dataFields);

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_PUSH_OPEN, requestJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void disableToken(@Nullable String email, @Nullable String userId, @Nullable String authToken, @NonNull String deviceToken, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        JSONObject requestJSON = new JSONObject();
        try {
            requestJSON.put(IterableConstants.KEY_TOKEN, deviceToken);
            if (email != null) {
                requestJSON.put(IterableConstants.KEY_EMAIL, email);
            } else if (userId != null) {
                requestJSON.put(IterableConstants.KEY_USER_ID, userId);
            }

            sendPostRequest(IterableConstants.ENDPOINT_DISABLE_DEVICE, requestJSON, authToken, onSuccess, onFailure);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void registerDeviceToken(@Nullable String email, @Nullable String userId, @Nullable String authToken, @NonNull String applicationName, @NonNull String deviceToken, @Nullable JSONObject dataFields, HashMap<String, String> deviceAttributes, @Nullable final IterableHelper.SuccessHandler successHandler, @Nullable final IterableHelper.FailureHandler failureHandler) {
        Context context = authProvider.getContext();
        JSONObject requestJSON = new JSONObject();
        try {
            addEmailOrUserIdToJson(requestJSON);

            if (dataFields == null) {
                dataFields = new JSONObject();
            }

            for (HashMap.Entry<String, String> entry : deviceAttributes.entrySet()) {
                dataFields.put(entry.getKey(), entry.getValue());
            }

            dataFields.put(IterableConstants.FIREBASE_TOKEN_TYPE, IterableConstants.MESSAGING_PLATFORM_FIREBASE);
            dataFields.put(IterableConstants.FIREBASE_COMPATIBLE, true);
            DeviceInfoUtils.populateDeviceDetails(dataFields, context, authProvider.getDeviceId());
            dataFields.put(IterableConstants.DEVICE_NOTIFICATIONS_ENABLED, NotificationManagerCompat.from(context).areNotificationsEnabled());

            JSONObject device = new JSONObject();
            device.put(IterableConstants.KEY_TOKEN, deviceToken);
            device.put(IterableConstants.KEY_PLATFORM, IterableConstants.MESSAGING_PLATFORM_GOOGLE);
            device.put(IterableConstants.KEY_APPLICATION_NAME, applicationName);
            device.putOpt(IterableConstants.KEY_DATA_FIELDS, dataFields);
            requestJSON.put(IterableConstants.KEY_DEVICE, device);

            // Create the user by userId if it doesn't exist
            if (email == null && userId != null) {
                requestJSON.put(IterableConstants.KEY_PREFER_USER_ID, true);
            }

            sendPostRequest(IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN, requestJSON, authToken, successHandler, failureHandler);
        } catch (JSONException e) {
            IterableLogger.e(TAG, "registerDeviceToken: exception", e);
        }
    }

    /**
     * Adds the current email or userID to the json request.
     * @param requestJSON
     */
    private void addEmailOrUserIdToJson(JSONObject requestJSON) {
        try {
            if (authProvider.getEmail() != null) {
                requestJSON.put(IterableConstants.KEY_EMAIL, authProvider.getEmail());
            } else {
                if (authProvider.getUserIdAnon() != null) {
                    requestJSON.put(IterableConstants.KEY_USER_ID, authProvider.getUserIdAnon());
                } else {
                    requestJSON.put(IterableConstants.KEY_USER_ID, authProvider.getUserId());
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addInboxSessionID(@NonNull JSONObject requestJSON, @Nullable String inboxSessionId) throws JSONException {
        if (inboxSessionId != null) {
            requestJSON.put(IterableConstants.KEY_INBOX_SESSION_ID, inboxSessionId);
        }
    }

    private JSONObject getInAppMessageContext(@NonNull IterableInAppMessage message, @Nullable IterableInAppLocation location) {
        JSONObject messageContext = new JSONObject();
        try {
            boolean isSilentInbox = message.isSilentInboxMessage();

            messageContext.putOpt(IterableConstants.ITERABLE_IN_APP_SAVE_TO_INBOX, message.isInboxMessage());
            messageContext.putOpt(IterableConstants.ITERABLE_IN_APP_SILENT_INBOX, isSilentInbox);
            if (location != null) {
                messageContext.putOpt(IterableConstants.ITERABLE_IN_APP_LOCATION, location.toString());
            }
        } catch (Exception e) {
            IterableLogger.e(TAG, "Could not populate messageContext JSON", e);
        }
        return messageContext;
    }

    @NonNull
    private JSONObject getDeviceInfoJson() {
        JSONObject deviceInfo = new JSONObject();
        try {
            deviceInfo.putOpt(IterableConstants.DEVICE_ID, authProvider.getDeviceId());
            deviceInfo.putOpt(IterableConstants.KEY_PLATFORM, IterableConstants.ITBL_PLATFORM_ANDROID);
            deviceInfo.putOpt(IterableConstants.DEVICE_APP_PACKAGE_NAME, authProvider.getContext().getPackageName());
        } catch (Exception e) {
            IterableLogger.e(TAG, "Could not populate deviceInfo JSON", e);
        }
        return deviceInfo;
    }

    /**
     * Attempts to add an array as a JSONArray to a JSONObject
     * @param requestJSON
     * @param key
     * @param value
     */
    void tryAddArrayToJSON(JSONObject requestJSON, String key, Object[] value) {
        if (requestJSON != null && key != null && value != null)
            try {
                JSONArray mJSONArray = new JSONArray(Arrays.asList(value));
                requestJSON.put(key, mJSONArray);
            } catch (JSONException e) {
                IterableLogger.e(TAG, e.toString());
            }
    }

    /**
     * Sends the POST request to Iterable.
     * Performs network operations on an async thread instead of the main thread.
     * @param resourcePath
     * @param json
     */
    void sendPostRequest(@NonNull String resourcePath, @NonNull JSONObject json) {
        sendPostRequest(resourcePath, json, authProvider.getAuthToken());
    }

    void sendPostRequest(@NonNull String resourcePath, @NonNull JSONObject json, @Nullable String authToken) {
        sendPostRequest(resourcePath, json, authToken, null, null);
    }

    void sendPostRequest(@NonNull String resourcePath, @NonNull JSONObject json, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        sendPostRequest(resourcePath, json, authProvider.getAuthToken(), onSuccess, onFailure);
    }

    void sendPostRequest(@NonNull String resourcePath, @NonNull JSONObject json, @Nullable String authToken, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        getRequestProcessor().processPostRequest(authProvider.getApiKey(), resourcePath, json, authToken, onSuccess, onFailure);
    }

    /**
     * Sends a GET request to Iterable.
     * Performs network operations on an async thread instead of the main thread.
     * @param resourcePath
     * @param json
     */
    void sendGetRequest(@NonNull String resourcePath, @NonNull JSONObject json, @Nullable IterableHelper.IterableActionHandler onCallback) {
        getRequestProcessor().processGetRequest(authProvider.getApiKey(), resourcePath, json, authProvider.getAuthToken(), onCallback);
    }

    void sendGetRequest(@NonNull String resourcePath, @NonNull JSONObject json, @NonNull IterableHelper.SuccessHandler onSuccess, @NonNull IterableHelper.FailureHandler onFailure) {
        getRequestProcessor().processGetRequest(authProvider.getApiKey(), resourcePath, json, authProvider.getAuthToken(), onSuccess, onFailure);
    }

    void onLogout() {
        getRequestProcessor().onLogout(authProvider.getContext());
        authProvider.resetAuth();
    }

    void mergeUser(String sourceEmail, String sourceUserId, String destinationEmail, String destinationUserId, @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
        JSONObject requestJson = new JSONObject();
        try {
            if (sourceEmail != null && !sourceEmail.isEmpty()) {
                requestJson.put(IterableConstants.SOURCE_EMAIL, sourceEmail);
            }
            if (sourceUserId != null && !sourceUserId.isEmpty()) {
                requestJson.put(IterableConstants.SOURCE_USER_ID, sourceUserId);
            }
            if (destinationEmail != null && !destinationEmail.isEmpty()) {
                requestJson.put(IterableConstants.DESTINATION_EMAIL, destinationEmail);
            }
            if (destinationUserId != null && !destinationUserId.isEmpty()) {
                requestJson.put(IterableConstants.DESTINATION_USER_ID, destinationUserId);
            }
            sendPostRequest(IterableConstants.ENDPOINT_MERGE_USER, requestJson, successHandler, failureHandler);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void getCriteriaList(@Nullable IterableHelper.IterableActionHandler actionHandler) {
        sendGetRequest(IterableConstants.ENDPOINT_CRITERIA_LIST, new JSONObject(), actionHandler);
    }

    void trackAnonSession(long createdAt, String userId, @NonNull JSONObject requestJson, JSONObject updateUserTrack, @NonNull IterableHelper.SuccessHandler onSuccess, @NonNull IterableHelper.FailureHandler onFailure) {
        try {
            JSONObject requestObject = new JSONObject();

            JSONObject userObject = new JSONObject();
            userObject.put(IterableConstants.KEY_USER_ID, userId);
            userObject.put(IterableConstants.KEY_PREFER_USER_ID, true);
            userObject.put(IterableConstants.KEY_MERGE_NESTED_OBJECTS, true);
            userObject.put(IterableConstants.KEY_CREATE_NEW_FIELDS, true);
            if (updateUserTrack != null) {
                userObject.put(IterableConstants.KEY_DATA_FIELDS, updateUserTrack);
            }
            requestObject.put(IterableConstants.KEY_USER, userObject);
            requestObject.put(IterableConstants.KEY_CREATED_AT, createdAt);
            requestObject.put(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson());
            requestObject.put(IterableConstants.KEY_ANON_SESSION_CONTEXT, requestJson);
            sendPostRequest(IterableConstants.ENDPOINT_TRACK_ANON_SESSION, requestObject, onSuccess, onFailure);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
