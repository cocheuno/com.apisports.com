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

package com.apisports.knime.football.nodes.query.predictions;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.port.ApiSportsConnectionPortObject;
import com.apisports.knime.port.ReferenceDataPortObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.*;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import java.io.File;
import java.util.*;

/**
 * Node model for Predictions queries.
 *
 * This node extends NodeModel directly (not AbstractFootballQueryNodeModel) because
 * it requires 3 input ports instead of the standard 2.
 *
 * Input Ports:
 *   0: ApiSportsConnectionPortObject (API connection)
 *   1: ReferenceDataPortObject (reference data)
 *   2: BufferedDataTable (fixtures from Fixtures node)
 *
 * Output Ports:
 *   0: BufferedDataTable (prediction results)
 */
public class PredictionsNodeModel extends NodeModel {

    public PredictionsNodeModel() {
        super(
            new PortType[]{
                ApiSportsConnectionPortObject.TYPE,
                ReferenceDataPortObject.TYPE,
                BufferedDataTable.TYPE  // Fixtures input
            },
            new PortType[]{
                BufferedDataTable.TYPE  // Predictions output
            }
        );
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        // Get API client from connection port
        ApiSportsConnectionPortObject connectionPort = (ApiSportsConnectionPortObject) inObjects[0];
        ApiSportsHttpClient client = connectionPort.getClient();

        // Get fixtures table from input port
        BufferedDataTable fixturesTable = (BufferedDataTable) inObjects[2];

        // Find Fixture_ID column
        DataTableSpec fixturesSpec = fixturesTable.getDataTableSpec();
        int fixtureIdIdx = fixturesSpec.findColumnIndex("Fixture_ID");

        if (fixtureIdIdx < 0) {
            throw new InvalidSettingsException(
                "Input table must contain a 'Fixture_ID' column. Please connect a Fixtures node output.");
        }

        // Collect unique fixture IDs from input table
        Set<Integer> fixtureIds = new LinkedHashSet<>();
        RowIterator rowIterator = fixturesTable.iterator();
        while (rowIterator.hasNext()) {
            DataRow row = rowIterator.next();
            DataCell cell = row.getCell(fixtureIdIdx);
            if (!cell.isMissing() && cell instanceof IntCell) {
                fixtureIds.add(((IntCell) cell).getIntValue());
            }
        }

        if (fixtureIds.isEmpty()) {
            throw new InvalidSettingsException(
                "No fixture IDs found in input table. Please ensure the Fixtures node executed successfully.");
        }

        getLogger().info("Processing predictions for " + fixtureIds.size() + " fixtures");

        // Query predictions for each fixture and aggregate results
        ObjectMapper mapper = new ObjectMapper();
        DataTableSpec outputSpec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        int rowNum = 0;
        int fixtureCount = 0;

        for (Integer fixtureId : fixtureIds) {
            exec.checkCanceled();
            exec.setProgress((double) fixtureCount / fixtureIds.size(),
                "Querying predictions for fixture " + fixtureId);

            try {
                Map<String, String> params = new HashMap<>();
                params.put("fixture", String.valueOf(fixtureId));

                JsonNode response = callApi(client, "/predictions", params, mapper);
                rowNum = parseResponse(response, container, rowNum, exec);
            } catch (Exception e) {
                getLogger().warn("Failed to get predictions for fixture " + fixtureId + ": " + e.getMessage());
            }

            fixtureCount++;
        }

        container.close();
        return new PortObject[]{container.getTable()};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // Check required ports
        if (inSpecs[0] == null) {
            throw new InvalidSettingsException("API connection required");
        }
        if (inSpecs[1] == null) {
            throw new InvalidSettingsException("Reference data required");
        }
        if (inSpecs[2] == null) {
            throw new InvalidSettingsException(
                "Fixtures table required. Please connect a Fixtures node output to the second input port.");
        }

        // Verify fixtures table has Fixture_ID column
        if (inSpecs[2] instanceof DataTableSpec) {
            DataTableSpec fixturesSpec = (DataTableSpec) inSpecs[2];
            if (fixturesSpec.findColumnIndex("Fixture_ID") < 0) {
                throw new InvalidSettingsException(
                    "Input table must contain a 'Fixture_ID' column. Please connect a Fixtures node output.");
            }
        }

        return new PortObjectSpec[]{getOutputSpec()};
    }

    /**
     * Make API call to Football API.
     */
    private JsonNode callApi(ApiSportsHttpClient client, String endpoint,
                            Map<String, String> params, ObjectMapper mapper) throws Exception {
        String jsonResponse = client.get(endpoint, params);
        JsonNode root = mapper.readTree(jsonResponse);

        // Check for errors
        if (root.has("errors") && !root.get("errors").isEmpty()) {
            throw new Exception("API returned errors: " + root.get("errors").toString());
        }

        // Return response array
        return root.has("response") ? root.get("response") : null;
    }

    /**
     * Parse prediction response and add rows to container.
     * Returns the updated row number.
     */
    private int parseResponse(JsonNode response, BufferedDataContainer container, int startRowNum,
                             ExecutionContext exec) {
        int rowNum = startRowNum;

        if (response != null && response.isArray()) {
            for (JsonNode item : response) {
                try {
                    JsonNode predictions = item.get("predictions");
                    JsonNode league = item.get("league");
                    JsonNode teams = item.get("teams");

                    int fixtureId = 0;
                    if (item.has("fixture") && item.get("fixture").has("id")) {
                        fixtureId = item.get("fixture").get("id").asInt();
                    }

                    String leagueName = league != null && league.has("name") ? league.get("name").asText() : "";
                    String winner = predictions != null && predictions.has("winner") && predictions.get("winner").has("name")
                        ? predictions.get("winner").get("name").asText() : "";
                    String winPercent = predictions != null && predictions.has("percent") && predictions.get("percent").has(winner.toLowerCase())
                        ? predictions.get("percent").get(winner.toLowerCase()).asText() : "";
                    String advice = predictions != null && predictions.has("advice") ? predictions.get("advice").asText() : "";

                    String homeTeam = teams != null && teams.has("home") && teams.get("home").has("name")
                        ? teams.get("home").get("name").asText() : "";
                    String awayTeam = teams != null && teams.has("away") && teams.get("away").has("name")
                        ? teams.get("away").get("name").asText() : "";

                    DataCell[] cells = new DataCell[]{
                        new IntCell(fixtureId),
                        new StringCell(leagueName),
                        new StringCell(homeTeam),
                        new StringCell(awayTeam),
                        new StringCell(winner),
                        new StringCell(winPercent),
                        new StringCell(advice)
                    };
                    container.addRowToTable(new DefaultRow(new RowKey("Row" + rowNum), cells));
                    rowNum++;
                } catch (Exception e) {
                    getLogger().warn("Failed to parse prediction: " + e.getMessage());
                }
            }
        }

        return rowNum;
    }

    private DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Fixture_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("League", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Home_Team", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Away_Team", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Winner", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Win_Percent", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Advice", StringCell.TYPE).createSpec()
        );
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // No settings to save - node configuration comes from input ports
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // No settings to validate
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // No settings to load
    }

    @Override
    protected void reset() {
        // Nothing to reset
    }

    @Override
    protected void loadInternals(File nodeInternDir, ExecutionMonitor exec) {
        // No internals to load
    }

    @Override
    protected void saveInternals(File nodeInternDir, ExecutionMonitor exec) {
        // No internals to save
    }
}
