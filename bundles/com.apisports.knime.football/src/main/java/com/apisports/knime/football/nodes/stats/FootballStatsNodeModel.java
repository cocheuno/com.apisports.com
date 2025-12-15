package com.apisports.knime.football.nodes.stats;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.port.ApiSportsConnectionPortObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FootballStatsNodeModel extends NodeModel {
    static final String CFGKEY_LEAGUE_ID = "leagueId";
    static final String CFGKEY_TEAM_ID = "teamId";
    static final String CFGKEY_SEASON = "season";

    private final SettingsModelInteger m_leagueId = new SettingsModelInteger(CFGKEY_LEAGUE_ID, 39);
    private final SettingsModelInteger m_teamId = new SettingsModelInteger(CFGKEY_TEAM_ID, 1);
    private final SettingsModelInteger m_season = new SettingsModelInteger(CFGKEY_SEASON, 2024);

    protected FootballStatsNodeModel() {
        super(new PortType[]{ApiSportsConnectionPortObject.TYPE}, 
              new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        ApiSportsConnectionPortObject connection = (ApiSportsConnectionPortObject) inObjects[0];
        ApiSportsHttpClient client = connection.getClient();
        int leagueId = m_leagueId.getIntValue();
        int teamId = m_teamId.getIntValue();
        int season = m_season.getIntValue();

        // Validate parameters
        if (leagueId <= 0) {
            throw new InvalidSettingsException("League ID must be greater than 0. " +
                "Tip: Use Football Leagues node to find league IDs (e.g., Premier League = 39, La Liga = 140)");
        }
        if (teamId <= 0) {
            throw new InvalidSettingsException("Team ID must be greater than 0. " +
                "Tip: Use Football Teams or Football Standings node to find team IDs (e.g., Arsenal = 42)");
        }
        if (season < 2000 || season > 2100) {
            throw new InvalidSettingsException("Season must be a valid year (2000-2100)");
        }

        // Call API to get team statistics
        exec.setMessage("Fetching statistics for team " + teamId + " in league " + leagueId + " season " + season + "...");
        Map<String, String> params = new HashMap<>();
        params.put("league", String.valueOf(leagueId));
        params.put("team", String.valueOf(teamId));
        params.put("season", String.valueOf(season));

        String responseBody;
        try {
            responseBody = client.get("/teams/statistics", params);
        } catch (Exception e) {
            throw new Exception("Failed to fetch team statistics from API: " + e.getMessage(), e);
        }

        // Parse JSON response using Jackson
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResponse;
        JsonNode statsData;
        try {
            jsonResponse = mapper.readTree(responseBody);

            // Check for API errors
            if (jsonResponse.has("errors") && jsonResponse.get("errors").size() > 0) {
                JsonNode errors = jsonResponse.get("errors");
                StringBuilder errorMsg = new StringBuilder("API returned errors: ");
                for (JsonNode error : errors) {
                    errorMsg.append(error.asText()).append("; ");
                }
                throw new Exception(errorMsg.toString());
            }

            statsData = jsonResponse.get("response");
            if (statsData == null) {
                throw new Exception("No statistics data returned from API");
            }

        } catch (Exception e) {
            throw new Exception("Failed to parse API response: " + e.getMessage() + ". Response: " +
                              (responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody), e);
        }

        // Create output table spec
        DataTableSpec outputSpec = createOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(outputSpec);

        exec.setMessage("Processing team statistics...");

        try {
            // Extract team name
            String teamName = statsData.get("team").get("name").asText();

            // Extract fixtures statistics
            JsonNode fixtures = statsData.get("fixtures");
            JsonNode fixturesPlayed = fixtures.get("played");
            int totalMatches = fixturesPlayed.get("total").asInt();

            JsonNode fixturesWins = fixtures.get("wins");
            int totalWins = fixturesWins.get("total").asInt();

            JsonNode fixturesDraws = fixtures.get("draws");
            int totalDraws = fixturesDraws.get("total").asInt();

            JsonNode fixturesLosses = fixtures.get("loses");
            int totalLosses = fixturesLosses.get("total").asInt();

            // Extract goals statistics
            JsonNode goals = statsData.get("goals");
            JsonNode goalsFor = goals.get("for");
            int totalGoalsFor = goalsFor.get("total").get("total").asInt();

            JsonNode goalsAgainst = goals.get("against");
            int totalGoalsAgainst = goalsAgainst.get("total").get("total").asInt();

            // Create single row with team statistics
            container.addRowToTable(new DefaultRow("Row0",
                new StringCell(teamName),
                new IntCell(totalMatches),
                new IntCell(totalWins),
                new IntCell(totalDraws),
                new IntCell(totalLosses),
                new IntCell(totalGoalsFor),
                new IntCell(totalGoalsAgainst)));

        } catch (Exception e) {
            throw new Exception("Failed to extract statistics from response: " + e.getMessage(), e);
        }

        container.close();
        exec.setMessage("Complete - team statistics retrieved");
        return new PortObject[]{container.getTable()};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{createOutputSpec()};
    }

    private DataTableSpec createOutputSpec() {
        return new DataTableSpec(
            new String[]{"Team", "Matches", "Wins", "Draws", "Losses", "Goals For", "Goals Against"},
            new org.knime.core.data.DataType[]{
                StringCell.TYPE, IntCell.TYPE, IntCell.TYPE, IntCell.TYPE, IntCell.TYPE, IntCell.TYPE, IntCell.TYPE
            });
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_leagueId.saveSettingsTo(settings);
        m_teamId.saveSettingsTo(settings);
        m_season.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_leagueId.loadSettingsFrom(settings);
        m_teamId.loadSettingsFrom(settings);
        m_season.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_leagueId.validateSettings(settings);
        m_teamId.validateSettings(settings);
        m_season.validateSettings(settings);
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
