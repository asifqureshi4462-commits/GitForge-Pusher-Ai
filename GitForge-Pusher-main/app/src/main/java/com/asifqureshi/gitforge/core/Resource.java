package com.asifqureshi.gitforge.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.asifqureshi.gitforge.domain.github.ApiException;

/** Loading / success / error wrapper exposed by ViewModels through LiveData. */
public final class Resource<T> {

    public enum Status { LOADING, SUCCESS, ERROR }

    private final Status status;
    @Nullable
    private final T data;
    @Nullable
    private final ApiException error;
    @Nullable
    private final String errorMessage;

    private Resource(Status status, @Nullable T data, @Nullable ApiException error, @Nullable String errorMessage) {
        this.status = status;
        this.data = data;
        this.error = error;
        this.errorMessage = errorMessage;
    }

    public static <T> Resource<T> loading(@Nullable T previous) {
        return new Resource<>(Status.LOADING, previous, null, null);
    }

    public static <T> Resource<T> success(@NonNull T data) {
        return new Resource<>(Status.SUCCESS, data, null, null);
    }

    public static <T> Resource<T> error(@NonNull ApiException e, @Nullable T previous) {
        return new Resource<>(Status.ERROR, previous, e, null);
    }

    public static <T> Resource<T> error(@NonNull String message, @Nullable T previous) {
        return new Resource<>(Status.ERROR, previous, null, message);
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @Nullable
    public ApiException getError() {
        return error;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }
}
