package com.kresshy.weatherstation.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A generic class that contains data and its loading status. Used to communicate state (Loading,
 * Success, Error) from Repository/ViewModel to UI.
 *
 * @param <T> Type of the data payload.
 */
public class Resource<T> {

    /** Current status of the operation. */
    @NonNull public final Status status;

    /** The actual data payload. */
    @Nullable public final T data;

    /** Optional error message. */
    @Nullable public final String message;

    private Resource(@NonNull Status status, @Nullable T data, @Nullable String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    /**
     * Creates a Success resource. This is used when an operation has completed successfully and has
     * a valid data payload to return.
     *
     * @param data The result data.
     * @return A Resource with SUCCESS status.
     */
    public static <T> Resource<T> success(@NonNull T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    /**
     * Creates an Error resource. This is used when an operation has failed, providing an error
     * message and potentially returning stale or cached data.
     *
     * @param msg Error message.
     * @param data Previous/cached data if available.
     * @return A Resource with ERROR status.
     */
    public static <T> Resource<T> error(@NonNull String msg, @Nullable T data) {
        return new Resource<>(Status.ERROR, data, msg);
    }

    /**
     * Creates a Loading resource. This is used when an operation is currently in progress, allowing
     * the UI to show a progress indicator while still having access to previous data.
     *
     * @param data Previous/cached data if available.
     * @return A Resource with LOADING status.
     */
    public static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(Status.LOADING, data, null);
    }

    /** Enum for the possible states of a Resource. */
    public enum Status {
        SUCCESS,
        ERROR,
        LOADING
    }
}
