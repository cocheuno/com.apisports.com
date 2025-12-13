package com.apisports.knime.port;

import com.apisports.knime.core.model.Sport;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObject.PortObjectSerializer;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpec.PortObjectSpecSerializer;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;

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
        public ApiSportsConnectionPortObjectSpec loadPortObjectSpec(PortObjectSpecZipInputStream in) 
                throws IOException {
            ZipEntry entry = in.getNextEntry();
            if (entry == null) {
                throw new IOException("Missing spec data in port object stream");
            }
            
            DataInputStream dataIn = new DataInputStream(in);
            String sportId = dataIn.readUTF();
            String apiKeyHash = dataIn.readUTF();
            String tierName = dataIn.readUTF();
            
            Sport sport = Sport.fromId(sportId);
            return new ApiSportsConnectionPortObjectSpec(sport, apiKeyHash, tierName);
        }

        @Override
        public void savePortObjectSpec(ApiSportsConnectionPortObjectSpec spec, PortObjectSpecZipOutputStream out) 
                throws IOException {
            out.putNextEntry(new ZipEntry("spec. data"));
            DataOutputStream dataOut = new DataOutputStream(out);
            dataOut.writeUTF(spec.getSport().getId());
            dataOut.writeUTF(spec.getApiKeyHash());
            dataOut.writeUTF(spec.getTierName());
            dataOut.flush();
            out.closeEntry();
        }
    }

    @Override
    public ApiSportsConnectionPortObject loadPortObject(PortObjectZipInputStream in,
                                                       PortObjectSpec spec,
                                                       ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        ApiSportsConnectionPortObjectSpec connSpec = (ApiSportsConnectionPortObjectSpec) spec;
        throw new IOException("Port object deserialization not fully supported - " +
                            "connection must be recreated from source node");
    }

    @Override
    public void savePortObject(ApiSportsConnectionPortObject portObject,
                              PortObjectZipOutputStream out,
                              ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        // We intentionally don't save the client or API key for security reasons
    }
}
