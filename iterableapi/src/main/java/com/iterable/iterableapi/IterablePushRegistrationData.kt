package com.iterable.iterableapi

/**
 * Created by David Truong dt@iterable.com.
 */
internal class IterablePushRegistrationData {

    enum class PushRegistrationAction {
        ENABLE,
        DISABLE
    }

    var email: String? = null
    var userId: String? = null
    var pushIntegrationName: String? = null
    var projectNumber: String = ""
    var messagingPlatform: String = IterableConstants.MESSAGING_PLATFORM_FIREBASE
    var authToken: String? = null
    var pushRegistrationAction: PushRegistrationAction? = null

    constructor(email: String?, userId: String?, pushIntegrationName: String?, projectNumber: String?, messagingPlatform: String?, pushRegistrationAction: PushRegistrationAction?) {
        this.email = email
        this.userId = userId
        this.pushIntegrationName = pushIntegrationName
        this.projectNumber = projectNumber ?: ""
        this.messagingPlatform = messagingPlatform ?: IterableConstants.MESSAGING_PLATFORM_FIREBASE
        this.pushRegistrationAction = pushRegistrationAction
    }

    constructor(email: String?, userId: String?, authToken: String?, pushIntegrationName: String?, pushRegistrationAction: PushRegistrationAction?) {
        this.email = email
        this.userId = userId
        this.pushIntegrationName = pushIntegrationName
        this.pushRegistrationAction = pushRegistrationAction
        this.authToken = authToken
    }
}
