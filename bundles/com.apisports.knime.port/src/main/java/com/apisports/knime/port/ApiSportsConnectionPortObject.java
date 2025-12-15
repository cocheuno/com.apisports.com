package com.apisports.knime.port;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.core.model.Sport;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

import javax.swing.JComponent;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Port object representing an API-Sports connection.
 * Carries the authenticated HTTP client between nodes.
 */
public class ApiSportsConnectionPortObject implements PortObject {

    // Static registry to keep clients alive during execution session
    private static final ConcurrentHashMap<String, ApiSportsHttpClient> clientRegistry = new ConcurrentHashMap<>();

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

        // Register client in memory if not null
        if (client != null && spec != null) {
            String key = generateKey(spec);
            clientRegistry.put(key, client);
        }
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
        // Try to use the direct client reference first
        if (client != null) {
            return client;
        }

        // If null, try to get from registry (in case this is a deserialized object during same session)
        String key = generateKey(spec);
        ApiSportsHttpClient registeredClient = clientRegistry.get(key);
        if (registeredClient != null) {
            return registeredClient;
        }

        // No client available
        throw new IllegalStateException(
            "API client is not available. Please reset and re-execute the API-Sports Connector node.");
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

    /**
     * Generate a unique key for this connection based on its spec.
     */
    private static String generateKey(ApiSportsConnectionPortObjectSpec spec) {
        return spec.getSport().getId() + "_" + spec.getApiKeyHash() + "_" + spec.getTierName();
    }

    /**
     * Clear the client registry (for testing purposes).
     */
    public static void clearRegistry() {
        clientRegistry.clear();
    }
}
