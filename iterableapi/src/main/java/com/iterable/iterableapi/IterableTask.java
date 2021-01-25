package com.iterable.iterableapi;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.UUID;

class IterableTask {

    int currentVersion = 1;

    String id;
    String name;
    int version;
    long createdAt;
    long modifiedAt;
    long lastAttemptedAt;
    long scheduledAt;
    long requestedAt;

    boolean processing;
    boolean failed;
    boolean blocking;

    //TODO: Confirm if data and failure data would be String converted from JSONObjects.
    String data;
    String taskFailureData;
    IterableTaskType taskType;
    int attempts;

    //To be used when creating IterableTask from database
    IterableTask(String id, @NonNull String name, int version, @NonNull long createdAt, long modifiedAt, long lastAttemptedAt, long scheduledAt, long requestedAt, boolean processing, boolean failed, boolean blocking, String data, String taskFailureData, IterableTaskType taskType, int attempts) {
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
    IterableTask(String name, IterableTaskType taskType, String data) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.createdAt = new Date().getTime();
        this.scheduledAt = new Date().getTime();
        this.requestedAt = new Date().getTime();
        this.data = data;
        this.taskType = taskType;
    }

}

enum IterableTaskType {
    API {
        @Override
        @NonNull
        public String toString() {
            return "API";
        }
    }
}
