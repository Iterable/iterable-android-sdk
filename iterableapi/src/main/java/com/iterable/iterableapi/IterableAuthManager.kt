package com.iterable.iterableapi

import android.util.Base64
import androidx.annotation.VisibleForTesting
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class IterableAuthManager(
    private val api: IterableApi,
    private val authHandler: IterableAuthHandler?,
    private val authRetryPolicy: RetryPolicy,
    private val expiringAuthTokenRefreshPeriod: Long
) {
    @VisibleForTesting
    var timer: Timer? = null
    private var hasFailedPriorAuth = false
    private var pendingAuth = false
    private var requiresAuthRefresh = false
    var pauseAuthRetry = false
    var retryCount = 0
    private var isLastAuthTokenValid = false
    private var isTimerScheduled = false

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    companion object {
        private const val TAG = "IterableAuth"
        private const val expirationString = "exp"

        private fun decodedExpiration(encodedJWT: String): Long {
            var exp: Long = 0
            val split = encodedJWT.split("\\.".toRegex()).toTypedArray()
            // Check if jwt is valid
            if (split.size != 3) {
                throw IllegalArgumentException("Invalid JWT")
            }
            val body = getJson(split[1])
            val jObj = JSONObject(body)
            exp = jObj.getLong(expirationString)
            return exp
        }

        @Throws(UnsupportedEncodingException::class)
        private fun getJson(strEncoded: String): String {
            val decodedBytes = Base64.decode(strEncoded, Base64.URL_SAFE)
            return String(decodedBytes, charset("UTF-8"))
        }
    }

    @Synchronized
    fun requestNewAuthToken(hasFailedPriorAuth: Boolean) {
        requestNewAuthToken(hasFailedPriorAuth, null, true)
    }

    fun pauseAuthRetries(pauseRetry: Boolean) {
        pauseAuthRetry = pauseRetry
        resetRetryCount()
    }

    fun reset() {
        clearRefreshTimer()
        setIsLastAuthTokenValid(false)
    }

    fun setIsLastAuthTokenValid(isValid: Boolean) {
        isLastAuthTokenValid = isValid
    }

    fun resetRetryCount() {
        retryCount = 0
    }

    private fun handleSuccessForAuthToken(authToken: String, successCallback: IterableHelper.SuccessHandler?) {
        try {
            val `object` = JSONObject()
            `object`.put("newAuthToken", authToken)
            successCallback?.onSuccess(`object`)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun requestNewAuthToken(
        hasFailedPriorAuth: Boolean,
        successCallback: IterableHelper.SuccessHandler?,
        shouldIgnoreRetryPolicy: Boolean
    ) {
        if (!shouldIgnoreRetryPolicy && (pauseAuthRetry || retryCount >= authRetryPolicy.maxRetry)) {
            return
        }

        if (authHandler != null) {
            if (!pendingAuth) {
                if (!(this.hasFailedPriorAuth && hasFailedPriorAuth)) {
                    this.hasFailedPriorAuth = hasFailedPriorAuth
                    pendingAuth = true

                    executor.submit {
                        try {
                            if (isLastAuthTokenValid && !shouldIgnoreRetryPolicy) {
                                // if some JWT retry had valid token it will not fetch the auth token again from developer function
                                handleAuthTokenSuccess(IterableApi.getInstance().authToken, successCallback)
                                pendingAuth = false
                                return@submit
                            }
                            val authToken = authHandler.onAuthTokenRequested()
                            pendingAuth = false
                            retryCount++
                            handleAuthTokenSuccess(authToken, successCallback)
                        } catch (e: Exception) {
                            retryCount++
                            handleAuthTokenFailure(e)
                        }
                    }
                }
            } else if (!hasFailedPriorAuth) {
                // setFlag to resync auth after current auth returns
                requiresAuthRefresh = true
            }
        } else {
            IterableApi.getInstance().setAuthToken(null, true)
        }
    }

    private fun handleAuthTokenSuccess(authToken: String?, successCallback: IterableHelper.SuccessHandler?) {
        if (authToken != null) {
            if (successCallback != null) {
                handleSuccessForAuthToken(authToken, successCallback)
            }
            queueExpirationRefresh(authToken)
        } else {
            handleAuthFailure(authToken, AuthFailureReason.AUTH_TOKEN_NULL)
            IterableApi.getInstance().setAuthToken(authToken)
            scheduleAuthTokenRefresh(getNextRetryInterval(), false, null)
            return
        }
        IterableApi.getInstance().setAuthToken(authToken)
        reSyncAuth()
        authHandler?.onTokenRegistrationSuccessful(authToken)
    }

    // This method is called when there is an error receiving an the auth token.
    private fun handleAuthTokenFailure(throwable: Throwable) {
        IterableLogger.e(TAG, "Error while requesting Auth Token", throwable)
        handleAuthFailure(null, AuthFailureReason.AUTH_TOKEN_GENERATION_ERROR)
        pendingAuth = false
        scheduleAuthTokenRefresh(getNextRetryInterval(), false, null)
    }

    fun queueExpirationRefresh(encodedJWT: String) {
        clearRefreshTimer()
        try {
            val expirationTimeSeconds = decodedExpiration(encodedJWT)
            val triggerExpirationRefreshTime = expirationTimeSeconds * 1000L - expiringAuthTokenRefreshPeriod - IterableUtil.currentTimeMillis()
            if (triggerExpirationRefreshTime > 0) {
                scheduleAuthTokenRefresh(triggerExpirationRefreshTime, true, null)
            } else {
                IterableLogger.w(TAG, "The expiringAuthTokenRefreshPeriod has already passed for the current JWT")
            }
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Error while parsing JWT for the expiration", e)
            isLastAuthTokenValid = false
            handleAuthFailure(encodedJWT, AuthFailureReason.AUTH_TOKEN_PAYLOAD_INVALID)
            scheduleAuthTokenRefresh(getNextRetryInterval(), false, null)
        }
    }

    fun resetFailedAuth() {
        hasFailedPriorAuth = false
    }

    fun reSyncAuth() {
        if (requiresAuthRefresh) {
            requiresAuthRefresh = false
            scheduleAuthTokenRefresh(getNextRetryInterval(), false, null)
        }
    }

    // This method is called is used to call the authHandler.onAuthFailure method with appropriate AuthFailureReason
    fun handleAuthFailure(authToken: String?, failureReason: AuthFailureReason) {
        if (authHandler != null) {
            authHandler.onAuthFailure(AuthFailure(getEmailOrUserId(), authToken, IterableUtil.currentTimeMillis(), failureReason))
        }
    }

    fun getNextRetryInterval(): Long {
        var nextRetryInterval = authRetryPolicy.retryInterval
        if (authRetryPolicy.retryBackoff == RetryPolicy.Type.EXPONENTIAL) {
            nextRetryInterval *= Math.pow(IterableConstants.EXPONENTIAL_FACTOR.toDouble(), (retryCount - 1).toDouble()).toLong() // Exponential backoff
        }
        return nextRetryInterval
    }

    fun scheduleAuthTokenRefresh(timeDuration: Long, isScheduledRefresh: Boolean, successCallback: IterableHelper.SuccessHandler?) {
        if ((pauseAuthRetry && !isScheduledRefresh) || isTimerScheduled) {
            // we only stop schedule token refresh if it is called from retry (in case of failure). The normal auth token refresh schedule would work
            return
        }
        if (timer == null) {
            timer = Timer(true)
        }

        try {
            timer!!.schedule(object : TimerTask() {
                override fun run() {
                    if (api.email != null || api.userId != null) {
                        api.authManager.requestNewAuthToken(false, successCallback, isScheduledRefresh)
                    } else {
                        IterableLogger.w(TAG, "Email or userId is not available. Skipping token refresh")
                    }
                    isTimerScheduled = false
                }
            }, timeDuration)
            isTimerScheduled = true
        } catch (e: Exception) {
            IterableLogger.e(TAG, "timer exception: $timer", e)
        }
    }

    private fun getEmailOrUserId(): String? {
        val email = api.email
        val userId = api.userId

        return if (email != null) {
            email
        } else if (userId != null) {
            userId
        } else null
    }

    fun clearRefreshTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
            isTimerScheduled = false
        }
    }
}