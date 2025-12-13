package com.apisports.knime.core.exception;

/**
 * Base exception class for all API-Sports related errors.
 */
public class ApiSportsException extends Exception {
    
    private static final long serialVersionUID = 1L;

    public ApiSportsException(String message) {
        super(message);
    }

    public ApiSportsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiSportsException(Throwable cause) {
        super(cause);
    }
}
