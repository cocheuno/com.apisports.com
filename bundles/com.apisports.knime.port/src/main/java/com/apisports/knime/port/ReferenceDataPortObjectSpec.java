package com.apisports.knime.port;

import org.knime.core.node.port.PortObjectSpec;

import javax.swing.JComponent;
import java.util.Objects;

/**
 * Specification for reference data port object.
 * Contains metadata about the reference data without the actual data.
 */
public class ReferenceDataPortObjectSpec implements PortObjectSpec {

    private final long loadedTimestamp;
    private final int countryCount;
    private final int leagueCount;
    private final int teamCount;
    private final int venueCount;

    public ReferenceDataPortObjectSpec(long loadedTimestamp, int countryCount,
                                      int leagueCount, int teamCount, int venueCount) {
        this.loadedTimestamp = loadedTimestamp;
        this.countryCount = countryCount;
        this.leagueCount = leagueCount;
        this.teamCount = teamCount;
        this.venueCount = venueCount;
    }

    public long getLoadedTimestamp() {
        return loadedTimestamp;
    }

    public int getCountryCount() {
        return countryCount;
    }

    public int getLeagueCount() {
        return leagueCount;
    }

    public int getTeamCount() {
        return teamCount;
    }

    public int getVenueCount() {
        return venueCount;
    }

    /**
     * Check if the reference data is stale (older than TTL).
     *
     * @param ttlSeconds Time-to-live in seconds
     * @return true if data is stale
     */
    public boolean isStale(int ttlSeconds) {
        long ageMillis = System.currentTimeMillis() - loadedTimestamp;
        long ageSeconds = ageMillis / 1000;
        return ageSeconds > ttlSeconds;
    }

    @Override
    public JComponent[] getViews() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ReferenceDataPortObjectSpec)) {
            return false;
        }
        ReferenceDataPortObjectSpec other = (ReferenceDataPortObjectSpec) obj;
        return loadedTimestamp == other.loadedTimestamp
            && countryCount == other.countryCount
            && leagueCount == other.leagueCount
            && teamCount == other.teamCount
            && venueCount == other.venueCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(loadedTimestamp, countryCount, leagueCount, teamCount, venueCount);
    }

    @Override
    public String toString() {
        return String.format("ReferenceData[countries=%d, leagues=%d, teams=%d, venues=%d]",
            countryCount, leagueCount, teamCount, venueCount);
    }
}
