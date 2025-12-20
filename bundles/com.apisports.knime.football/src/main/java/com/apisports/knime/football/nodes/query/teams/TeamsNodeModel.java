package com.apisports.knime.football.nodes.query.teams;

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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.util.HashMap;
import java.util.Map;

/**
 * NodeModel for Teams query node.
 * Queries team information and statistics from the Football API.
 */
public class TeamsNodeModel extends AbstractFootballQueryNodeModel {

    static final String CFGKEY_INCLUDE_STATISTICS = "includeStatistics";
    static final String CFGKEY_TEAM_NAME = "teamName";

    protected final SettingsModelBoolean m_includeStatistics =
        new SettingsModelBoolean(CFGKEY_INCLUDE_STATISTICS, false);
    protected final SettingsModelString m_teamName =
        new SettingsModelString(CFGKEY_TEAM_NAME, "");

    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        exec.setMessage("Building query parameters...");

        // Build query parameters
        Map<String, String> params = new HashMap<>();
        params.put("league", String.valueOf(m_leagueId.getIntValue()));
        params.put("season", String.valueOf(m_season.getIntValue()));

        // Optional team filter
        if (m_teamId.getIntValue() > 0) {
            params.put("id", String.valueOf(m_teamId.getIntValue()));
        } else if (!m_teamName.getStringValue().isEmpty()) {
            params.put("search", m_teamName.getStringValue());
        }

        // Make API call
        exec.setMessage("Querying teams from API...");
        JsonNode response = callApi(client, "/teams", params, mapper);

        // If statistics requested, fetch them separately
        BufferedDataTable result;
        if (m_includeStatistics.getBooleanValue()) {
            result = parseTeamsWithStatistics(response, client, mapper, exec);
        } else {
            result = parseTeamsResponse(response, exec);
        }

        getLogger().info("Retrieved " + result.size() + " team records");
        return result;
    }

    /**
     * Parse teams API response (basic info only).
     */
    private BufferedDataTable parseTeamsResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);

        if (response != null && response.isArray()) {
            int rowNum = 0;

            for (JsonNode teamItem : response) {
                try {
                    DataRow row = parseTeamRow(teamItem, null, rowNum);
                    container.addRowToTable(row);
                    rowNum++;
                } catch (Exception e) {
                    getLogger().warn("Failed to parse team row: " + e.getMessage());
                }
            }
        }

        container.close();
        return container.getTable();
    }

    /**
     * Parse teams with statistics.
     */
    private BufferedDataTable parseTeamsWithStatistics(JsonNode teamsResponse, ApiSportsHttpClient client,
                                                        ObjectMapper mapper, ExecutionContext exec) throws Exception {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);

        if (teamsResponse != null && teamsResponse.isArray()) {
            int rowNum = 0;

            for (JsonNode teamItem : teamsResponse) {
                try {
                    JsonNode team = teamItem.get("team");
                    int teamId = team != null && team.has("id") ? team.get("id").asInt() : 0;

                    // Fetch statistics for this team
                    JsonNode stats = null;
                    if (teamId > 0) {
                        Map<String, String> statsParams = new HashMap<>();
                        statsParams.put("team", String.valueOf(teamId));
                        statsParams.put("league", String.valueOf(m_leagueId.getIntValue()));
                        statsParams.put("season", String.valueOf(m_season.getIntValue()));

                        JsonNode statsResponse = callApi(client, "/teams/statistics", statsParams, mapper);
                        if (statsResponse != null) {
                            stats = statsResponse;
                        }
                    }

                    DataRow row = parseTeamRow(teamItem, stats, rowNum);
                    container.addRowToTable(row);
                    rowNum++;
                } catch (Exception e) {
                    getLogger().warn("Failed to parse team with statistics: " + e.getMessage());
                }
            }
        }

        container.close();
        return container.getTable();
    }

    /**
     * Parse a single team JSON object into a DataRow.
     */
    private DataRow parseTeamRow(JsonNode teamItem, JsonNode statistics, int rowNum) {
        JsonNode team = teamItem.get("team");
        JsonNode venue = teamItem.get("venue");

        // Extract team info
        int teamId = team != null && team.has("id") ? team.get("id").asInt() : 0;
        String teamName = team != null && team.has("name") ? team.get("name").asText() : "";
        String code = team != null && team.has("code") ? team.get("code").asText() : "";
        String country = team != null && team.has("country") ? team.get("country").asText() : "";
        String founded = team != null && team.has("founded") && !team.get("founded").isNull()
            ? String.valueOf(team.get("founded").asInt()) : "";

        // Extract venue info
        String venueName = venue != null && venue.has("name") ? venue.get("name").asText() : "";
        String venueCity = venue != null && venue.has("city") ? venue.get("city").asText() : "";
        String venueCapacity = venue != null && venue.has("capacity") && !venue.get("capacity").isNull()
            ? String.valueOf(venue.get("capacity").asInt()) : "";

        // Extract statistics if available
        String wins = "";
        String draws = "";
        String losses = "";
        String goalsFor = "";
        String goalsAgainst = "";

        if (statistics != null) {
            JsonNode fixtures = statistics.get("fixtures");
            if (fixtures != null) {
                JsonNode wins_node = fixtures.get("wins");
                JsonNode draws_node = fixtures.get("draws");
                JsonNode loses_node = fixtures.get("loses");

                wins = wins_node != null && wins_node.has("total") && !wins_node.get("total").isNull()
                    ? String.valueOf(wins_node.get("total").asInt()) : "0";
                draws = draws_node != null && draws_node.has("total") && !draws_node.get("total").isNull()
                    ? String.valueOf(draws_node.get("total").asInt()) : "0";
                losses = loses_node != null && loses_node.has("total") && !loses_node.get("total").isNull()
                    ? String.valueOf(loses_node.get("total").asInt()) : "0";
            }

            JsonNode goals = statistics.get("goals");
            if (goals != null) {
                JsonNode forNode = goals.get("for");
                JsonNode againstNode = goals.get("against");

                goalsFor = forNode != null && forNode.has("total") && !forNode.get("total").isNull()
                    ? String.valueOf(forNode.get("total").asInt()) : "0";
                goalsAgainst = againstNode != null && againstNode.has("total") && !againstNode.get("total").isNull()
                    ? String.valueOf(againstNode.get("total").asInt()) : "0";
            }
        }

        // Create row cells
        DataCell[] cells = new DataCell[]{
            new IntCell(teamId),
            new StringCell(teamName),
            new StringCell(code),
            new StringCell(country),
            new StringCell(founded),
            new StringCell(venueName),
            new StringCell(venueCity),
            new StringCell(venueCapacity),
            new StringCell(wins),
            new StringCell(draws),
            new StringCell(losses),
            new StringCell(goalsFor),
            new StringCell(goalsAgainst)
        };

        return new DefaultRow(new RowKey("Row" + rowNum), cells);
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Team_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Code", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Country", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Founded", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Venue_Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Venue_City", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Venue_Capacity", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Wins", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Draws", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Losses", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goals_For", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goals_Against", StringCell.TYPE).createSpec()
        );
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_includeStatistics.saveSettingsTo(settings);
        m_teamName.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_includeStatistics.validateSettings(settings);
        m_teamName.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_includeStatistics.loadSettingsFrom(settings);
        m_teamName.loadSettingsFrom(settings);
    }
}
