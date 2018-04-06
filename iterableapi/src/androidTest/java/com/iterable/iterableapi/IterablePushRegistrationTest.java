package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

/**
 * Tests
 * Created by David Truong dt@iterable.com.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class IterablePushRegistrationTest {
    Context appContext;
    IterableApi iterableApi;
    String senderID = "111111111111";

    @Before
    public void setUp() throws Exception {
        appContext = InstrumentationRegistry.getContext().getApplicationContext();
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
    @Test
    public void testGetGCMToken() throws Exception {
        assumeTrue(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext) == ConnectionResult.SUCCESS);
        IterablePushRegistration registration = new IterablePushRegistration();
        IterablePushRegistration.PushRegistrationObject registrationObject = registration.getDeviceToken(senderID, IterableConstants.MESSAGING_PLATFORM_GOOGLE, "test_application_GCM", false);
        assertNotNull(registrationObject.token);
        assertEquals(IterableConstants.MESSAGING_PLATFORM_GOOGLE, registrationObject.messagingPlatform);

        SharedPreferences sharedPref = appContext.getSharedPreferences(IterableConstants.PUSH_APP_ID, Context.MODE_PRIVATE);
        String pushIdPref = sharedPref.getString(IterableConstants.PUSH_APP_ID, null);
        assertNull(pushIdPref);
    }

    /**
     * Tests getting a token for FCM. Defaults to GCM if the project hasn't upgraded to Firebase and included the google-services.json
     * Checks the sharedPref flag that is set after upgrading to firebase.
     * @throws Exception
     */
    @Test
    public void testGetFCMToken() throws Exception {
        assumeTrue(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext) == ConnectionResult.SUCCESS);
        IterablePushRegistration registration = new IterablePushRegistration();
        SharedPreferences sharedPref;
        String pushIdPref;
        IterablePushRegistration.PushRegistrationObject registrationObject = registration.getDeviceToken(senderID, IterableConstants.MESSAGING_PLATFORM_FIREBASE, "test_application_FCM", false);
        assertNotNull(registrationObject.token);
        if (IterablePushRegistration.getFirebaseResouceId(appContext) != 0) {
            //FCM registration
            sharedPref = appContext.getSharedPreferences(IterableConstants.PUSH_APP_ID, Context.MODE_PRIVATE);
            pushIdPref = sharedPref.getString(IterableConstants.PUSH_APP_ID, null);
            assertNotNull(pushIdPref);
            assertEquals(IterableConstants.MESSAGING_PLATFORM_FIREBASE, registrationObject.messagingPlatform);

        } else {
            //GCM registration
            sharedPref = appContext.getSharedPreferences(IterableConstants.PUSH_APP_ID, Context.MODE_PRIVATE);
            pushIdPref = sharedPref.getString(IterableConstants.PUSH_APP_ID, null);
            assertNull(pushIdPref);
            assertEquals(IterableConstants.MESSAGING_PLATFORM_GOOGLE, registrationObject.messagingPlatform);
        }
    }
}
