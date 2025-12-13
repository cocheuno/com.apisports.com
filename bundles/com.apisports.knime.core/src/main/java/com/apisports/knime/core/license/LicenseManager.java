package com.apisports.knime.core.license;

import com.apisports.knime.core.exception.LicenseValidationException;
import com.apisports.knime.core.model.Sport;

import java.util.Set;
import java.util.HashSet;

/**
 * Manages license validation and feature gating.
 */
public class LicenseManager {
    
    public enum SubscriptionTier {
        FREE(100, 10000, Set.of()),
        BASIC(300, 30000, Set.of("standings", "stats")),
        PRO(1000, 100000, Set.of("standings", "stats", "predictions", "odds")),
        ULTRA(3000, 300000, Set.of("standings", "stats", "predictions", "odds", "livestream"));

        private final int requestsPerMinute;
        private final int requestsPerDay;
        private final Set<String> features;

        SubscriptionTier(int requestsPerMinute, int requestsPerDay, Set<String> features) {
            this.requestsPerMinute = requestsPerMinute;
            this.requestsPerDay = requestsPerDay;
            this.features = features;
        }

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public int getRequestsPerDay() {
            return requestsPerDay;
        }

        public boolean hasFeature(String feature) {
            return features.contains(feature);
        }
    }

    private String apiKey;
    private Sport sport;
    private SubscriptionTier tier;

    public LicenseManager() {
        this.tier = SubscriptionTier.FREE; // Default to free tier
    }

    /**
     * Validate the API key and determine subscription tier.
     * 
     * @param apiKey The API key to validate
     * @param sport The sport to validate for
     * @throws LicenseValidationException if validation fails
     */
    public void validate(String apiKey, Sport sport) throws LicenseValidationException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new LicenseValidationException("API key cannot be empty");
        }

        if (sport == null) {
            throw new LicenseValidationException("Sport must be specified");
        }

        // In a real implementation, this would make an API call to validate the key
        // For now, we'll use a simple heuristic based on key length
        this.apiKey = apiKey;
        this.sport = sport;
        this.tier = detectTier(apiKey);
    }

    /**
     * Check if a specific feature is available for the current license.
     * 
     * @param feature The feature name
     * @return true if feature is available, false otherwise
     */
    public boolean hasFeature(String feature) {
        return tier.hasFeature(feature);
    }

    /**
     * Get the current subscription tier.
     * 
     * @return The subscription tier
     */
    public SubscriptionTier getTier() {
        return tier;
    }

    /**
     * Get the API key.
     * 
     * @return The API key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Get the sport.
     * 
     * @return The sport
     */
    public Sport getSport() {
        return sport;
    }

    /**
     * Detect subscription tier based on API key.
     * This is a placeholder - real implementation would query the API.
     * 
     * @param apiKey The API key
     * @return The detected subscription tier
     */
    private SubscriptionTier detectTier(String apiKey) {
        // Placeholder logic - in reality, would call API to check subscription
        // For demo purposes, use key length as a proxy
        int keyLength = apiKey.length();
        if (keyLength > 60) {
            return SubscriptionTier.ULTRA;
        } else if (keyLength > 50) {
            return SubscriptionTier.PRO;
        } else if (keyLength > 40) {
            return SubscriptionTier.BASIC;
        } else {
            return SubscriptionTier.FREE;
        }
    }
}
