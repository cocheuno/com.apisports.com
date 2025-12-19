package com.apisports.knime.port;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

import javax.swing.JComponent;
import java.io.File;
import java.util.Objects;

/**
 * Port object containing reference to SQLite database with reference data.
 * Used to populate UI dropdowns in query nodes and share cached reference data.
 */
public class ReferenceDataPortObject implements PortObject {

    /**
     * Port type for reference data.
     */
    public static final PortType TYPE = PortTypeRegistry.getInstance()
        .getPortType(ReferenceDataPortObject.class);

    private final ReferenceDataPortObjectSpec spec;
    private final String dbPath;

    /**
     * Constructor for SQLite-backed reference data.
     * @param dbPath Path to the SQLite database file
     */
    public ReferenceDataPortObject(String dbPath) {
        this.dbPath = Objects.requireNonNull(dbPath, "Database path cannot be null");
        File dbFile = new File(dbPath);
        this.spec = new ReferenceDataPortObjectSpec(
            dbFile.exists() ? dbFile.lastModified() : System.currentTimeMillis(),
            dbPath
        );
    }

    /**
     * Legacy constructor for in-memory reference data (backward compatibility).
     * @deprecated Use constructor with dbPath instead
     */
    @Deprecated
    public ReferenceDataPortObject(ReferenceData data) {
        Objects.requireNonNull(data, "Reference data cannot be null");
        this.dbPath = null; // In-memory mode (not recommended)
        this.spec = new ReferenceDataPortObjectSpec(
            data.getLoadedTimestamp(),
            null
        );
    }

    @Override
    public ReferenceDataPortObjectSpec getSpec() {
        return spec;
    }

    /**
     * Get the database file path.
     *
     * @return The path to the SQLite database file
     */
    public String getDbPath() {
        return dbPath;
    }

    /**
     * Check if database file exists.
     *
     * @return true if the database file exists
     */
    public boolean databaseExists() {
        if (dbPath == null) {
            return false;
        }
        return new File(dbPath).exists();
    }

    @Override
    public String getSummary() {
        if (dbPath != null) {
            File dbFile = new File(dbPath);
            if (dbFile.exists()) {
                long sizeKB = dbFile.length() / 1024;
                return String.format("Reference Data (SQLite): %s (%.1f KB)",
                    dbFile.getName(), sizeKB / 1024.0);
            } else {
                return "Reference Data (SQLite): " + dbPath + " (not found)";
            }
        }
        return "Reference Data (in-memory - deprecated)";
    }

    @Override
    public JComponent[] getViews() {
        return new JComponent[0];
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ReferenceDataPortObject)) {
            return false;
        }
        ReferenceDataPortObject other = (ReferenceDataPortObject) obj;
        return Objects.equals(dbPath, other.dbPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dbPath);
    }
}
