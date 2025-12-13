package com.apisports.knime.core.model;

/**
 * Enum representing supported sports in API-Sports.
 */
public enum Sport {
    FOOTBALL("football", "Football", "api-football.com"),
    BASKETBALL("basketball", "Basketball", "api-basketball.com"),
    FORMULA_1("formula-1", "Formula 1", "api-formula1.com"),
    NFL("nfl", "NFL", "api-nfl.com"),
    NBA("nba", "NBA", "api-nba.com"),
    MLB("mlb", "MLB", "api-mlb.com"),
    NHL("nhl", "NHL", "api-nhl.com"),
    RUGBY("rugby", "Rugby", "api-rugby.com"),
    HANDBALL("handball", "Handball", "api-handball.com"),
    VOLLEYBALL("volleyball", "Volleyball", "api-volleyball.com");

    private final String id;
    private final String displayName;
    private final String baseUrl;

    Sport(String id, String displayName, String baseUrl) {
        this.id = id;
        this.displayName = displayName;
        this.baseUrl = baseUrl;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public static Sport fromId(String id) {
        for (Sport sport : values()) {
            if (sport.id.equals(id)) {
                return sport;
            }
        }
        throw new IllegalArgumentException("Unknown sport ID: " + id);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
