package com.iterable.iterableapi

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import android.util.Log

/**
 *
 */
class IterableConfig private constructor(builder: Builder) {

    /**
     * Push integration name - used for token registration.
     * Make sure the name of this integration matches the one set up in Iterable console.
     */
    val pushIntegrationName: String? = builder.pushIntegrationName

    /**
     * Custom URL handler to override openUrl actions
     */
    val urlHandler: IterableUrlHandler? = builder.urlHandler

    /**
     * Action handler for custom actions
     */
    val customActionHandler: IterableCustomActionHandler? = builder.customActionHandler

    /**
     * If set to `true`, the SDK will automatically register the push token when you
     * call [IterableApi.setUserId] or [IterableApi.setEmail]
     * and disable the old device entry when the user logs out
     */
    val autoPushRegistration: Boolean = builder.autoPushRegistration

    /**
     * When set to true, it will check for deferred deep links on first time app launch
     * after installation.
     */
    @Deprecated("Deprecated")
    val checkForDeferredDeeplink: Boolean = builder.checkForDeferredDeeplink

    /**
     * Log level for Iterable SDK log messages
     */
    val logLevel: Int = builder.logLevel

    /**
     * Custom in-app handler that can be used to control whether an incoming in-app message should
     * be shown immediately or not
     */
    val inAppHandler: IterableInAppHandler = builder.inAppHandler

    /**
     * The number of seconds to wait before showing the next in-app message, if there are multiple
     * messages in the queue
     */
    val inAppDisplayInterval: Double = builder.inAppDisplayInterval

    /**
     * Custom auth handler that can be used to control retrieving and storing an auth token
     */
    val authHandler: IterableAuthHandler? = builder.authHandler

    /**
     * Duration prior to an auth expiration that a new auth token should be requested.
     */
    val expiringAuthTokenRefreshPeriod: Long = builder.expiringAuthTokenRefreshPeriod

    /**
     * Retry policy for JWT Refresh.
     */
    val retryPolicy: RetryPolicy = builder.retryPolicy

    /**
     * By default, the SDK allows navigation/calls to URLs with the `https` protocol (e.g. deep links or external links)
     * If you'd like to allow other protocols like `http`, `tel`, etc., add them to the `allowedProtocols` array
     */
    val allowedProtocols: Array<String> = builder.allowedProtocols

    /**
     * Data region determining which data center and endpoints are used by the SDK.
     */
    val dataRegion: IterableDataRegion = builder.dataRegion

    /**
     * This controls whether the in-app content should be saved to disk, or only kept in memory.
     * By default, the SDK will save in-apps to disk.
     */
    val useInMemoryStorageForInApps: Boolean = builder.useInMemoryStorageForInApps

    /**
     * Allows for fetching embedded messages.
     */
    val enableEmbeddedMessaging: Boolean = builder.enableEmbeddedMessaging

    /**
     * When set to true, disables encryption for keychain storage.
     * By default, encryption is enabled for storing sensitive user data.
     */
    val keychainEncryption: Boolean = builder.keychainEncryption

    /**
     * Handler for decryption failures of PII information.
     * Before calling this handler, the SDK will clear the PII information and create new encryption keys
     */
    val decryptionFailureHandler: IterableDecryptionFailureHandler? = builder.decryptionFailureHandler

    /**
     * Mobile framework information for the app
     */
    val mobileFrameworkInfo: IterableAPIMobileFrameworkInfo? = builder.mobileFrameworkInfo

    class Builder {
        internal var pushIntegrationName: String? = null
        internal var urlHandler: IterableUrlHandler? = null
        internal var customActionHandler: IterableCustomActionHandler? = null
        internal var autoPushRegistration = true
        internal var checkForDeferredDeeplink = false
        internal var logLevel = Log.ERROR
        internal var inAppHandler: IterableInAppHandler = IterableDefaultInAppHandler()
        internal var inAppDisplayInterval = 30.0
        internal var authHandler: IterableAuthHandler? = null
        internal var expiringAuthTokenRefreshPeriod = 60000L
        internal var retryPolicy = RetryPolicy(10, 6L, RetryPolicy.Type.LINEAR)
        internal var allowedProtocols = emptyArray<String>()
        internal var dataRegion = IterableDataRegion.US
        internal var useInMemoryStorageForInApps = false
        internal var enableEmbeddedMessaging = false
        internal var keychainEncryption = true
        internal var decryptionFailureHandler: IterableDecryptionFailureHandler? = null
        internal var mobileFrameworkInfo: IterableAPIMobileFrameworkInfo? = null

        /**
         * Push integration name - used for token registration
         * Make sure the name of this integration matches the one set up in Iterable console
         * If this field is not set, Iterable SDK defaults it to the app's package name
         * @param pushIntegrationName Push integration name
         */
        @NonNull
        fun setPushIntegrationName(@NonNull pushIntegrationName: String): Builder {
            this.pushIntegrationName = pushIntegrationName
            return this
        }

        /**
         * Set a custom URL handler to override openUrl actions
         * @param urlHandler Custom URL handler provided by the app
         */
        @NonNull
        fun setUrlHandler(@NonNull urlHandler: IterableUrlHandler): Builder {
            this.urlHandler = urlHandler
            return this
        }

        /**
         * Set an action handler for custom actions
         * @param customActionHandler Custom action handler provided by the app
         */
        @NonNull
        fun setCustomActionHandler(@NonNull customActionHandler: IterableCustomActionHandler): Builder {
            this.customActionHandler = customActionHandler
            return this
        }

        /**
         * Enable or disable automatic push token registration
         * If set to `true`, the SDK will automatically register the push token when you
         * call [IterableApi.setUserId] or [IterableApi.setEmail]
         * and disable the old device entry when the user logs out
         * @param enabled Enable automatic push token registration
         */
        @NonNull
        fun setAutoPushRegistration(enabled: Boolean): Builder {
            this.autoPushRegistration = enabled
            return this
        }

        /**
         * When set to true, it will check for deferred deep links on first time app launch
         * after installation.
         * @param checkForDeferredDeeplink Enable deferred deep link checks on first launch
         */
        @NonNull
        fun setCheckForDeferredDeeplink(checkForDeferredDeeplink: Boolean): Builder {
            this.checkForDeferredDeeplink = checkForDeferredDeeplink
            return this
        }

        /**
         * Set the log level for Iterable SDK log messages
         * @param logLevel Log level, defaults to [Log.ERROR]
         */
        @NonNull
        fun setLogLevel(logLevel: Int): Builder {
            this.logLevel = logLevel
            return this
        }

        /**
         * Set a custom in-app handler that can be used to control whether an incoming in-app message
         * should be shown immediately or not
         * @param inAppHandler In-app handler provided by the app
         */
        @NonNull
        fun setInAppHandler(@NonNull inAppHandler: IterableInAppHandler): Builder {
            this.inAppHandler = inAppHandler
            return this
        }

        /**
         * Set the in-app message display interval: the number of seconds to wait before showing
         * the next in-app message, if there are multiple messages in the queue
         * @param inAppDisplayInterval display interval in seconds
         */
        @NonNull
        fun setInAppDisplayInterval(inAppDisplayInterval: Double): Builder {
            this.inAppDisplayInterval = inAppDisplayInterval
            return this
        }

        /**
         * Set a custom auth handler that can be used to retrieve a new auth token
         * @param authHandler Auth handler provided by the app
         */
        @NonNull
        fun setAuthHandler(@NonNull authHandler: IterableAuthHandler): Builder {
            this.authHandler = authHandler
            return this
        }

        /**
         * Set retry policy for JWT Refresh
         * @param retryPolicy
         */
        @NonNull
        fun setAuthRetryPolicy(@NonNull retryPolicy: RetryPolicy): Builder {
            this.retryPolicy = retryPolicy
            return this
        }

        /**
         * Set a custom period before an auth token expires to automatically retrieve a new token
         * @param period in seconds
         */
        @NonNull
        fun setExpiringAuthTokenRefreshPeriod(@NonNull period: Long): Builder {
            this.expiringAuthTokenRefreshPeriod = period * 1000L
            return this
        }

        /**
         * Set what URLs the SDK should allow to open (in addition to `https`)
         * @param allowedProtocols an array/list of protocols (e.g. `http`, `tel`)
         */
        @NonNull
        fun setAllowedProtocols(@NonNull allowedProtocols: Array<String>): Builder {
            this.allowedProtocols = allowedProtocols
            return this
        }

        /**
         * Set the data region used by the SDK
         * @param dataRegion enum value that determines which endpoint to use, defaults to IterableDataRegion.US
         */
        @NonNull
        fun setDataRegion(@NonNull dataRegion: IterableDataRegion): Builder {
            this.dataRegion = dataRegion
            return this
        }

        /**
         * Set whether the SDK should store in-apps only in memory, or in file storage
         * @param useInMemoryStorageForInApps `true` will have in-apps be only in memory
         */
        @NonNull
        fun setUseInMemoryStorageForInApps(useInMemoryStorageForInApps: Boolean): Builder {
            this.useInMemoryStorageForInApps = useInMemoryStorageForInApps
            return this
        }

        /**
         * Allows for fetching embedded messages.
         * @param enableEmbeddedMessaging `true` will allow automatically fetching embedded messaging.
         */
        fun setEnableEmbeddedMessaging(enableEmbeddedMessaging: Boolean): Builder {
            this.enableEmbeddedMessaging = enableEmbeddedMessaging
            return this
        }

        /**
         * When set to true, disables encryption for Iterable's keychain storage.
         * By default, encryption is enabled for storing sensitive user data.
         * @param keychainEncryption Whether to disable encryption for keychain
         */
        @NonNull
        fun setKeychainEncryption(keychainEncryption: Boolean): Builder {
            this.keychainEncryption = keychainEncryption
            return this
        }

        /**
         * Set a handler for decryption failures that can be used to handle data recovery
         * @param handler Decryption failure handler provided by the app
         */
        @NonNull
        fun setDecryptionFailureHandler(@NonNull handler: IterableDecryptionFailureHandler): Builder {
            this.decryptionFailureHandler = handler
            return this
        }

        /**
         * Set mobile framework information for the app
         * @param mobileFrameworkInfo Mobile framework information
         */
        @NonNull
        fun setMobileFrameworkInfo(@NonNull mobileFrameworkInfo: IterableAPIMobileFrameworkInfo): Builder {
            this.mobileFrameworkInfo = mobileFrameworkInfo
            return this
        }

        @NonNull
        fun build(): IterableConfig {
            return IterableConfig(this)
        }
    }
}