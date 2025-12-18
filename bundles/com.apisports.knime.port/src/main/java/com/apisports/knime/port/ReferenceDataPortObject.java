package com.apisports.knime.port;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

import javax.swing.JComponent;
import java.util.Objects;

/**
 * Port object containing reference data (countries, leagues, teams, venues).
 * Used to populate UI dropdowns in query nodes and share cached reference data.
 */
public class ReferenceDataPortObject implements PortObject {

    /**
     * Port type for reference data.
     */
    public static final PortType TYPE = PortTypeRegistry.getInstance()
        .getPortType(ReferenceDataPortObject.class);

    private final ReferenceDataPortObjectSpec spec;
    private final ReferenceData data;

    public ReferenceDataPortObject(ReferenceData data) {
        this.data = Objects.requireNonNull(data, "Reference data cannot be null");
        this.spec = new ReferenceDataPortObjectSpec(
            data.getLoadedTimestamp(),
            data.getCountries().size(),
            data.getLeagues().size(),
            data.getTeams().size(),
            data.getVenues().size()
        );
    }

    @Override
    public ReferenceDataPortObjectSpec getSpec() {
        return spec;
    }

    /**
     * Get the reference data.
     *
     * @return The reference data
     */
    public ReferenceData getData() {
        return data;
    }

    @Override
    public String getSummary() {
        return String.format("Reference Data: %d countries, %d leagues, %d teams, %d venues",
            spec.getCountryCount(), spec.getLeagueCount(),
            spec.getTeamCount(), spec.getVenueCount());
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
        return spec.equals(other.spec);
    }

    @Override
    public int hashCode() {
        return spec.hashCode();
    }
}
