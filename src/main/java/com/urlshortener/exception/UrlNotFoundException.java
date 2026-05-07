package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a short code does not exist in the database
 *
 * @ResponseStatus maps this exception to HTTP 404
 * if Spring's default exception resolution handles it
 * before GlobalExceptionHandler kicks in
 *
 * We store the shortCode so the error response
 * can include which code was not found
 * Much more helpful than a generic "not found" message
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UrlNotFoundException extends RuntimeException {

    private final String shortCode;

    public UrlNotFoundException(String shortCode) {
        // Calls RuntimeException constructor with the message
        // This message appears in logs and error responses
        super("Short URL not found for code: '" +
              shortCode + "'");
        this.shortCode = shortCode;
    }

    public String getShortCode() {
        return shortCode;
    }
}