package com.iterable.iterableapi

import android.content.Context
import android.content.pm.PackageManager
import android.os.AsyncTask
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.iterable.iterableapi.util.DeviceInfoUtils
import org.json.JSONObject

/**
 * Created by David Truong dt@iterable.com
 */
internal class IterablePushRegistrationTask : AsyncTask<IterablePushRegistrationData, Void, Void>() {
    
    companion object {
        private const val TAG = "IterablePushRegistrationTask"
    }
    
    override fun doInBackground(vararg params: IterablePushRegistrationData): Void? {
        if (params.isEmpty()) {
            return null
        }

        val registrationData = params[0]
        val pushRegistrationObject = getDeviceToken()
        if (pushRegistrationObject != null) {
            if (registrationData.pushRegistrationAction == IterablePushRegistrationData.PushRegistrationAction.ENABLE) {
                IterableApi.getInstance().registerDeviceToken(
                    registrationData.email,
                    registrationData.userId,
                    registrationData.authToken,
                    registrationData.pushIntegrationName,
                    pushRegistrationObject.token,
                    null,
                    HashMap()
                )
            } else {
                IterableApi.getInstance().disableToken(
                    registrationData.email,
                    registrationData.userId,
                    registrationData.authToken,
                    pushRegistrationObject.token,
                    null,
                    null
                )
            }
        }
        return null
    }

    private fun getDeviceToken(): PushRegistrationObject? {
        return try {
            val applicationContext = IterableApi.getInstance().mainActivityContext
            if (applicationContext == null) {
                IterableLogger.e(TAG, "MainActivity Context is null")
                return null
            }

            val token = Tasks.await(FirebaseMessaging.getInstance().token)
            if (token != null) {
                PushRegistrationObject(token)
            } else {
                null
            }
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Failed to retrieve the FCM token", e)
            null
        }
    }

    private data class PushRegistrationObject(
        val token: String,
        val messagingPlatform: String = IterableConstants.MESSAGING_PLATFORM_FIREBASE
    )
}


