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
    private final String dbPath;

    public ReferenceDataPortObjectSpec(long loadedTimestamp, String dbPath) {
        this.loadedTimestamp = loadedTimestamp;
        this.dbPath = dbPath;
    }

    public long getLoadedTimestamp() {
        return loadedTimestamp;
    }

    public String getDbPath() {
        return dbPath;
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
            && Objects.equals(dbPath, other.dbPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loadedTimestamp, dbPath);
    }

    @Override
    public String toString() {
        return String.format("ReferenceData[dbPath=%s, loaded=%d]", dbPath, loadedTimestamp);
    }
}
