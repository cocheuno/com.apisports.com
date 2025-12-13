package com.apisports.knime.port;

import com.apisports.knime.core.cache.CacheManager;
import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.core.license.LicenseManager;
import com.apisports.knime.core.model.Sport;
import com.apisports.knime.core.ratelimit.RateLimiterManager;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

import java.io.IOException;

/**
 * Serializer for API-Sports connection port objects.
 * Handles persistence of connection configuration.
 */
public class ApiSportsConnectionPortObjectSerializer 
        extends org.knime.core.node.port.AbstractSimplePortObjectSerializer<ApiSportsConnectionPortObject> {

    @Override
    public ApiSportsConnectionPortObjectSpec loadPortObjectSpec(PortObjectSpecZipInputStream in) 
            throws IOException {
        String sportId = in.readUTF();
        String apiKeyHash = in.readUTF();
        String tierName = in.readUTF();
        
        Sport sport = Sport.fromId(sportId);
        return new ApiSportsConnectionPortObjectSpec(sport, apiKeyHash, tierName);
    }

    @Override
    public void savePortObjectSpec(PortObjectSpec spec, PortObjectSpecZipOutputStream out) 
            throws IOException {
        ApiSportsConnectionPortObjectSpec connSpec = (ApiSportsConnectionPortObjectSpec) spec;
        out.writeUTF(connSpec.getSport().getId());
        out.writeUTF(connSpec.getApiKeyHash());
        out.writeUTF(connSpec.getTierName());
    }

    @Override
    public ApiSportsConnectionPortObject loadPortObject(PortObjectZipInputStream in,
                                                       PortObjectSpec spec,
                                                       org.knime.core.node.ExecutionMonitor exec) 
            throws IOException {
        // Note: We cannot serialize the actual HTTP client or credentials
        // The client must be recreated by the source node
        // For now, create a dummy client - real implementation would require re-authentication
        ApiSportsConnectionPortObjectSpec connSpec = (ApiSportsConnectionPortObjectSpec) spec;
        
        // This is a limitation - deserialized connections won't have working clients
        // In a real implementation, you'd need to store encrypted credentials or require re-authentication
        throw new IOException("Port object deserialization not fully supported - " +
                            "connection must be recreated from source node");
    }

    @Override
    public void savePortObject(ApiSportsConnectionPortObject portObject,
                              PortObjectZipOutputStream out,
                              org.knime.core.node.ExecutionMonitor exec) 
            throws IOException {
        // We intentionally don't save the client or API key for security reasons
        // Only the spec is saved
    }
}
