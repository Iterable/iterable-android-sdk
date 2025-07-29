package com.iterable.integration.tests

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampaignTriggerIntegrationTest : BaseIntegrationTest() {
    
    companion object {
        private const val TAG = "CampaignTriggerIntegrationTest"
        
        // Test campaign IDs - these should be configured in your Iterable project
        private const val TEST_INAPP_CAMPAIGN_ID = 14332357 // Example campaign ID from your curl command
        private const val TEST_PUSH_CAMPAIGN_ID = 14332358 // Example push campaign ID
        private const val TEST_EMBEDDED_CAMPAIGN_ID = 14332359 // Example embedded campaign ID
        
        // Test user email
        private const val TEST_USER_EMAIL = "akshay.ayyanchira@iterable.com"
    }
    
    @Test
    fun testInAppCampaignTriggerViaAPI() {
        Log.d(TAG, "Testing in-app campaign trigger via API")
        
        // Reset test states
        testUtils.resetTestStates()
        
        // Trigger the campaign via API with callback
        var campaignTriggered = false
        triggerCampaignViaAPI(TEST_INAPP_CAMPAIGN_ID, TEST_USER_EMAIL) { success ->
            campaignTriggered = success
        }
        
        // Wait for campaign to be triggered
        val triggered = waitForCondition({ campaignTriggered }, 10)
        assertTrue("Campaign trigger should succeed", triggered)
        
        // Wait for the in-app message to be displayed
        val displayed = waitForInAppMessage()
        assertTrue("In-app message should be displayed after campaign trigger", displayed)
        
        Log.d(TAG, "In-app campaign trigger test completed successfully")
    }
    
    @Test
    fun testPushCampaignTriggerViaAPI() {
        Log.d(TAG, "Testing push campaign trigger via API")
        
        // Reset test states
        testUtils.resetTestStates()
        
        // Trigger the push campaign via API with callback
        var campaignTriggered = false
        triggerPushCampaignViaAPI(TEST_PUSH_CAMPAIGN_ID, TEST_USER_EMAIL) { success ->
            campaignTriggered = success
        }
        
        // Wait for campaign to be triggered
        val triggered = waitForCondition({ campaignTriggered }, 10)
        assertTrue("Push campaign trigger should succeed", triggered)
        
        // Wait for the push notification to be received
        val received = waitForPushNotification()
        assertTrue("Push notification should be received after campaign trigger", received)
        
        Log.d(TAG, "Push campaign trigger test completed successfully")
    }
    
    @Test
    fun testCampaignTriggerWithDataFields() {
        Log.d(TAG, "Testing campaign trigger with data fields")
        
        // Reset test states
        testUtils.resetTestStates()
        
        // Create test data fields
        val dataFields = mapOf(
            "firstName" to "Jane",
            "lastName" to "Smith",
            "purchaseAmount" to 42.42,
            "testType" to "integration_test"
        )
        
        // Trigger the campaign via API with data fields
        val success = triggerCampaignViaAPI(TEST_INAPP_CAMPAIGN_ID, TEST_USER_EMAIL, dataFields)
        assertTrue("Campaign trigger with data fields should succeed", success)
        
        // Wait for the campaign to be processed
        val processed = waitForCampaignTrigger(TEST_INAPP_CAMPAIGN_ID)
        assertTrue("Campaign should be processed with data fields", processed)
        
        Log.d(TAG, "Campaign trigger with data fields test completed successfully")
    }
    
    @Test
    fun testCampaignTriggerWithCustomUser() {
        Log.d(TAG, "Testing campaign trigger with custom user")
        
        // Reset test states
        testUtils.resetTestStates()
        
        // Use a different test user
        val customUserEmail = "integration.test@iterable.com"
        
        // Trigger the campaign via API for custom user
        val success = triggerCampaignViaAPI(TEST_INAPP_CAMPAIGN_ID, customUserEmail)
        assertTrue("Campaign trigger for custom user should succeed", success)
        
        // Wait for the campaign to be processed
        val processed = waitForCampaignTrigger(TEST_INAPP_CAMPAIGN_ID)
        assertTrue("Campaign should be processed for custom user", processed)
        
        Log.d(TAG, "Campaign trigger with custom user test completed successfully")
    }
    
    @Test
    fun testMultipleCampaignTriggers() {
        Log.d(TAG, "Testing multiple campaign triggers")
        
        // Reset test states
        testUtils.resetTestStates()
        
        // Trigger multiple campaigns
        val campaign1Success = triggerCampaignViaAPI(TEST_INAPP_CAMPAIGN_ID, TEST_USER_EMAIL)
        val campaign2Success = triggerPushCampaignViaAPI(TEST_PUSH_CAMPAIGN_ID, TEST_USER_EMAIL)
        
        assertTrue("First campaign trigger should succeed", campaign1Success)
        assertTrue("Second campaign trigger should succeed", campaign2Success)
        
        // Wait for at least one campaign to be processed
        val processed = waitForCondition({
            testUtils.hasInAppMessageDisplayed() || testUtils.hasReceivedPushNotification()
        })
        assertTrue("At least one campaign should be processed", processed)
        
        Log.d(TAG, "Multiple campaign triggers test completed successfully")
    }
    
    @Test
    fun testCampaignTriggerErrorHandling() {
        Log.d(TAG, "Testing campaign trigger error handling")
        
        // Test with invalid campaign ID
        val invalidCampaignId = 99999999
        val success = triggerCampaignViaAPI(invalidCampaignId, TEST_USER_EMAIL)
        
        // The API call might succeed but the campaign might not exist
        // We'll just verify the method doesn't crash
        Log.d(TAG, "Campaign trigger with invalid ID result: $success")
        
        // Test with invalid email
        val invalidEmail = "invalid@email.com"
        val success2 = triggerCampaignViaAPI(TEST_INAPP_CAMPAIGN_ID, invalidEmail)
        
        // The API call might succeed but the user might not exist
        // We'll just verify the method doesn't crash
        Log.d(TAG, "Campaign trigger with invalid email result: $success2")
        
        Log.d(TAG, "Campaign trigger error handling test completed")
    }
} 