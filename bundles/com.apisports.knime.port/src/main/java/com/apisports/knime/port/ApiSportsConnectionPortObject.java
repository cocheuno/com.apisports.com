package com.apisports.knime.port;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.core.model.Sport;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

import javax.swing.JComponent;
import java.util.Objects;

/**
 * Port object representing an API-Sports connection.
 * Carries the authenticated HTTP client between nodes.
 */
public class ApiSportsConnectionPortObject implements PortObject {
    
    /**
     * Port type for API-Sports connections.
     */
    public static final PortType TYPE = PortTypeRegistry.getInstance()
        .getPortType(ApiSportsConnectionPortObject.class);

    private final ApiSportsConnectionPortObjectSpec spec;
    private final transient ApiSportsHttpClient client;

    public ApiSportsConnectionPortObject(ApiSportsConnectionPortObjectSpec spec,
                                        ApiSportsHttpClient client) {
        this.spec = Objects.requireNonNull(spec, "Spec cannot be null");
        this.client = client; // Can be null after deserialization
    }

    @Override
    public ApiSportsConnectionPortObjectSpec getSpec() {
        return spec;
    }

    /**
     * Get the HTTP client for making API requests.
     *
     * @return The HTTP client
     * @throws IllegalStateException if the client is not available (e.g., after deserialization)
     */
    public ApiSportsHttpClient getClient() {
        if (client == null) {
            throw new IllegalStateException(
                "API client is not available. Please reset and re-execute the API-Sports Connector node.");
        }
        return client;
    }

    /**
     * Get the sport configured for this connection.
     * 
     * @return The sport
     */
    public Sport getSport() {
        return spec.getSport();
    }

    @Override
    public String getSummary() {
        return String.format("API-Sports Connection: %s (%s)", 
            spec.getSport().getDisplayName(), 
            spec.getTierName());
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
        if (!(obj instanceof ApiSportsConnectionPortObject)) {
            return false;
        }
        ApiSportsConnectionPortObject other = (ApiSportsConnectionPortObject) obj;
        return spec.equals(other.spec);
    }

    @Override
    public int hashCode() {
        return spec.hashCode();
    }
}
