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

package com.apisports.knime.football.nodes.query.odds;

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
 * Node model for Odds queries.
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
 *   0: BufferedDataTable (odds results)
 */
public class OddsNodeModel extends NodeModel {

    public OddsNodeModel() {
        super(
            new PortType[]{
                ApiSportsConnectionPortObject.TYPE,
                ReferenceDataPortObject.TYPE,
                BufferedDataTable.TYPE  // Fixtures input
            },
            new PortType[]{
                BufferedDataTable.TYPE  // Odds output
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

        getLogger().info("Processing odds for " + fixtureIds.size() + " fixtures");
        System.out.println("=============================================================");
        System.out.println("ODDS NODE: Processing odds for " + fixtureIds.size() + " fixtures");
        System.out.println("Fixture IDs from input: " + fixtureIds);
        System.out.println("=============================================================");

        // Query odds for each fixture and aggregate results
        ObjectMapper mapper = new ObjectMapper();
        DataTableSpec outputSpec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        int rowNum = 0;
        int fixtureCount = 0;

        for (Integer fixtureId : fixtureIds) {
            exec.checkCanceled();
            exec.setProgress((double) fixtureCount / fixtureIds.size(),
                "Querying odds for fixture " + fixtureId);

            try {
                Map<String, String> params = new HashMap<>();
                params.put("fixture", String.valueOf(fixtureId));

                System.out.println("\n--- Querying odds for fixture ID: " + fixtureId + " ---");
                System.out.println("API endpoint: /odds with params: " + params);

                JsonNode response = callApi(client, "/odds", params, mapper);

                // Log response size for debugging
                if (response != null && response.isArray()) {
                    System.out.println("API returned " + response.size() + " odds items");

                    // Log first item's fixture ID to see what we're getting
                    if (response.size() > 0) {
                        JsonNode firstItem = response.get(0);
                        JsonNode firstFixture = firstItem.get("fixture");
                        int firstFixtureId = firstFixture != null && firstFixture.has("id") ?
                            firstFixture.get("id").asInt() : -1;
                        System.out.println("First odds item has fixture ID: " + firstFixtureId);
                        System.out.println("REQUESTED: " + fixtureId + " | RECEIVED: " + firstFixtureId +
                                         " | MATCH: " + (fixtureId == firstFixtureId));
                    }
                } else {
                    System.out.println("API returned null or non-array response");
                }

                // Pass the requested fixture ID to parser for validation
                int rowsBefore = rowNum;
                int addedRows = parseOddsResponse(response, container, rowNum, fixtureId, exec);
                System.out.println("Added " + (addedRows - rowsBefore) + " odds rows for fixture " + fixtureId);
                rowNum = addedRows;
            } catch (Exception e) {
                System.out.println("ERROR: Failed to get odds for fixture " + fixtureId + ": " + e.getMessage());
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
     * Parse odds API response and add rows to container.
     * Only includes odds for the requested fixture ID.
     * Returns the updated row number.
     */
    private int parseOddsResponse(JsonNode response, BufferedDataContainer container, int startRowNum,
                                  int requestedFixtureId, ExecutionContext exec) {
        int rowNum = startRowNum;
        int skippedItems = 0;
        int processedItems = 0;

        if (response != null && response.isArray()) {
            System.out.println("  Parsing " + response.size() + " odds items, expecting fixture ID: " + requestedFixtureId);

            for (JsonNode oddsItem : response) {
                try {
                    // Each odds item may have multiple bookmakers and bets
                    JsonNode fixture = oddsItem.get("fixture");
                    JsonNode league = oddsItem.get("league");
                    JsonNode bookmakers = oddsItem.get("bookmakers");

                    int fixtureId = fixture != null && fixture.has("id") ? fixture.get("id").asInt() : 0;

                    // VALIDATE: Only process odds for the requested fixture
                    if (fixtureId != requestedFixtureId) {
                        System.out.println("  MISMATCH: Skipping fixture " + fixtureId +
                                         " (expected " + requestedFixtureId + ")");
                        skippedItems++;
                        continue;
                    }

                    System.out.println("  MATCH: Processing odds for fixture " + fixtureId);
                    processedItems++;

                    String leagueName = league != null && league.has("name") ? league.get("name").asText() : "";

                    if (bookmakers != null && bookmakers.isArray()) {
                        for (JsonNode bookmaker : bookmakers) {
                            String bookmakerId = bookmaker.has("id") ? String.valueOf(bookmaker.get("id").asInt()) : "";
                            String bookmakerName = bookmaker.has("name") ? bookmaker.get("name").asText() : "";

                            JsonNode bets = bookmaker.get("bets");
                            if (bets != null && bets.isArray()) {
                                for (JsonNode bet : bets) {
                                    String betName = bet.has("name") ? bet.get("name").asText() : "";

                                    JsonNode values = bet.get("values");
                                    if (values != null && values.isArray()) {
                                        for (JsonNode value : values) {
                                            DataRow row = parseOddsRow(fixtureId, leagueName, bookmakerId,
                                                                       bookmakerName, betName, value, rowNum);
                                            container.addRowToTable(row);
                                            rowNum++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    getLogger().warn("Failed to parse odds row: " + e.getMessage());
                }
            }

            System.out.println("  Summary: " + processedItems + " items MATCHED, " +
                             skippedItems + " items SKIPPED");

            if (skippedItems > 0) {
                System.out.println("  WARNING: Skipped " + skippedItems + " odds items with mismatched fixture IDs");
            }
        } else {
            System.out.println("  Response is null or not an array");
        }

        return rowNum;
    }

    /**
     * Parse a single odds value into a DataRow.
     */
    private DataRow parseOddsRow(int fixtureId, String leagueName, String bookmakerId,
                                  String bookmakerName, String betName, JsonNode value, int rowNum) {
        String valueName = value.has("value") ? value.get("value").asText() : "";
        String odd = value.has("odd") ? value.get("odd").asText() : "";

        DataCell[] cells = new DataCell[]{
            new IntCell(fixtureId),
            new StringCell(leagueName),
            new StringCell(bookmakerId),
            new StringCell(bookmakerName),
            new StringCell(betName),
            new StringCell(valueName),
            new StringCell(odd)
        };

        return new DefaultRow(new RowKey("Row" + rowNum), cells);
    }

    private DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Fixture_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("League", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Bookmaker_ID", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Bookmaker_Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Bet_Type", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Bet_Value", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Odd", StringCell.TYPE).createSpec()
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
