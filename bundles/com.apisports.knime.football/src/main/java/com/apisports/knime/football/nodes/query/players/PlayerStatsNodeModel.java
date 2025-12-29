package com.apisports.knime.football.nodes.query.players;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeModel;
import com.apisports.knime.port.ApiSportsConnectionPortObject;
import com.apisports.knime.port.ReferenceDataPortObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NodeModel for Player Stats node.
 * Retrieves detailed match-by-match player statistics from the Football API.
 *
 * This node can work in multiple modes:
 * 1. Standalone: Uses dialog settings to query player stats by fixture ID
 * 2. Player IDs mode: Accepts optional input table with Player_ID column
 * 3. Fixture IDs mode: Accepts optional input table with Fixture_ID column
 */
public class PlayerStatsNodeModel extends AbstractFootballQueryNodeModel {

    // Player stats specific settings
    static final String CFGKEY_FIXTURE_ID = "fixtureId";

    protected final SettingsModelString m_fixtureId =
        new SettingsModelString(CFGKEY_FIXTURE_ID, "");

    /**
     * Constructor with optional third input port for Player IDs or Fixture IDs.
     */
    public PlayerStatsNodeModel() {
        super(
            new PortType[]{
                ApiSportsConnectionPortObject.TYPE,
                ReferenceDataPortObject.TYPE,
                PortType.OPTIONAL(BufferedDataTable.TYPE)  // Optional Player_ID or Fixture_ID input
            },
            new PortType[]{
                BufferedDataTable.TYPE
            }
        );
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        // Get API client from connection port
        ApiSportsConnectionPortObject connectionPort = (ApiSportsConnectionPortObject) inObjects[0];
        ApiSportsHttpClient client = connectionPort.getClient();

        // Get reference data from port
        ReferenceDataPortObject refDataPort = (ReferenceDataPortObject) inObjects[1];
        m_dbPath = refDataPort.getDbPath();

        // Load reference data from database
        loadReferenceData();

        // Check if optional input port is connected
        BufferedDataTable inputTable = null;
        if (inObjects.length > 2 && inObjects[2] != null) {
            inputTable = (BufferedDataTable) inObjects[2];
            getLogger().info("Input port connected - using " + inputTable.size() + " IDs");
        }

        // If input table provided, extract IDs and query
        if (inputTable != null) {
            BufferedDataTable result = executeWithInputTable(inputTable, client, new ObjectMapper(), exec);
            return new PortObject[]{result};
        } else {
            // No input - validate settings and use dialog configuration
            validateExecutionSettings();
            BufferedDataTable result = executeQuery(client, new ObjectMapper(), exec);
            return new PortObject[]{result};
        }
    }

    /**
     * Execute query using input table (either Player_ID or Fixture_ID column).
     */
    private BufferedDataTable executeWithInputTable(BufferedDataTable inputTable,
                                                      ApiSportsHttpClient client,
                                                      ObjectMapper mapper,
                                                      ExecutionContext exec) throws Exception {
        DataTableSpec spec = inputTable.getDataTableSpec();

        // Check which type of ID column is present
        int fixtureIdColIndex = spec.findColumnIndex("Fixture_ID");
        int playerIdColIndex = spec.findColumnIndex("Player_ID");

        if (fixtureIdColIndex >= 0) {
            // Fixture IDs provided - get player stats for all players in those fixtures
            List<Integer> fixtureIds = extractIntColumn(inputTable, fixtureIdColIndex);
            getLogger().info("Querying player stats for " + fixtureIds.size() + " fixtures");
            return queryByFixtureIds(fixtureIds, client, mapper, exec);

        } else if (playerIdColIndex >= 0) {
            // Player IDs provided - get stats for specific players
            // Note: API requires fixture ID, so we'd need to query all fixtures for the season
            // This is expensive - for now, throw informative error
            throw new InvalidSettingsException(
                "Player Stats node currently requires Fixture_ID input, not Player_ID. " +
                "Use Fixtures Selector to select fixtures, then connect to this node.");

        } else {
            throw new InvalidSettingsException(
                "Input table must contain either 'Fixture_ID' or 'Player_ID' column");
        }
    }

    /**
     * Extract integer values from a specific column in the input table.
     */
    private List<Integer> extractIntColumn(BufferedDataTable table, int columnIndex) {
        List<Integer> values = new ArrayList<>();
        for (DataRow row : table) {
            DataCell cell = row.getCell(columnIndex);
            if (!cell.isMissing() && cell instanceof IntCell) {
                values.add(((IntCell) cell).getIntValue());
            }
        }
        return values;
    }

    /**
     * Query player stats for specific fixture IDs.
     */
    private BufferedDataTable queryByFixtureIds(List<Integer> fixtureIds,
                                                  ApiSportsHttpClient client,
                                                  ObjectMapper mapper,
                                                  ExecutionContext exec) throws Exception {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);

        int rowNum = 0;
        int fixtureCount = 0;

        for (Integer fixtureId : fixtureIds) {
            exec.checkCanceled();
            exec.setProgress((double) fixtureCount / fixtureIds.size(),
                "Processing fixture " + (fixtureCount + 1) + " of " + fixtureIds.size());

            Map<String, String> params = new HashMap<>();
            params.put("fixture", String.valueOf(fixtureId));

            try {
                JsonNode response = callApi(client, "/fixtures/players", params, mapper);

                if (response != null && response.isArray()) {
                    for (JsonNode teamData : response) {
                        JsonNode players = teamData.get("players");
                        if (players != null && players.isArray()) {
                            for (JsonNode playerData : players) {
                                try {
                                    DataRow row = parsePlayerStatsRow(fixtureId, teamData, playerData, rowNum);
                                    container.addRowToTable(row);
                                    rowNum++;
                                } catch (Exception e) {
                                    getLogger().warn("Failed to parse player stats for fixture " +
                                        fixtureId + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                getLogger().warn("Failed to get player stats for fixture " + fixtureId + ": " + e.getMessage());
            }

            fixtureCount++;
        }

        container.close();
        getLogger().info("Retrieved " + rowNum + " player performance records");
        return container.getTable();
    }

    @Override
    protected void validateExecutionSettings() throws InvalidSettingsException {
        if (m_fixtureId.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("Please specify a fixture ID or connect input table");
        }
    }

    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        exec.setMessage("Building query parameters...");

        Map<String, String> params = new HashMap<>();
        params.put("fixture", m_fixtureId.getStringValue());

        exec.setMessage("Querying player stats from API...");
        JsonNode response = callApi(client, "/fixtures/players", params, mapper);

        exec.setMessage("Parsing results...");
        BufferedDataTable result = parsePlayerStatsResponse(response, exec);

        getLogger().info("Retrieved " + result.size() + " player performance records");
        return result;
    }

    /**
     * Parse player stats API response for a single fixture.
     */
    private BufferedDataTable parsePlayerStatsResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);

        int rowNum = 0;

        if (response != null && response.isArray()) {
            // Response is array of team data
            for (JsonNode teamData : response) {
                JsonNode players = teamData.get("players");
                if (players != null && players.isArray()) {
                    for (JsonNode playerData : players) {
                        try {
                            // Parse fixture ID from input setting
                            int fixtureId = Integer.parseInt(m_fixtureId.getStringValue());
                            DataRow row = parsePlayerStatsRow(fixtureId, teamData, playerData, rowNum);
                            container.addRowToTable(row);
                            rowNum++;
                        } catch (Exception e) {
                            getLogger().warn("Failed to parse player stats row: " + e.getMessage());
                        }
                    }
                }
            }
        }

        container.close();
        return container.getTable();
    }

    /**
     * Parse a single player stats JSON object into a DataRow.
     * Based on fixtures-players-response.json structure.
     */
    private DataRow parsePlayerStatsRow(int fixtureId, JsonNode teamData,
                                         JsonNode playerData, int rowNum) {
        JsonNode team = teamData.get("team");
        JsonNode player = playerData.get("player");
        JsonNode statistics = playerData.get("statistics");

        // Extract first statistics entry (should only be one per fixture)
        JsonNode stats = statistics != null && statistics.isArray() && statistics.size() > 0
            ? statistics.get(0) : null;

        // 28 columns total
        DataCell[] cells = new DataCell[28];
        int colIdx = 0;

        // IDs
        cells[colIdx++] = new IntCell(fixtureId);
        cells[colIdx++] = getIntCell(player, "id");
        cells[colIdx++] = getIntCell(team, "id");

        // Player info
        cells[colIdx++] = getStringCell(player, "name");
        cells[colIdx++] = getStringCell(team, "name");

        // Game info
        JsonNode games = stats != null ? stats.get("games") : null;
        cells[colIdx++] = getIntCell(games, "minutes");
        cells[colIdx++] = getIntCell(games, "number");
        cells[colIdx++] = getStringCell(games, "position");
        cells[colIdx++] = getStringCell(games, "rating");
        cells[colIdx++] = getBooleanCell(games, "captain");
        cells[colIdx++] = getBooleanCell(games, "substitute");

        // Goals
        JsonNode goalsNode = stats != null ? stats.get("goals") : null;
        cells[colIdx++] = getIntCell(goalsNode, "total");
        cells[colIdx++] = getIntCell(goalsNode, "conceded");
        cells[colIdx++] = getIntCell(goalsNode, "assists");
        cells[colIdx++] = getIntCell(goalsNode, "saves");

        // Shots
        JsonNode shots = stats != null ? stats.get("shots") : null;
        cells[colIdx++] = getIntCell(shots, "total");
        cells[colIdx++] = getIntCell(shots, "on");

        // Passes
        JsonNode passes = stats != null ? stats.get("passes") : null;
        cells[colIdx++] = getIntCell(passes, "total");
        cells[colIdx++] = getIntCell(passes, "key");
        cells[colIdx++] = getStringCell(passes, "accuracy");

        // Tackles
        JsonNode tackles = stats != null ? stats.get("tackles") : null;
        cells[colIdx++] = getIntCell(tackles, "total");
        cells[colIdx++] = getIntCell(tackles, "blocks");
        cells[colIdx++] = getIntCell(tackles, "interceptions");

        // Dribbles
        JsonNode dribbles = stats != null ? stats.get("dribbles") : null;
        cells[colIdx++] = getIntCell(dribbles, "attempts");
        cells[colIdx++] = getIntCell(dribbles, "success");

        // Fouls and Cards
        JsonNode fouls = stats != null ? stats.get("fouls") : null;
        cells[colIdx++] = getIntCell(fouls, "drawn");
        cells[colIdx++] = getIntCell(fouls, "committed");

        JsonNode cards = stats != null ? stats.get("cards") : null;
        cells[colIdx++] = getIntCell(cards, "yellow");
        cells[colIdx++] = getIntCell(cards, "red");

        return new DefaultRow(new RowKey("Row" + rowNum), cells);
    }

    // Helper methods for safe data extraction
    private DataCell getIntCell(JsonNode node, String field) {
        if (node != null && node.has(field) && !node.get(field).isNull()) {
            return new IntCell(node.get(field).asInt());
        }
        return DataType.getMissingCell();
    }

    private DataCell getStringCell(JsonNode node, String field) {
        if (node != null && node.has(field) && !node.get(field).isNull()) {
            return new StringCell(node.get(field).asText());
        }
        return DataType.getMissingCell();
    }

    private DataCell getBooleanCell(JsonNode node, String field) {
        if (node != null && node.has(field) && !node.get(field).isNull()) {
            return new StringCell(node.get(field).asBoolean() ? "Yes" : "No");
        }
        return DataType.getMissingCell();
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        java.util.List<DataColumnSpec> columns = new java.util.ArrayList<>();

        // IDs (3 columns)
        columns.add(new DataColumnSpecCreator("Fixture_ID", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Player_ID", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Team_ID", IntCell.TYPE).createSpec());

        // Player/Team info (2 columns)
        columns.add(new DataColumnSpecCreator("Player_Name", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Team_Name", StringCell.TYPE).createSpec());

        // Game info (6 columns)
        columns.add(new DataColumnSpecCreator("Minutes", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Number", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Position", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Rating", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Captain", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Substitute", StringCell.TYPE).createSpec());

        // Goals (4 columns)
        columns.add(new DataColumnSpecCreator("Goals_Total", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Goals_Conceded", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Assists", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Saves", IntCell.TYPE).createSpec());

        // Shots (2 columns)
        columns.add(new DataColumnSpecCreator("Shots_Total", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Shots_On_Target", IntCell.TYPE).createSpec());

        // Passes (3 columns)
        columns.add(new DataColumnSpecCreator("Passes_Total", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Passes_Key", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Passes_Accuracy", StringCell.TYPE).createSpec());

        // Tackles (3 columns)
        columns.add(new DataColumnSpecCreator("Tackles_Total", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Tackles_Blocks", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Tackles_Interceptions", IntCell.TYPE).createSpec());

        // Dribbles (2 columns)
        columns.add(new DataColumnSpecCreator("Dribbles_Attempts", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Dribbles_Success", IntCell.TYPE).createSpec());

        // Fouls and Cards (4 columns)
        columns.add(new DataColumnSpecCreator("Fouls_Drawn", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Fouls_Committed", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Yellow_Cards", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Red_Cards", IntCell.TYPE).createSpec());

        return new DataTableSpec(columns.toArray(new DataColumnSpec[0]));
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_fixtureId.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_fixtureId.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_fixtureId.loadSettingsFrom(settings);
    }
}
