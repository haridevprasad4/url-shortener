package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Returned for every error that happens in the API
 *
 * Example JSON:
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Short URL not found for code: 'xyz'",
 *   "timestamp": "2025-04-01T12:30:00",
 *   "path": "/r/xyz"
 * }
 *
 * WHY a consistent error format?
 * Without this every error looks different
 * Frontend developers hate inconsistent errors
 * They never know which field has the message
 * One format means one parser on the frontend
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(

        // HTTP status code number
        // 404, 409, 410, 422, 500
        int status,

        // Human readable status name
        // "Not Found", "Conflict", "Gone"
        String error,

        // Specific message about what went wrong
        String message,

        // When the error happened
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,

        // Which URL caused the error
        String path

) {
        // Static factory for clean construction
        public static ErrorResponse of(
                        int status,
                        String error,
                        String message,
                        String path) {
                return new ErrorResponse(
                        status,
                        error,
                        message,
                        LocalDateTime.now(),
                        path
                );
        }
}