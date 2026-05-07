package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user requests a custom alias
 * that is already taken by another URL
 *
 * HTTP 409 Conflict — the request conflicts
 * with the current state of the server
 * The alias exists — you cannot have it
 *
 * We store the alias so the error response
 * can tell the user exactly which alias is taken
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class AliasAlreadyExistsException
        extends RuntimeException {

    private final String alias;

    public AliasAlreadyExistsException(String alias) {
        super("Custom alias '" + alias +
              "' is already in use");
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }
}