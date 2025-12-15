package com.apisports.knime.core.model;

/**
 * Enum representing supported sports in API-Sports.
 */
public enum Sport {
    FOOTBALL("football", "Football", "football.api-sports.io"),
    BASKETBALL("basketball", "Basketball", "basketball.api-sports.io"),
    FORMULA_1("formula-1", "Formula 1", "formula1.api-sports.io"),
    NFL("nfl", "NFL", "nfl.api-sports.io"),
    NBA("nba", "NBA", "nba.api-sports.io"),
    MLB("mlb", "MLB", "baseball.api-sports.io"),
    NHL("nhl", "NHL", "hockey.api-sports.io"),
    RUGBY("rugby", "Rugby", "rugby.api-sports.io"),
    HANDBALL("handball", "Handball", "handball.api-sports.io"),
    VOLLEYBALL("volleyball", "Volleyball", "volleyball.api-sports.io");

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

    public static Sport fromDisplayName(String displayName) {
        for (Sport sport : values()) {
            if (sport.displayName.equals(displayName)) {
                return sport;
            }
        }
        throw new IllegalArgumentException("Unknown sport display name: " + displayName);
    }

    /**
     * Flexible method that accepts either an ID or display name and returns the corresponding Sport.
     * Tries ID first, then display name for backward compatibility.
     */
    public static Sport from(String value) {
        // Try ID first
        for (Sport sport : values()) {
            if (sport.id.equals(value)) {
                return sport;
            }
        }
        // Try display name for backward compatibility
        for (Sport sport : values()) {
            if (sport.displayName.equals(value)) {
                return sport;
            }
        }
        throw new IllegalArgumentException("Unknown sport: " + value);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
