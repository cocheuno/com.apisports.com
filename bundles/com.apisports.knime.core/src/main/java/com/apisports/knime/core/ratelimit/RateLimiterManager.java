/*
 * Copyright 2025 Carone Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apisports.knime.core.ratelimit;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter using token bucket algorithm.
 * Manages per-API-key rate limiting with configurable limits.
 */
public class RateLimiterManager {
    
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 100;
    private static final int DEFAULT_REQUESTS_PER_DAY = 10000;
    
    private final Map<String, TokenBucket> buckets;
    private final int requestsPerMinute;
    private final int requestsPerDay;

    public RateLimiterManager() {
        this(DEFAULT_REQUESTS_PER_MINUTE, DEFAULT_REQUESTS_PER_DAY);
    }

    public RateLimiterManager(int requestsPerMinute, int requestsPerDay) {
        this.requestsPerMinute = requestsPerMinute;
        this.requestsPerDay = requestsPerDay;
        this.buckets = new ConcurrentHashMap<>();
    }

    /**
     * Try to acquire a token for the given API key.
     * 
     * @param apiKey The API key
     * @return true if a token was acquired, false if rate limit exceeded
     */
    public synchronized boolean tryAcquire(String apiKey) {
        TokenBucket bucket = buckets.computeIfAbsent(apiKey, 
            k -> new TokenBucket(requestsPerMinute, requestsPerDay));
        return bucket.tryAcquire();
    }

    /**
     * Get the number of seconds to wait before retrying.
     * 
     * @param apiKey The API key
     * @return Seconds to wait
     */
    public long getRetryAfterSeconds(String apiKey) {
        TokenBucket bucket = buckets.get(apiKey);
        return bucket != null ? bucket.getRetryAfterSeconds() : 60;
    }

    /**
     * Reset rate limits for a specific API key.
     * 
     * @param apiKey The API key
     */
    public void reset(String apiKey) {
        buckets.remove(apiKey);
    }

    /**
     * Reset all rate limits.
     */
    public void resetAll() {
        buckets.clear();
    }

    /**
     * Token bucket implementation for rate limiting.
     */
    private static class TokenBucket {
        private final int minuteLimit;
        private final int dayLimit;
        private final AtomicInteger minuteTokens;
        private final AtomicInteger dayTokens;
        private volatile Instant minuteWindowStart;
        private volatile Instant dayWindowStart;

        TokenBucket(int minuteLimit, int dayLimit) {
            this.minuteLimit = minuteLimit;
            this.dayLimit = dayLimit;
            this.minuteTokens = new AtomicInteger(minuteLimit);
            this.dayTokens = new AtomicInteger(dayLimit);
            this.minuteWindowStart = Instant.now();
            this.dayWindowStart = Instant.now();
        }

        synchronized boolean tryAcquire() {
            Instant now = Instant.now();
            
            // Reset minute window if needed
            if (now.isAfter(minuteWindowStart.plusSeconds(60))) {
                minuteTokens.set(minuteLimit);
                minuteWindowStart = now;
            }
            
            // Reset day window if needed
            if (now.isAfter(dayWindowStart.plusSeconds(86400))) {
                dayTokens.set(dayLimit);
                dayWindowStart = now;
            }
            
            // Try to consume tokens
            if (minuteTokens.get() > 0 && dayTokens.get() > 0) {
                minuteTokens.decrementAndGet();
                dayTokens.decrementAndGet();
                return true;
            }
            
            return false;
        }

        long getRetryAfterSeconds() {
            Instant now = Instant.now();
            
            // If minute limit exceeded, wait until next minute
            if (minuteTokens.get() <= 0) {
                long secondsSinceMinuteStart = now.getEpochSecond() - minuteWindowStart.getEpochSecond();
                return Math.max(1, 60 - secondsSinceMinuteStart);
            }
            
            // If day limit exceeded, wait until next day
            if (dayTokens.get() <= 0) {
                long secondsSinceDayStart = now.getEpochSecond() - dayWindowStart.getEpochSecond();
                return Math.max(1, 86400 - secondsSinceDayStart);
            }
            
            return 60; // Default retry after 1 minute
        }
    }
}
