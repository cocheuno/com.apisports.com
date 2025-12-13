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
