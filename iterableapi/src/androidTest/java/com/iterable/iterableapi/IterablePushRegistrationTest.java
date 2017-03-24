package com.iterable.iterableapi;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.test.ApplicationTestCase;

/**
 * Tests
 * Created by David Truong dt@iterable.com.
 */
public class IterablePushRegistrationTest extends ApplicationTestCase<Application> {
    public IterablePushRegistrationTest() {
        super(Application.class);
    }

    Context appContext;
    IterableApi iterableApi;
    String senderID = "668868396391";

    public void setUp() throws Exception {
        super.setUp();

        appContext = getContext().getApplicationContext();
        iterableApi = IterableApi.sharedInstanceWithApiKey(appContext, "fake_key", "test_email");

        SharedPreferences sharedPref = appContext.getSharedPreferences(IterableConstants.PUSH_APP_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(IterableConstants.PUSH_APP_ID);
        editor.commit();
    }

    /**
     * Tests getting a token for GCM
     * @throws Exception
     */
    public void testGetGCMToken() throws Exception {
        IterablePushRegistration registration = new IterablePushRegistration();
        String token = registration.getDeviceToken(senderID, IterableConstants.MESSAGING_PLATFORM_GOOGLE, "test_application_GCM", false);
        assertNotNull(token);

        SharedPreferences sharedPref = appContext.getSharedPreferences(IterableConstants.PUSH_APP_ID, Context.MODE_PRIVATE);
        String pushIdPref = sharedPref.getString(IterableConstants.PUSH_APP_ID, null);
        assertNull(pushIdPref);
    }

    /**
     * Tests getting a token for FCM. Defaults to GCM if the project hasn't upgraded to Firebase and included the google-services.json
     * Checks the sharedPref flag that is set after upgrading to firebase.
     * @throws Exception
     */
    public void testGetFCMToken() throws Exception {
        IterablePushRegistration registration = new IterablePushRegistration();
        SharedPreferences sharedPref;
        String pushIdPref;
        String token = registration.getDeviceToken(senderID, IterableConstants.MESSAGING_PLATFORM_FIREBASE, "test_application_FCM", false);
        assertNotNull(token);
        if (IterablePushRegistration.getFirebaseResouceId(appContext) != 0) {
            //FCM registration
            sharedPref = appContext.getSharedPreferences(IterableConstants.PUSH_APP_ID, Context.MODE_PRIVATE);
            pushIdPref = sharedPref.getString(IterableConstants.PUSH_APP_ID, null);
            assertNotNull(pushIdPref);

        } else {
            //GCM registration
            sharedPref = appContext.getSharedPreferences(IterableConstants.PUSH_APP_ID, Context.MODE_PRIVATE);
            pushIdPref = sharedPref.getString(IterableConstants.PUSH_APP_ID, null);
            assertNull(pushIdPref);
        }
    }
}
