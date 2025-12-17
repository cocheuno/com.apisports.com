package com.apisports.knime.football.nodes.universal;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.core.descriptor.DescriptorRegistry;
import com.apisports.knime.core.descriptor.EndpointDescriptor;
import com.apisports.knime.core.descriptor.ParameterDescriptor;
import com.apisports.knime.port.ApiSportsConnectionPortObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Universal Node Model - dynamically executes endpoints based on descriptors.
 * This is a prototype implementation demonstrating the descriptor-driven architecture.
 */
public class UniversalNodeModel extends NodeModel {

    static final String CFGKEY_ENDPOINT_ID = "endpointId";
    static final String CFGKEY_PARAMETERS = "parameters";

    private final SettingsModelString m_endpointId = new SettingsModelString(CFGKEY_ENDPOINT_ID, "");
    private final SettingsModelString m_parameters = new SettingsModelString(CFGKEY_PARAMETERS, "{}");

    protected UniversalNodeModel() {
        super(new PortType[]{ApiSportsConnectionPortObject.TYPE},
              new PortType[]{BufferedDataTable.TYPE});

        // Initialize descriptor registry
        try {
            // Load from this bundle's resources (football bundle, not core)
            InputStream stream = getClass().getResourceAsStream("/descriptors/football-endpoints.yaml");
            if (stream != null) {
                DescriptorRegistry.getInstance().loadFromStream(stream);
            } else {
                getLogger().warn("Descriptor file not found in football bundle");
            }
        } catch (Exception e) {
            getLogger().error("Failed to load endpoint descriptors: " + e.getMessage(), e);
        }
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        ApiSportsConnectionPortObject connection = (ApiSportsConnectionPortObject) inObjects[0];
        ApiSportsHttpClient client = connection.getClient();

        // Get endpoint descriptor
        String endpointId = m_endpointId.getStringValue();
        if (endpointId == null || endpointId.isEmpty()) {
            throw new InvalidSettingsException("No endpoint selected");
        }

        EndpointDescriptor descriptor = DescriptorRegistry.getInstance().getDescriptor(endpointId);
        if (descriptor == null) {
            throw new InvalidSettingsException("Endpoint not found: " + endpointId);
        }

        // Parse parameters - JSON can have integers, so parse as Object first then convert to String
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> paramsRaw = mapper.readValue(m_parameters.getStringValue(), HashMap.class);

        // Convert all values to strings for API client
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, Object> entry : paramsRaw.entrySet()) {
            params.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
        }

        // Validate parameters
        validateParameters(descriptor, params);

        // Execute API call
        exec.setMessage("Calling " + descriptor.getPath() + "...");
        String responseBody = client.get(descriptor.getPath(), params);

        // Parse response
        JsonNode jsonResponse = mapper.readTree(responseBody);

        // Check for API errors
        if (jsonResponse.has("errors") && jsonResponse.get("errors").size() > 0) {
            throw new Exception("API returned errors: " + jsonResponse.get("errors"));
        }

        // Get data array from response
        JsonNode dataArray = jsonResponse.get(descriptor.getResponse().getRootPath());
        if (dataArray == null) {
            throw new Exception("Response missing expected root path: " + descriptor.getResponse().getRootPath());
        }

        // Flatten to table (simple implementation for prototype)
        BufferedDataTable outputTable = flattenJsonToTable(dataArray, exec, descriptor);

        exec.setMessage("Complete - processed " + outputTable.size() + " rows");
        return new PortObject[]{outputTable};
    }

    /**
     * Validate parameters against descriptor rules.
     */
    private void validateParameters(EndpointDescriptor descriptor, Map<String, String> params)
            throws InvalidSettingsException {
        // Check required parameters
        for (String required : descriptor.getValidation().getRequiredParams()) {
            if (!params.containsKey(required) || params.get(required).isEmpty()) {
                throw new InvalidSettingsException("Required parameter missing: " + required);
            }
        }

        // Check "at least one of" rule
        if (!descriptor.getValidation().getRequiresAtLeastOneOf().isEmpty()) {
            boolean found = false;
            for (String param : descriptor.getValidation().getRequiresAtLeastOneOf()) {
                if (params.containsKey(param) && !params.get(param).isEmpty()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new InvalidSettingsException(
                    "At least one of these parameters is required: " +
                    String.join(", ", descriptor.getValidation().getRequiresAtLeastOneOf()));
            }
        }
    }

    /**
     * Flatten JSON array to KNIME table (simple prototype implementation).
     * TODO: Implement full flattening engine with descriptor rules.
     */
    private BufferedDataTable flattenJsonToTable(JsonNode dataArray, ExecutionContext exec,
                                                   EndpointDescriptor descriptor) throws Exception {
        if (!dataArray.isArray() || dataArray.size() == 0) {
            // Return empty table with minimal spec
            DataTableSpec emptySpec = new DataTableSpec(
                new DataColumnSpecCreator("_empty", StringCell.TYPE).createSpec());
            BufferedDataContainer container = exec.createDataContainer(emptySpec);
            container.close();
            return container.getTable();
        }

        // Analyze first row to determine schema
        JsonNode firstRow = dataArray.get(0);
        List<DataColumnSpec> columnSpecs = new ArrayList<>();
        List<String> columnKeys = new ArrayList<>();

        // Simple flattening: get all top-level fields
        Iterator<String> fieldNames = firstRow.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode fieldValue = firstRow.get(fieldName);

            // Determine type
            if (fieldValue.isInt()) {
                columnSpecs.add(new DataColumnSpecCreator(fieldName, IntCell.TYPE).createSpec());
            } else {
                columnSpecs.add(new DataColumnSpecCreator(fieldName, StringCell.TYPE).createSpec());
            }
            columnKeys.add(fieldName);
        }

        DataTableSpec outputSpec = new DataTableSpec(columnSpecs.toArray(new DataColumnSpec[0]));
        BufferedDataContainer container = exec.createDataContainer(outputSpec);

        // Process each row
        int rowCount = dataArray.size();
        for (int i = 0; i < rowCount; i++) {
            exec.checkCanceled();
            exec.setProgress((double) i / rowCount);

            JsonNode row = dataArray.get(i);
            DataCell[] cells = new DataCell[columnKeys.size()];

            for (int j = 0; j < columnKeys.size(); j++) {
                String key = columnKeys.get(j);
                JsonNode value = row.get(key);

                if (value == null || value.isNull()) {
                    cells[j] = columnSpecs.get(j).getType().getMissingCell();
                } else if (value.isInt()) {
                    cells[j] = new IntCell(value.asInt());
                } else {
                    cells[j] = new StringCell(value.asText());
                }
            }

            container.addRowToTable(new DefaultRow("Row" + i, cells));
        }

        container.close();
        return container.getTable();
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // Cannot determine output spec without execution
        return new PortObjectSpec[]{null};
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_endpointId.saveSettingsTo(settings);
        m_parameters.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_endpointId.loadSettingsFrom(settings);
        m_parameters.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_endpointId.validateSettings(settings);
        m_parameters.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException {
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException {
    }

    @Override
    protected void reset() {
    }
}
