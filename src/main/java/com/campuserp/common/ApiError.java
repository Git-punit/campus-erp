package com.campuserp.common;

import java.time.Instant;
import java.util.Map;

/**
 * The body of every error response the API sends back.
 * fieldErrors is only filled in for validation failures (field name -> message).
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, Map.of());
    }
}
