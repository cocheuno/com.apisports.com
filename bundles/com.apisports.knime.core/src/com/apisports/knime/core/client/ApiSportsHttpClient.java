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

package com.apisports.knime.core.client;

import com.apisports.knime.core.exception.ApiSportsException;
import com.apisports.knime.core.exception.RateLimitExceededException;
import com.apisports.knime.core.model.Sport;
import com.apisports.knime.core.ratelimit.RateLimiterManager;
import com.apisports.knime.core.cache.CacheManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Generic HTTP client for API-Sports endpoints.
 * Handles authentication, rate limiting, caching, and retry logic.
 */
public class ApiSportsHttpClient {
    
    private static final String API_KEY_HEADER = "x-apisports-key";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    
    private final HttpClient httpClient;
    private final String apiKey;
    private final Sport sport;
    private final RateLimiterManager rateLimiter;
    private final CacheManager cacheManager;
    private final RequestPipeline requestPipeline;
    private int apiCallCount = 0;
    private int cacheHitCount = 0;

    public ApiSportsHttpClient(String apiKey, Sport sport, RateLimiterManager rateLimiter,
                               CacheManager cacheManager) {
        this.apiKey = apiKey;
        this.sport = sport;
        this.rateLimiter = rateLimiter;
        this.cacheManager = cacheManager;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(DEFAULT_TIMEOUT)
            .build();
        this.requestPipeline = new RequestPipeline();
    }

    /**
     * Execute a GET request to the specified endpoint.
     * 
     * @param endpoint The API endpoint path
     * @param queryParams Query parameters
     * @return The response body as a String
     * @throws ApiSportsException if the request fails
     */
    public String get(String endpoint, Map<String, String> queryParams) throws ApiSportsException {
        // Check cache first
        String cacheKey = buildCacheKey(endpoint, queryParams);
        String cachedResponse = cacheManager.get(cacheKey);
        if (cachedResponse != null) {
            cacheHitCount++;
            return cachedResponse;
        }

        // Check rate limit
        if (!rateLimiter.tryAcquire(apiKey)) {
            throw new RateLimitExceededException(
                "Rate limit exceeded for API key", 
                rateLimiter.getRetryAfterSeconds(apiKey)
            );
        }

        // Build request
        URI uri = buildUri(endpoint, queryParams);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .header(API_KEY_HEADER, apiKey)
            .header("Accept", "application/json")
            .timeout(DEFAULT_TIMEOUT)
            .GET()
            .build();

        // Execute with retry logic
        try {
            apiCallCount++;  // Increment call count
            HttpResponse<String> response = requestPipeline.execute(httpClient, request);

            if (response.statusCode() == 200) {
                String body = response.body();
                cacheManager.put(cacheKey, body);
                return body;
            } else if (response.statusCode() == 429) {
                throw new RateLimitExceededException(
                    "Rate limit exceeded (HTTP 429)", 
                    60
                );
            } else {
                throw new ApiSportsException(
                    "API request failed with status " + response.statusCode() + ": " + response.body()
                );
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiSportsException("Request execution failed", e);
        }
    }

    private URI buildUri(String endpoint, Map<String, String> queryParams) {
        StringBuilder url = new StringBuilder("https://v3.");
        url.append(sport.getBaseUrl());
        url.append(endpoint);
        
        if (queryParams != null && !queryParams.isEmpty()) {
            url.append("?");
            queryParams.forEach((key, value) -> 
                url.append(key).append("=").append(value).append("&")
            );
            url.setLength(url.length() - 1); // Remove trailing &
        }
        
        return URI.create(url.toString());
    }

    private String buildCacheKey(String endpoint, Map<String, String> queryParams) {
        StringBuilder key = new StringBuilder(sport.getId())
            .append(":")
            .append(endpoint);
        
        if (queryParams != null && !queryParams.isEmpty()) {
            key.append(":");
            queryParams.forEach((k, v) -> key.append(k).append("=").append(v).append(";"));
        }
        
        return key.toString();
    }

    public Sport getSport() {
        return sport;
    }

    /**
     * Get the total number of API calls made (excluding cache hits).
     */
    public int getApiCallCount() {
        return apiCallCount;
    }

    /**
     * Get the number of cache hits.
     */
    public int getCacheHitCount() {
        return cacheHitCount;
    }

    /**
     * Get total number of requests (API calls + cache hits).
     */
    public int getTotalRequestCount() {
        return apiCallCount + cacheHitCount;
    }
}
