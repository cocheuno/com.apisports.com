package com.apisports.knime.port;

import com.apisports.knime.core.model.Sport;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.JComponent;
import java.util.Objects;

/**
 * Specification for API-Sports connection port object.
 * Contains connection configuration without the actual client instance.
 */
public class ApiSportsConnectionPortObjectSpec implements PortObjectSpec {
    
    private final Sport sport;
    private final String apiKeyHash; // Store hash instead of actual key for security
    private final String tierName;

    public ApiSportsConnectionPortObjectSpec(Sport sport, String apiKeyHash, String tierName) {
        this.sport = Objects.requireNonNull(sport, "Sport cannot be null");
        this.apiKeyHash = Objects.requireNonNull(apiKeyHash, "API key hash cannot be null");
        this.tierName = Objects.requireNonNull(tierName, "Tier name cannot be null");
    }

    public Sport getSport() {
        return sport;
    }

    public String getApiKeyHash() {
        return apiKeyHash;
    }

    public String getTierName() {
        return tierName;
    }

    @Override
    public JComponent[] getViews() {
        // Return null for now - could add a view showing connection details
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ApiSportsConnectionPortObjectSpec)) {
            return false;
        }
        ApiSportsConnectionPortObjectSpec other = (ApiSportsConnectionPortObjectSpec) obj;
        return sport == other.sport 
            && apiKeyHash.equals(other.apiKeyHash)
            && tierName.equals(other.tierName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sport, apiKeyHash, tierName);
    }

    @Override
    public String toString() {
        return String.format("ApiSportsConnection[sport=%s, tier=%s]", sport.getDisplayName(), tierName);
    }
}
