package com.apisports.knime.football.nodes.query.players;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeModel;
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

import java.util.HashMap;
import java.util.Map;

/**
 * NodeModel for Players Selector node.
 * Returns basic player information without detailed statistics.
 * Output can be filtered and fed into Player Stats node.
 */
public class PlayersSelectorNodeModel extends AbstractFootballQueryNodeModel {

    // Players-specific settings
    static final String CFGKEY_QUERY_TYPE = "queryType";
    static final String CFGKEY_PLAYER_NAME = "playerName";
    static final String CFGKEY_PLAYER_ID = "playerId";
    static final String CFGKEY_TEAM_IDS = "teamIds";  // Multi-selection team IDs

    // Query type options
    static final String QUERY_BY_TEAM = "Players by Team";
    static final String QUERY_BY_NAME = "Search by Name";
    static final String QUERY_BY_ID = "By Player ID";

    protected final SettingsModelString m_queryType =
        new SettingsModelString(CFGKEY_QUERY_TYPE, QUERY_BY_TEAM);
    protected final SettingsModelString m_playerName =
        new SettingsModelString(CFGKEY_PLAYER_NAME, "");
    protected final SettingsModelString m_playerId =
        new SettingsModelString(CFGKEY_PLAYER_ID, "");

    // Multi-selection team IDs (stored separately from SettingsModel pattern)
    private int[] m_teamIds = new int[]{};

    @Override
    protected void validateExecutionSettings() throws InvalidSettingsException {
        String queryType = m_queryType.getStringValue();

        // Specific validations
        if (QUERY_BY_NAME.equals(queryType) && m_playerName.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("Please specify a player name to search");
        }
        if (QUERY_BY_ID.equals(queryType) && m_playerId.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("Please specify a player ID");
        }
        if (QUERY_BY_TEAM.equals(queryType) && (m_teamIds == null || m_teamIds.length == 0 || m_teamIds[0] <= 0)) {
            throw new InvalidSettingsException("Please select a team");
        }
    }

    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        exec.setMessage("Building query parameters...");

        // Build query parameters based on query type
        Map<String, String> params = buildQueryParams();

        // Make API call
        exec.setMessage("Querying players from API...");
        JsonNode response = callApi(client, "/players", params, mapper);

        // Parse response and create output table with basic info only
        exec.setMessage("Parsing results...");
        BufferedDataTable result = parsePlayersResponse(response, exec);

        getLogger().info("Retrieved " + result.size() + " players");
        return result;
    }

    /**
     * Build query parameters based on selected query type.
     */
    private Map<String, String> buildQueryParams() {
        Map<String, String> params = new HashMap<>();
        String queryType = m_queryType.getStringValue();

        // Get first selected team ID (multi-selection UI uses first team)
        int firstTeamId = (m_teamIds != null && m_teamIds.length > 0) ? m_teamIds[0] : -1;
        // TODO: Support multiple teams by making multiple queries and combining results

        if (QUERY_BY_ID.equals(queryType)) {
            params.put("id", m_playerId.getStringValue());
            params.put("season", String.valueOf(m_season.getIntValue()));
        } else if (QUERY_BY_NAME.equals(queryType)) {
            params.put("search", m_playerName.getStringValue());
            if (m_leagueId.getIntValue() > 0) {
                params.put("league", String.valueOf(m_leagueId.getIntValue()));
            }
            if (firstTeamId > 0) {
                params.put("team", String.valueOf(firstTeamId));
            }
            params.put("season", String.valueOf(m_season.getIntValue()));
        } else if (QUERY_BY_TEAM.equals(queryType)) {
            params.put("team", String.valueOf(firstTeamId));
            params.put("season", String.valueOf(m_season.getIntValue()));
        }

        return params;
    }

    /**
     * Parse players API response and create output table with BASIC info only.
     */
    private BufferedDataTable parsePlayersResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);

        if (response != null && response.isArray()) {
            int rowNum = 0;

            for (JsonNode playerItem : response) {
                try {
                    DataRow row = parseBasicPlayerRow(playerItem, rowNum);
                    container.addRowToTable(row);
                    rowNum++;
                } catch (Exception e) {
                    getLogger().warn("Failed to parse player row " + rowNum + ": " + e.getMessage());
                }
            }
        }

        container.close();
        return container.getTable();
    }

    /**
     * Parse a single player JSON object into a DataRow with BASIC info only.
     * Returns: Player_ID, Name, Firstname, Lastname, Nationality, Age,
     *          Team_ID, Team_Name, Position
     */
    private DataRow parseBasicPlayerRow(JsonNode playerItem, int rowNum) {
        JsonNode player = playerItem.get("player");
        JsonNode statistics = playerItem.get("statistics");

        // 9 basic columns
        DataCell[] cells = new DataCell[9];
        int colIdx = 0;

        // Player basic info
        cells[colIdx++] = getIntCell(player, "id");
        cells[colIdx++] = getStringCell(player, "name");
        cells[colIdx++] = getStringCell(player, "firstname");
        cells[colIdx++] = getStringCell(player, "lastname");
        cells[colIdx++] = getStringCell(player, "nationality");
        cells[colIdx++] = getStringCell(player, "age");

        // Team info (from first statistics entry if available)
        if (statistics != null && statistics.isArray() && statistics.size() > 0) {
            JsonNode stats = statistics.get(0);
            JsonNode team = stats.get("team");

            cells[colIdx++] = getIntCell(team, "id");
            cells[colIdx++] = getStringCell(team, "name");

            JsonNode games = stats.get("games");
            cells[colIdx++] = getStringCell(games, "position");
        } else {
            // No statistics available
            cells[colIdx++] = DataType.getMissingCell();  // Team_ID
            cells[colIdx++] = DataType.getMissingCell();  // Team_Name
            cells[colIdx++] = DataType.getMissingCell();  // Position
        }

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
            String value = node.get(field).asText();
            return new StringCell(value);
        }
        return DataType.getMissingCell();
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Player_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Firstname", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Lastname", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Nationality", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Age", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Position", StringCell.TYPE).createSpec()
        );
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_queryType.saveSettingsTo(settings);
        m_playerName.saveSettingsTo(settings);
        m_playerId.saveSettingsTo(settings);
        settings.addIntArray(CFGKEY_TEAM_IDS, m_teamIds);  // Save multi-selection team IDs
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_queryType.validateSettings(settings);
        m_playerName.validateSettings(settings);
        m_playerId.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_queryType.loadSettingsFrom(settings);
        m_playerName.loadSettingsFrom(settings);
        m_playerId.loadSettingsFrom(settings);

        // Load multi-selection team IDs with backward compatibility
        if (settings.containsKey(CFGKEY_TEAM_IDS)) {
            m_teamIds = settings.getIntArray(CFGKEY_TEAM_IDS);
        } else {
            // Backward compatibility: use single team ID from parent class if new array doesn't exist
            m_teamIds = (m_teamId.getIntValue() > 0) ? new int[]{m_teamId.getIntValue()} : new int[]{};
        }
    }
}
