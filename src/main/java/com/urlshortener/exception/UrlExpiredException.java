package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a short URL exists but has passed
 * its expiry date
 *
 * HTTP 410 Gone is semantically correct:
 * "This resource existed here but is GONE
 *  and will not come back"
 *
 * Unlike 404 which means "might exist elsewhere
 * or might be temporary", 410 is final
 */
@ResponseStatus(HttpStatus.GONE)
public class UrlExpiredException extends RuntimeException {

    public UrlExpiredException(String shortCode) {
        super("Short URL '" + shortCode +
              "' has expired");
    }
}