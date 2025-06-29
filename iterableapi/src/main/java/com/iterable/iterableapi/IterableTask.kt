package com.iterable.iterableapi

import androidx.annotation.NonNull

import java.util.Date
import java.util.UUID

internal class IterableTask {

    var currentVersion = 1

    var id: String? = null
    var name: String? = null
    var version: Int = 0
    var createdAt: Long = 0
    var modifiedAt: Long = 0
    var lastAttemptedAt: Long = 0
    var scheduledAt: Long = 0
    var requestedAt: Long = 0

    var processing: Boolean = false
    var failed: Boolean = false
    var blocking: Boolean = false

    //TODO: Confirm if data and failure data would be String converted from JSONObjects.
    var data: String? = null
    var taskFailureData: String? = null
    var taskType: IterableTaskType? = null
    var attempts: Int = 0

    //To be used when creating IterableTask from database
    constructor(id: String?, @NonNull name: String, version: Int, @NonNull createdAt: Long, modifiedAt: Long, lastAttemptedAt: Long, scheduledAt: Long, requestedAt: Long, processing: Boolean, failed: Boolean, blocking: Boolean, data: String?, taskFailureData: String?, taskType: IterableTaskType?, attempts: Int) {
        this.id = id
        this.name = name
        this.version = version
        this.createdAt = createdAt
        this.modifiedAt = modifiedAt
        this.lastAttemptedAt = lastAttemptedAt
        this.scheduledAt = scheduledAt
        this.requestedAt = requestedAt
        this.processing = processing
        this.failed = failed
        this.blocking = blocking
        this.data = data
        this.taskFailureData = taskFailureData
        this.taskType = taskType
        this.attempts = attempts
    }

    //Bare minimum one to be used when creating the Task
    constructor(name: String?, taskType: IterableTaskType?, data: String?) {
        this.id = UUID.randomUUID().toString()
        this.name = name
        this.createdAt = Date().time
        this.scheduledAt = Date().time
        this.requestedAt = Date().time
        this.data = data
        this.taskType = taskType
    }

}

enum class IterableTaskType {
    API {
        @NonNull
        override fun toString(): String {
            return "API"
        }
    }
}
