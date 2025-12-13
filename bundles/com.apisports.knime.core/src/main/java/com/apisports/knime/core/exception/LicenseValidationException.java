package com.apisports.knime.core.exception;

/**
 * Exception thrown when license validation fails.
 */
public class LicenseValidationException extends ApiSportsException {
    
    private static final long serialVersionUID = 1L;

    public LicenseValidationException(String message) {
        super(message);
    }

    public LicenseValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
