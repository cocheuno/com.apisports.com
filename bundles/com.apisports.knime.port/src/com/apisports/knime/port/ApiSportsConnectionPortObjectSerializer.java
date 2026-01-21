/*
 * Copyright 2025 Carone Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        // The port object was saved without the client (for security)
        // We create a minimal port object with spec only
        // The client will be null, so nodes must check for this
        ApiSportsConnectionPortObjectSpec connSpec = (ApiSportsConnectionPortObjectSpec) spec;
        return new ApiSportsConnectionPortObject(connSpec, null);
    }

    @Override
    public void savePortObject(ApiSportsConnectionPortObject portObject,
                              PortObjectZipOutputStream out,
                              ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        // We intentionally don't save the client or API key for security reasons
    }
}
