package com.iterable.iterableapi;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.UUID;

class IterableTask {

    //String columns as stored in DB
    static final String TASK_ID = "task_id";
    static final String NAME = "name";
    static final String VERSION = "version";
    static final String CREATED_AT = "created";
    static final String MODIFIED_AT = "modified";
    static final String LAST_ATTEMPTED_AT = "last_attempt";
    static final String SCHEDULED_AT = "scheduled";
    static final String REQUESTED_AT = "requested";
    static final String PROCESSING = "processing";
    static final String FAILED = "failed";
    static final String BLOCKING = "blocking";
    static final String DATA = "data";
    static final String ERROR = "error";
    static final String TYPE = "type";
    static final String ATTEMPTS = "attempts";

    int currentVersion = 1;

    String id; //uuid generated for each task when getting created
    String name;//name of the api
    int version;//version for the task? Not sure. replicating as on iOS
    Date createdAt;
    Date modifiedAt;
    Date lastAttemptedAt;
    Date scheduledAt;
    Date requestedAt;

    Boolean processing;
    Boolean failed;
    Boolean blocking;

    //TODO: Confirm if data and failure data would be String converted from JSONObjects.
    Object data;
    Object taskFailureData;
    IterableTaskType taskType;
    int attempts;

    //To be used when creating IterableTask from database
    public IterableTask(String id, @NonNull String name, @NonNull int version, @NonNull Date createdAt, Date modifiedAt, Date lastAttemptedAt, Date scheduledAt, Date requestedAt, Boolean processing, Boolean failed, Boolean blocking, Object data, Object taskFailureData, IterableTaskType taskType, int attempts) {

        this.id = id;
        this.name = name;
        this.version = version;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.lastAttemptedAt = lastAttemptedAt;
        this.scheduledAt = scheduledAt;
        this.requestedAt = requestedAt;
        this.processing = processing;
        this.failed = failed;
        this.blocking = blocking;
        this.data = data;
        this.taskFailureData = taskFailureData;
        this.taskType = taskType;
        this.attempts = attempts;
    }

    //Bare minimum one to be used when creating the Task
    public IterableTask(String name, IterableTaskType taskType) {

        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.createdAt = new Date();
        this.scheduledAt = new Date();
        this.requestedAt = new Date();
        this.taskType = taskType;

    }

    IterableTask updateTask(int attempts, Date lastAttemptedAt, Boolean processing, Date scheduledAt, Object data, Object taskFailureData) {
        return new IterableTask(id, name, version, createdAt, modifiedAt, lastAttemptedAt, scheduledAt, requestedAt, processing, failed, blocking, data, taskFailureData, taskType, attempts);
    }
}

enum IterableTaskType {
    API {
        @Override
        public String toString() {
            return "API";
        }
    }
}
