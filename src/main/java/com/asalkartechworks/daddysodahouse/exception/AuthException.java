package com.asalkartechworks.daddysodahouse.exception;

/**
 * Exception thrown on authentication failures such as invalid credentials or inactive account.
 */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }
}
