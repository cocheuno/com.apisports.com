package com.apisports.knime.port;

import com.apisports. knime.core.model.Sport;
import org.knime.core.node.CanceledExecutionException;
import org. knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObject.PortObjectSerializer;
import org.knime. core.node.port.PortObjectSpec;
import org. knime.core. node.port.PortObjectSpec.PortObjectSpecSerializer;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime. core.node.port.PortObjectZipOutputStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;

/**
 * Serializer for API-Sports connection port objects. 
 * Handles persistence of connection configuration. 
 */
public class ApiSportsConnectionPortObjectSerializer extends PortObjectSerializer<ApiSportsConnectionPortObject> {

    /**
     * Spec serializer for ApiSportsConnectionPortObjectSpec.
     */
    public static final class SpecSerializer extends PortObjectSpecSerializer<ApiSportsConnectionPortObjectSpec> {
        
        @Override
        public ApiSportsConnectionPortObjectSpec loadPortObjectSpec(PortObjectZipInputStream in) 
                throws IOException {
            // Read the spec data from a zip entry
            ZipEntry entry = in.getNextEntry();
            if (entry == null) {
                throw new IOException("Missing spec data in port object stream");
            }
            
            // Don't close DataInputStream to avoid closing the underlying zip stream
            DataInputStream dataIn = new DataInputStream(in);
            String sportId = dataIn.readUTF();
            String apiKeyHash = dataIn. readUTF();
            String tierName = dataIn. readUTF();
            
            Sport sport = Sport.fromId(sportId);
            return new ApiSportsConnectionPortObjectSpec(sport, apiKeyHash, tierName);
        }

        @Override
        public void savePortObjectSpec(ApiSportsConnectionPortObjectSpec spec, PortObjectZipOutputStream out) 
                throws IOException {
            // Write the spec data to a zip entry
            out.putNextEntry(new ZipEntry("spec.data"));
            // Don't close DataOutputStream to avoid closing the underlying zip stream
            DataOutputStream dataOut = new DataOutputStream(out);
            dataOut.writeUTF(spec.getSport().getId());
            dataOut.writeUTF(spec. getApiKeyHash());
            dataOut. writeUTF(spec.getTierName());
            dataOut.flush(); // Ensure all data is written
            out.closeEntry();
        }
    }

    @Override
    public ApiSportsConnectionPortObject loadPortObject(PortObjectZipInputStream in,
                                                       PortObjectSpec spec,
                                                       ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        // Note: We cannot serialize the actual HTTP client or credentials
        // The client must be recreated by the source node
        ApiSportsConnectionPortObjectSpec connSpec = (ApiSportsConnectionPortObjectSpec) spec;
        
        // This is a limitation - deserialized connections won't have working clients
        // In a real implementation, you'd need to store encrypted credentials or require re-authentication
        throw new IOException("Port object deserialization not fully supported - " +
                            "connection must be recreated from source node");
    }

    @Override
    public void savePortObject(ApiSportsConnectionPortObject portObject,
                              PortObjectZipOutputStream out,
                              ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        // We intentionally don't save the client or API key for security reasons
        // Only the spec is saved (which is handled by the framework via the SpecSerializer)
    }
}
