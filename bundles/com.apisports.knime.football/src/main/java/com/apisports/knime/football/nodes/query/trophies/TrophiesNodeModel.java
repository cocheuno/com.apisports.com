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

package com.apisports.knime.football.nodes.query.trophies;

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

import java.util.*;

/**
 * Node model for Trophies queries.
 *
 * This node extends NodeModel directly (not AbstractFootballQueryNodeModel) because
 * it requires 3 input ports instead of the standard 2.
 *
 * Input Ports:
 *   0: ApiSportsConnectionPortObject (API connection)
 *   1: ReferenceDataPortObject (reference data)
 *   2: BufferedDataTable (players or coaches from Players/Coaches node)
 *
 * Output Ports:
 *   0: BufferedDataTable (trophies results)
 */
public class TrophiesNodeModel extends NodeModel {

    public TrophiesNodeModel() {
        super(
            new PortType[]{
                ApiSportsConnectionPortObject.TYPE,
                ReferenceDataPortObject.TYPE,
                BufferedDataTable.TYPE  // Players or Coaches input
            },
            new PortType[]{
                BufferedDataTable.TYPE  // Trophies output
            }
        );
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        // Get API client from connection port
        ApiSportsConnectionPortObject connectionPort = (ApiSportsConnectionPortObject) inObjects[0];
        ApiSportsHttpClient client = connectionPort.getClient();

        // Get players/coaches table from input port
        BufferedDataTable inputTable = (BufferedDataTable) inObjects[2];
        DataTableSpec inputSpec = inputTable.getDataTableSpec();

        // Determine if input is from Players or Coaches node
        int playerIdIdx = inputSpec.findColumnIndex("Player_ID");
        int coachIdIdx = inputSpec.findColumnIndex("Coach_ID");
        int nameIdx = inputSpec.findColumnIndex("Name");

        String paramType;
        int idColumnIdx;

        if (playerIdIdx >= 0) {
            paramType = "player";
            idColumnIdx = playerIdIdx;
            getLogger().info("Detected Players node input - will query player trophies");
            System.out.println("TROPHIES NODE: Detected Players node input");
        } else if (coachIdIdx >= 0) {
            paramType = "coach";
            idColumnIdx = coachIdIdx;
            getLogger().info("Detected Coaches node input - will query coach trophies");
            System.out.println("TROPHIES NODE: Detected Coaches node input");
        } else {
            throw new InvalidSettingsException(
                "Input table must contain either 'Player_ID' or 'Coach_ID' column. " +
                "Please connect a Players or Coaches node output.");
        }

        // Collect IDs and names from input table
        Map<Integer, String> idToNameMap = new LinkedHashMap<>();
        RowIterator rowIterator = inputTable.iterator();
        while (rowIterator.hasNext()) {
            DataRow row = rowIterator.next();
            DataCell idCell = row.getCell(idColumnIdx);

            if (!idCell.isMissing() && idCell instanceof IntCell) {
                int id = ((IntCell) idCell).getIntValue();
                String name = "";
                if (nameIdx >= 0) {
                    DataCell nameCell = row.getCell(nameIdx);
                    if (!nameCell.isMissing() && nameCell instanceof StringCell) {
                        name = ((StringCell) nameCell).getStringValue();
                    }
                }
                idToNameMap.put(id, name);
            }
        }

        if (idToNameMap.isEmpty()) {
            throw new InvalidSettingsException(
                "No IDs found in input table. Please ensure the " +
                (paramType.equals("player") ? "Players" : "Coaches") +
                " node executed successfully.");
        }

        getLogger().info("Processing trophies for " + idToNameMap.size() + " " + paramType + "s");
        System.out.println("=============================================================");
        System.out.println("TROPHIES NODE: Processing trophies for " + idToNameMap.size() + " " + paramType + "s");
        System.out.println(paramType.toUpperCase() + " IDs from input: " + idToNameMap.keySet());
        System.out.println("=============================================================");

        // Query trophies for each player/coach and aggregate results
        ObjectMapper mapper = new ObjectMapper();
        DataTableSpec outputSpec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        int rowNum = 0;
        int personCount = 0;

        for (Map.Entry<Integer, String> entry : idToNameMap.entrySet()) {
            exec.checkCanceled();
            exec.setProgress((double) personCount / idToNameMap.size(),
                "Querying trophies for " + paramType + " " + entry.getKey());

            try {
                Map<String, String> params = new HashMap<>();
                params.put(paramType, String.valueOf(entry.getKey()));

                System.out.println("Querying /trophies with " + paramType + "=" + entry.getKey() +
                                 " (" + entry.getValue() + ")");

                JsonNode response = callApi(client, "/trophies", params, mapper);

                // Parse trophies for this player/coach
                if (response != null && response.isArray()) {
                    System.out.println("  Found " + response.size() + " trophies for " + entry.getValue());

                    for (JsonNode item : response) {
                        try {
                            String personName = entry.getValue();
                            String league = item.has("league") ? item.get("league").asText() : "";
                            String country = item.has("country") ? item.get("country").asText() : "";
                            String season = item.has("season") ? item.get("season").asText() : "";
                            String place = item.has("place") ? item.get("place").asText() : "";

                            DataCell[] cells = new DataCell[]{
                                new IntCell(entry.getKey()),
                                new StringCell(personName),
                                new StringCell(league),
                                new StringCell(country),
                                new StringCell(season),
                                new StringCell(place)
                            };
                            container.addRowToTable(new DefaultRow(new RowKey("Row" + rowNum), cells));
                            rowNum++;
                        } catch (Exception e) {
                            getLogger().warn("Failed to parse trophy: " + e.getMessage());
                            System.out.println("  ERROR parsing trophy: " + e.getMessage());
                        }
                    }
                } else {
                    System.out.println("  No trophies found for " + entry.getValue());
                }
            } catch (Exception e) {
                getLogger().warn("Failed to query trophies for " + paramType + " " +
                               entry.getKey() + ": " + e.getMessage());
                System.out.println("  ERROR: " + e.getMessage());
            }

            personCount++;
        }

        container.close();
        getLogger().info("Retrieved " + rowNum + " total trophies");
        System.out.println("=============================================================");
        System.out.println("TROPHIES NODE: Retrieved " + rowNum + " total trophies");
        System.out.println("=============================================================");

        return new PortObject[]{container.getTable()};
    }

    /**
     * Helper method to make API call and parse JSON response.
     */
    private JsonNode callApi(ApiSportsHttpClient client, String endpoint,
                             Map<String, String> params, ObjectMapper mapper) throws Exception {
        String response = client.get(endpoint, params);
        JsonNode root = mapper.readTree(response);

        // Check for errors in response
        JsonNode errors = root.get("errors");
        if (errors != null && !errors.isEmpty()) {
            throw new Exception("API returned errors: " + errors.toString());
        }

        return root.get("response");
    }

    /**
     * Get the output table specification.
     */
    private DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Person_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Person_Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("League", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Country", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Season", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Place", StringCell.TYPE).createSpec()
        );
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // Validate that input port 2 is connected
        if (inSpecs[2] == null) {
            throw new InvalidSettingsException(
                "Please connect a Players or Coaches node to the third input port.");
        }

        // Validate that input has required columns
        if (inSpecs[2] instanceof DataTableSpec) {
            DataTableSpec inputSpec = (DataTableSpec) inSpecs[2];
            int playerIdIdx = inputSpec.findColumnIndex("Player_ID");
            int coachIdIdx = inputSpec.findColumnIndex("Coach_ID");

            if (playerIdIdx < 0 && coachIdIdx < 0) {
                throw new InvalidSettingsException(
                    "Input table must contain either 'Player_ID' or 'Coach_ID' column. " +
                    "Please connect a Players or Coaches node output.");
            }
        }

        return new PortObjectSpec[]{getOutputSpec()};
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // No settings to save
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
    protected void loadInternals(final java.io.File nodeInternDir,
                                 final org.knime.core.node.ExecutionMonitor exec)
            throws java.io.IOException, org.knime.core.node.CanceledExecutionException {
        // No internals to load
    }

    @Override
    protected void saveInternals(final java.io.File nodeInternDir,
                                 final org.knime.core.node.ExecutionMonitor exec)
            throws java.io.IOException, org.knime.core.node.CanceledExecutionException {
        // No internals to save
    }
}
