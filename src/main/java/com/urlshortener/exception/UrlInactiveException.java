package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a short URL exists but was manually
 * deactivated by its owner
 *
 * Also 410 Gone — the URL existed and worked
 * but has been deliberately turned off
 */
@ResponseStatus(HttpStatus.GONE)
public class UrlInactiveException extends RuntimeException {

    public UrlInactiveException(String shortCode) {
        super("Short URL '" + shortCode +
              "' has been deactivated");
    }
}