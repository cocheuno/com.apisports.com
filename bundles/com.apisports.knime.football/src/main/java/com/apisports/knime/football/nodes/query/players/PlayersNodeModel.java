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
 * NodeModel for Players query node.
 * Queries player data and statistics from the Football API.
 */
public class PlayersNodeModel extends AbstractFootballQueryNodeModel {

    // Players-specific settings
    static final String CFGKEY_QUERY_TYPE = "queryType";
    static final String CFGKEY_PLAYER_NAME = "playerName";
    static final String CFGKEY_PLAYER_ID = "playerId";

    // Query type options
    static final String QUERY_TOP_SCORERS = "Top Scorers";
    static final String QUERY_TOP_ASSISTS = "Top Assists";
    static final String QUERY_TOP_YELLOW_CARDS = "Top Yellow Cards";
    static final String QUERY_TOP_RED_CARDS = "Top Red Cards";
    static final String QUERY_BY_TEAM = "Players by Team";
    static final String QUERY_BY_NAME = "Search by Name";
    static final String QUERY_BY_ID = "By Player ID";

    protected final SettingsModelString m_queryType =
        new SettingsModelString(CFGKEY_QUERY_TYPE, QUERY_TOP_SCORERS);
    protected final SettingsModelString m_playerName =
        new SettingsModelString(CFGKEY_PLAYER_NAME, "");
    protected final SettingsModelString m_playerId =
        new SettingsModelString(CFGKEY_PLAYER_ID, "");

    @Override
    protected void validateExecutionSettings() throws InvalidSettingsException {
        String queryType = m_queryType.getStringValue();

        // Most queries require league and season
        if (!QUERY_BY_ID.equals(queryType) && !QUERY_BY_TEAM.equals(queryType)) {
            super.validateExecutionSettings(); // Validates league and season
        }

        // Specific validations
        if (QUERY_BY_NAME.equals(queryType) && m_playerName.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("Please specify a player name to search");
        }
        if (QUERY_BY_ID.equals(queryType) && m_playerId.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("Please specify a player ID");
        }
        if (QUERY_BY_TEAM.equals(queryType) && m_teamId.getIntValue() <= 0) {
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
        String queryType = m_queryType.getStringValue();
        String endpoint = getEndpoint(queryType);
        exec.setMessage("Querying players from API...");
        JsonNode response = callApi(client, endpoint, params, mapper);

        // Parse response and create output table
        exec.setMessage("Parsing results...");
        BufferedDataTable result = parsePlayersResponse(response, exec);

        getLogger().info("Retrieved " + result.size() + " player records");
        return result;
    }

    /**
     * Get the appropriate endpoint based on query type.
     */
    private String getEndpoint(String queryType) {
        switch (queryType) {
            case QUERY_TOP_SCORERS:
                return "/players/topscorers";
            case QUERY_TOP_ASSISTS:
                return "/players/topassists";
            case QUERY_TOP_YELLOW_CARDS:
                return "/players/topyellowcards";
            case QUERY_TOP_RED_CARDS:
                return "/players/topredcards";
            default:
                return "/players";
        }
    }

    /**
     * Build query parameters based on selected query type.
     */
    private Map<String, String> buildQueryParams() {
        Map<String, String> params = new HashMap<>();
        String queryType = m_queryType.getStringValue();

        if (QUERY_BY_ID.equals(queryType)) {
            params.put("id", m_playerId.getStringValue());
            params.put("season", String.valueOf(m_season.getIntValue()));
        } else if (QUERY_BY_NAME.equals(queryType)) {
            params.put("search", m_playerName.getStringValue());
            if (m_leagueId.getIntValue() > 0) {
                params.put("league", String.valueOf(m_leagueId.getIntValue()));
            }
            // Add team filter for name searches when team is selected
            if (m_teamId.getIntValue() > 0) {
                params.put("team", String.valueOf(m_teamId.getIntValue()));
            }
            params.put("season", String.valueOf(m_season.getIntValue()));
        } else if (QUERY_BY_TEAM.equals(queryType)) {
            // Get all players for a specific team
            params.put("team", String.valueOf(m_teamId.getIntValue()));
            params.put("season", String.valueOf(m_season.getIntValue()));
        } else {
            // Top scorers/assists/cards queries
            params.put("league", String.valueOf(m_leagueId.getIntValue()));
            params.put("season", String.valueOf(m_season.getIntValue()));
            // Add team filter when team is selected
            if (m_teamId.getIntValue() > 0) {
                params.put("team", String.valueOf(m_teamId.getIntValue()));
            }
        }

        return params;
    }

    /**
     * Parse players API response and create output table.
     */
    private BufferedDataTable parsePlayersResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);

        if (response != null && response.isArray()) {
            int rowNum = 0;

            for (JsonNode playerItem : response) {
                try {
                    DataRow row = parsePlayerRow(playerItem, rowNum);
                    container.addRowToTable(row);
                    rowNum++;
                } catch (Exception e) {
                    getLogger().warn("Failed to parse player row: " + e.getMessage());
                }
            }
        }

        container.close();
        return container.getTable();
    }

    /**
     * Parse a single player JSON object into a DataRow.
     */
    private DataRow parsePlayerRow(JsonNode playerItem, int rowNum) {
        JsonNode player = playerItem.get("player");
        JsonNode statistics = playerItem.get("statistics");

        // Extract player basic info
        int playerId = player != null && player.has("id") ? player.get("id").asInt() : 0;
        String playerName = player != null && player.has("name") ? player.get("name").asText() : "";
        String firstname = player != null && player.has("firstname") ? player.get("firstname").asText() : "";
        String lastname = player != null && player.has("lastname") ? player.get("lastname").asText() : "";
        String nationality = player != null && player.has("nationality") ? player.get("nationality").asText() : "";
        String age = player != null && player.has("age") && !player.get("age").isNull()
            ? String.valueOf(player.get("age").asInt()) : "";

        // Extract statistics (from first statistics entry if available)
        String teamName = "";
        String position = "";
        String appearances = "";
        String goals = "";
        String assists = "";
        String yellowCards = "";
        String redCards = "";
        String rating = "";

        if (statistics != null && statistics.isArray() && statistics.size() > 0) {
            JsonNode stats = statistics.get(0);

            JsonNode team = stats.get("team");
            teamName = team != null && team.has("name") ? team.get("name").asText() : "";

            JsonNode games = stats.get("games");
            position = games != null && games.has("position") ? games.get("position").asText() : "";
            appearances = games != null && games.has("appearences") && !games.get("appearences").isNull()
                ? String.valueOf(games.get("appearences").asInt()) : "";
            rating = games != null && games.has("rating") && !games.get("rating").isNull()
                ? games.get("rating").asText() : "";

            JsonNode goalsNode = stats.get("goals");
            goals = goalsNode != null && goalsNode.has("total") && !goalsNode.get("total").isNull()
                ? String.valueOf(goalsNode.get("total").asInt()) : "0";
            assists = goalsNode != null && goalsNode.has("assists") && !goalsNode.get("assists").isNull()
                ? String.valueOf(goalsNode.get("assists").asInt()) : "0";

            JsonNode cards = stats.get("cards");
            yellowCards = cards != null && cards.has("yellow") && !cards.get("yellow").isNull()
                ? String.valueOf(cards.get("yellow").asInt()) : "0";
            redCards = cards != null && cards.has("red") && !cards.get("red").isNull()
                ? String.valueOf(cards.get("red").asInt()) : "0";
        }

        // Create row cells
        DataCell[] cells = new DataCell[]{
            new IntCell(playerId),
            new StringCell(playerName),
            new StringCell(firstname),
            new StringCell(lastname),
            new StringCell(nationality),
            new StringCell(age),
            new StringCell(teamName),
            new StringCell(position),
            new StringCell(appearances),
            new StringCell(goals),
            new StringCell(assists),
            new StringCell(yellowCards),
            new StringCell(redCards),
            new StringCell(rating)
        };

        return new DefaultRow(new RowKey("Row" + rowNum), cells);
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
            new DataColumnSpecCreator("Team", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Position", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Appearances", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goals", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Assists", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Yellow_Cards", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Red_Cards", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Rating", StringCell.TYPE).createSpec()
        );
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_queryType.saveSettingsTo(settings);
        m_playerName.saveSettingsTo(settings);
        m_playerId.saveSettingsTo(settings);
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
    }
}
