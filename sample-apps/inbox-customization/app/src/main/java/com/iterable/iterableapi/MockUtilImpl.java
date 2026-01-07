package com.iterable.iterableapi;

import android.content.Context;

/**
 * Mock implementation of UtilImpl for sample app purposes.
 * This extends UtilImpl and overrides getFirebaseToken() to return a mock token
 * so that setEmail can succeed without requiring actual Firebase setup.
 */
class MockUtilImpl extends IterablePushRegistrationTask.Util.UtilImpl {
    
    @Override
    String getFirebaseToken() {
        // Return a mock Firebase token for sample app
        return "mock-firebase-token-for-sample-app-" + System.currentTimeMillis();
    }
    
    @Override
    String getSenderId(Context applicationContext) {
        // Return the mock sender ID from resources
        int resId = applicationContext.getResources().getIdentifier(
            IterableConstants.FIREBASE_SENDER_ID,
            IterableConstants.ANDROID_STRING,
            applicationContext.getPackageName()
        );
        if (resId != 0) {
            return applicationContext.getResources().getString(resId);
        } else {
            // Fallback mock sender ID
            return "123456789";
        }
    }
}




