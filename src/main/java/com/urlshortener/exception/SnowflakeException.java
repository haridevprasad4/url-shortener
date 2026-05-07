package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the Snowflake ID generator fails
 *
 * This should never happen in normal operation
 * If it does something is seriously wrong:
 * - Clock moved backwards significantly
 * - Invalid datacenter or machine ID configuration
 *
 * HTTP 500 Internal Server Error
 * This is our fault not the user's fault
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SnowflakeException extends RuntimeException {

    public SnowflakeException(
            String message,
            Throwable cause) {
        // Pass both message and original cause
        // so the full stack trace is preserved in logs
        super(message, cause);
    }
}