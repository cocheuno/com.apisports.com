package com.apisports.knime.core.exception;

/**
 * Exception thrown when API rate limit is exceeded.
 */
public class RateLimitExceededException extends ApiSportsException {
    
    private static final long serialVersionUID = 1L;
    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
