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
 * Serializer for Reference Data port objects.
 * Handles persistence of database path reference.
 */
public class ReferenceDataPortObjectSerializer extends PortObjectSerializer<ReferenceDataPortObject> {

    /**
     * Spec serializer for ReferenceDataPortObjectSpec.
     */
    public static final class SpecSerializer extends PortObjectSpecSerializer<ReferenceDataPortObjectSpec> {

        @Override
        public ReferenceDataPortObjectSpec loadPortObjectSpec(PortObjectSpecZipInputStream in)
                throws IOException {
            ZipEntry entry = in.getNextEntry();
            if (entry == null) {
                throw new IOException("Missing spec data in port object stream");
            }

            DataInputStream dataIn = new DataInputStream(in);
            long loadedTimestamp = dataIn.readLong();
            String dbPath = dataIn.readUTF();

            return new ReferenceDataPortObjectSpec(loadedTimestamp, dbPath);
        }

        @Override
        public void savePortObjectSpec(ReferenceDataPortObjectSpec spec, PortObjectSpecZipOutputStream out)
                throws IOException {
            out.putNextEntry(new ZipEntry("spec.data"));
            DataOutputStream dataOut = new DataOutputStream(out);
            dataOut.writeLong(spec.getLoadedTimestamp());
            dataOut.writeUTF(spec.getDbPath() != null ? spec.getDbPath() : "");
            dataOut.flush();
            out.closeEntry();
        }
    }

    @Override
    public ReferenceDataPortObject loadPortObject(PortObjectZipInputStream in,
                                                   PortObjectSpec spec,
                                                   ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        ZipEntry entry = in.getNextEntry();
        if (entry == null) {
            // No data entry, just use spec
            ReferenceDataPortObjectSpec refSpec = (ReferenceDataPortObjectSpec) spec;
            return new ReferenceDataPortObject(refSpec.getDbPath());
        }

        DataInputStream dataIn = new DataInputStream(in);
        String dbPath = dataIn.readUTF();
        return new ReferenceDataPortObject(dbPath);
    }

    @Override
    public void savePortObject(ReferenceDataPortObject portObject,
                               PortObjectZipOutputStream out,
                               ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        out.putNextEntry(new ZipEntry("data"));
        DataOutputStream dataOut = new DataOutputStream(out);
        dataOut.writeUTF(portObject.getDbPath() != null ? portObject.getDbPath() : "");
        dataOut.flush();
        out.closeEntry();
    }
}
