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

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Handles request execution with retry and exponential backoff logic.
 */
public class RequestPipeline {
    
    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
    private static final double BACKOFF_MULTIPLIER = 2.0;

    /**
     * Execute an HTTP request with retry logic.
     * 
     * @param client The HTTP client
     * @param request The HTTP request
     * @return The HTTP response
     * @throws IOException if all retries fail
     * @throws InterruptedException if interrupted during retry
     */
    public HttpResponse<String> execute(HttpClient client, HttpRequest request) 
            throws IOException, InterruptedException {
        
        int attempt = 0;
        IOException lastException = null;
        
        while (attempt < MAX_RETRIES) {
            try {
                return client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                lastException = e;
                attempt++;
                
                if (attempt < MAX_RETRIES) {
                    long backoffMillis = (long) (INITIAL_BACKOFF.toMillis() * 
                                                Math.pow(BACKOFF_MULTIPLIER, attempt - 1));
                    Thread.sleep(backoffMillis);
                }
            }
        }
        
        throw lastException != null ? lastException : 
            new IOException("Request failed after " + MAX_RETRIES + " attempts");
    }
}
