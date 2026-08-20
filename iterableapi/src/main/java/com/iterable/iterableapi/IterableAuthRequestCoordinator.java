package com.iterable.iterableapi;

import androidx.annotation.Nullable;

/**
 * Serializes auth-token requests while keeping each callback bound to the identity that requested
 * it. Identity comparison is by reference so a logout and login with the same value still creates
 * a distinct auth lifecycle.
 */
class IterableAuthRequestCoordinator<I> {
    enum EnqueueStatus {
        STARTED,
        QUEUED_FOR_NEW_IDENTITY,
        ALREADY_ACTIVE_FOR_IDENTITY,
        IGNORED_FAILED_RETRY
    }

    static final class Request<I> {
        private final I identity;
        @Nullable
        private final IterableHelper.SuccessHandler successCallback;
        private final boolean hasFailedPriorAuth;
        private final boolean shouldIgnoreRetryPolicy;

        Request(
                I identity,
                @Nullable IterableHelper.SuccessHandler successCallback,
                boolean hasFailedPriorAuth,
                boolean shouldIgnoreRetryPolicy
        ) {
            this.identity = identity;
            this.successCallback = successCallback;
            this.hasFailedPriorAuth = hasFailedPriorAuth;
            this.shouldIgnoreRetryPolicy = shouldIgnoreRetryPolicy;
        }

        I getIdentity() {
            return identity;
        }

        @Nullable
        IterableHelper.SuccessHandler getSuccessCallback() {
            return successCallback;
        }

        boolean hasFailedPriorAuth() {
            return hasFailedPriorAuth;
        }

        boolean shouldIgnoreRetryPolicy() {
            return shouldIgnoreRetryPolicy;
        }
    }

    static final class EnqueueResult<I> {
        private final EnqueueStatus status;
        @Nullable
        private final Request<I> requestToStart;

        EnqueueResult(EnqueueStatus status, @Nullable Request<I> requestToStart) {
            this.status = status;
            this.requestToStart = requestToStart;
        }

        EnqueueStatus getStatus() {
            return status;
        }

        @Nullable
        Request<I> getRequestToStart() {
            return requestToStart;
        }
    }

    static final class Completion<I> {
        private final boolean resultAccepted;
        @Nullable
        private final Request<I> nextRequest;

        Completion(boolean resultAccepted, @Nullable Request<I> nextRequest) {
            this.resultAccepted = resultAccepted;
            this.nextRequest = nextRequest;
        }

        boolean isResultAccepted() {
            return resultAccepted;
        }

        @Nullable
        Request<I> getNextRequest() {
            return nextRequest;
        }
    }

    @Nullable
    private Request<I> activeRequest;
    @Nullable
    private Request<I> queuedRequest;

    synchronized EnqueueResult<I> enqueue(
            I identity,
            @Nullable IterableHelper.SuccessHandler successCallback,
            boolean hasFailedPriorAuth,
            boolean shouldIgnoreRetryPolicy
    ) {
        Request<I> request = new Request<>(
                identity,
                successCallback,
                hasFailedPriorAuth,
                shouldIgnoreRetryPolicy
        );
        if (activeRequest == null) {
            activeRequest = request;
            return new EnqueueResult<>(EnqueueStatus.STARTED, request);
        }
        if (hasFailedPriorAuth) {
            return new EnqueueResult<>(EnqueueStatus.IGNORED_FAILED_RETRY, null);
        }
        if (activeRequest.getIdentity() == identity) {
            return new EnqueueResult<>(
                    EnqueueStatus.ALREADY_ACTIVE_FOR_IDENTITY,
                    null
            );
        }

        queuedRequest = request;
        return new EnqueueResult<>(EnqueueStatus.QUEUED_FOR_NEW_IDENTITY, null);
    }

    synchronized boolean isCurrent(Request<I> request, I currentIdentity) {
        return activeRequest == request && request.getIdentity() == currentIdentity;
    }

    synchronized Completion<I> complete(Request<I> request, I currentIdentity) {
        if (activeRequest != request) {
            return new Completion<>(false, null);
        }

        boolean resultAccepted = request.getIdentity() == currentIdentity;
        activeRequest = null;

        Request<I> nextRequest = null;
        if (queuedRequest != null && queuedRequest.getIdentity() == currentIdentity) {
            nextRequest = queuedRequest;
            activeRequest = nextRequest;
        }
        queuedRequest = null;
        return new Completion<>(resultAccepted, nextRequest);
    }

    synchronized void clearQueued() {
        queuedRequest = null;
    }
}
