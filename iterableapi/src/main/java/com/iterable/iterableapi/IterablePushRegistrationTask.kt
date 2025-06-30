package com.iterable.iterableapi

import android.content.Context
import android.os.AsyncTask

/**
 * Created by David Truong dt@iterable.com
 */
internal class IterablePushRegistrationTask : AsyncTask<IterablePushRegistrationData, Void, Void>() {
    
    companion object {
        const val TAG = "IterablePushRegistration"
    }
    
    private var iterablePushRegistrationData: IterablePushRegistrationData? = null

    /**
     * Registers or disables the device
     * @param params Push registration request data
     */
    override fun doInBackground(vararg params: IterablePushRegistrationData): Void? {
        iterablePushRegistrationData = params[0]
        if (iterablePushRegistrationData?.pushIntegrationName != null) {
            val pushRegistrationObject = getDeviceToken()
            if (pushRegistrationObject != null) {
                when (iterablePushRegistrationData?.pushRegistrationAction) {
                    IterablePushRegistrationData.PushRegistrationAction.ENABLE -> {
                        val registrationData = iterablePushRegistrationData!!
                        IterableApi.sharedInstance.registerDeviceToken(
                                registrationData.email,
                                registrationData.userId,
                                registrationData.authToken,
                                registrationData.pushIntegrationName ?: "",
                                pushRegistrationObject.token,
                                IterableApi.getInstance().deviceAttributes)
                    }
                    IterablePushRegistrationData.PushRegistrationAction.DISABLE -> {
                        IterableApi.sharedInstance.disableToken(
                                iterablePushRegistrationData?.email,
                                iterablePushRegistrationData?.userId,
                                iterablePushRegistrationData?.authToken,
                                pushRegistrationObject.token,
                                null,
                                null
                        )
                    }
                    else -> {}
                }
            }
        } else {
            IterableLogger.e("IterablePush", "iterablePushRegistrationData has not been specified")
        }
        return null
    }

    /**
     * @return PushRegistrationObject
     */
    private fun getDeviceToken(): PushRegistrationObject? {
        return try {
            val applicationContext = IterableApi.sharedInstance.mainActivityContext
            if (applicationContext == null) {
                IterableLogger.e(TAG, "MainActivity Context is null")
                return null
            }

            val senderId = Util.getSenderId(applicationContext)
            if (senderId == null) {
                IterableLogger.e(TAG, "Could not find gcm_defaultSenderId, please check that Firebase SDK is set up properly")
                return null
            }

            PushRegistrationObject(Util.getFirebaseToken() ?: "")

        } catch (e: Exception) {
            IterableLogger.e(TAG, "Exception while retrieving the device token: check that firebase is added to the build dependencies", e)
            null
        }
    }

    internal object Util {
        @JvmStatic
        var instance: UtilImpl = UtilImpl()

        @JvmStatic
        fun getFirebaseToken(): String? {
            return instance.getFirebaseToken()
        }

        @JvmStatic
        fun getSenderId(applicationContext: Context): String? {
            return instance.getSenderId(applicationContext)
        }

        internal class UtilImpl {
            fun getFirebaseToken(): String? {
                return IterableFirebaseMessagingService.getFirebaseToken()
            }

            fun getSenderId(applicationContext: Context): String? {
                val resId = applicationContext.resources.getIdentifier(IterableConstants.FIREBASE_SENDER_ID, IterableConstants.ANDROID_STRING, applicationContext.packageName)
                return if (resId != 0) {
                    applicationContext.resources.getString(resId)
                } else {
                    null
                }
            }
        }
    }

    internal class PushRegistrationObject(val token: String) {
        val messagingPlatform: String = IterableConstants.MESSAGING_PLATFORM_FIREBASE
    }
}


