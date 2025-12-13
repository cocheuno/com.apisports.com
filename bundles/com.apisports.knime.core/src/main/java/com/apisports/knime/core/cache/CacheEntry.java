package com.apisports.knime.core.cache;

import java.time.Instant;

/**
 * Represents a cached entry with TTL (Time To Live).
 */
public record CacheEntry(String key, String value, Instant expiresAt) {
    
    /**
     * Check if this cache entry has expired.
     * 
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
