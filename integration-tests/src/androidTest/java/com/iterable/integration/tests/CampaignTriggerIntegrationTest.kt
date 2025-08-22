package com.iterable.integration.tests

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
        val latch = CountDownLatch(1)
        
        triggerCampaignViaAPI(TEST_INAPP_CAMPAIGN_ID, TEST_USER_EMAIL) { success ->
            campaignTriggered = success
            latch.countDown()
        }
        
        // Wait for callback
        assertTrue("Campaign trigger should complete within timeout", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Campaign trigger should succeed", campaignTriggered)
        
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
        val latch = CountDownLatch(1)
        
        triggerPushCampaignViaAPI(TEST_PUSH_CAMPAIGN_ID, TEST_USER_EMAIL) { success ->
            campaignTriggered = success
            latch.countDown()
        }
        
        // Wait for callback
        assertTrue("Push campaign trigger should complete within timeout", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Push campaign trigger should succeed", campaignTriggered)
        
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
        var success = false
        val latch = CountDownLatch(1)
        
        triggerCampaignViaAPI(TEST_INAPP_CAMPAIGN_ID, TEST_USER_EMAIL, dataFields) { result ->
            success = result
            latch.countDown()
        }
        
        // Wait for callback
        assertTrue("Campaign trigger should complete within timeout", latch.await(10, TimeUnit.SECONDS))
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
        var success = false
        val latch = CountDownLatch(1)
        
        triggerCampaignViaAPI(TEST_INAPP_CAMPAIGN_ID, customUserEmail) { result ->
            success = result
            latch.countDown()
        }
        
        // Wait for callback
        assertTrue("Campaign trigger should complete within timeout", latch.await(10, TimeUnit.SECONDS))
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
        var campaign1Success = false
        var campaign2Success = false
        val latch1 = CountDownLatch(1)
        val latch2 = CountDownLatch(1)
        
        triggerCampaignViaAPI(TEST_INAPP_CAMPAIGN_ID, TEST_USER_EMAIL) { result ->
            campaign1Success = result
            latch1.countDown()
        }
        
        triggerPushCampaignViaAPI(TEST_PUSH_CAMPAIGN_ID, TEST_USER_EMAIL) { result ->
            campaign2Success = result
            latch2.countDown()
        }
        
        // Wait for both callbacks
        assertTrue("First campaign trigger should complete within timeout", latch1.await(10, TimeUnit.SECONDS))
        assertTrue("Second campaign trigger should complete within timeout", latch2.await(10, TimeUnit.SECONDS))
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
        var success = false
        val latch1 = CountDownLatch(1)
        
        triggerCampaignViaAPI(invalidCampaignId, TEST_USER_EMAIL) { result ->
            success = result
            latch1.countDown()
        }
        
        // Wait for callback
        try {
            latch1.await(10, TimeUnit.SECONDS)
            Log.d(TAG, "Campaign trigger with invalid ID result: $success")
        } catch (e: InterruptedException) {
            Log.d(TAG, "Campaign trigger with invalid ID timed out")
        }
        
        // Test with invalid email
        val invalidEmail = "invalid@email.com"
        var success2 = false
        val latch2 = CountDownLatch(1)
        
        triggerCampaignViaAPI(TEST_INAPP_CAMPAIGN_ID, invalidEmail) { result ->
            success2 = result
            latch2.countDown()
        }
        
        // Wait for callback
        try {
            latch2.await(10, TimeUnit.SECONDS)
            Log.d(TAG, "Campaign trigger with invalid email result: $success2")
        } catch (e: InterruptedException) {
            Log.d(TAG, "Campaign trigger with invalid email timed out")
        }
        
        Log.d(TAG, "Campaign trigger error handling test completed")
    }
} 