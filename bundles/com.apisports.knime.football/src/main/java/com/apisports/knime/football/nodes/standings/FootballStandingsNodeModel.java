package com.apisports.knime.football.nodes.standings;

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

public class FootballStandingsNodeModel extends NodeModel {
    static final String CFGKEY_LEAGUE_ID = "leagueId";
    static final String CFGKEY_SEASON = "season";
    
    private final SettingsModelInteger m_leagueId = new SettingsModelInteger(CFGKEY_LEAGUE_ID, 1);
    private final SettingsModelInteger m_season = new SettingsModelInteger(CFGKEY_SEASON, 2024);

    protected FootballStandingsNodeModel() {
        super(new PortType[]{ApiSportsConnectionPortObject.TYPE}, 
              new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        ApiSportsConnectionPortObject connection = (ApiSportsConnectionPortObject) inObjects[0];
        ApiSportsHttpClient client = connection.getClient();
        int leagueId = m_leagueId.getIntValue();
        int season = m_season.getIntValue();

        // Validate parameters
        if (leagueId <= 0) {
            throw new InvalidSettingsException("League ID must be greater than 0");
        }
        if (season < 2000 || season > 2100) {
            throw new InvalidSettingsException("Season must be a valid year (2000-2100)");
        }

        // Call API to get standings
        exec.setMessage("Fetching standings for league " + leagueId + " season " + season + "...");
        Map<String, String> params = new HashMap<>();
        params.put("league", String.valueOf(leagueId));
        params.put("season", String.valueOf(season));

        String responseBody;
        try {
            responseBody = client.get("/standings", params);
        } catch (Exception e) {
            throw new Exception("Failed to fetch standings from API: " + e.getMessage(), e);
        }

        // Parse JSON response using Jackson
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResponse;
        JsonNode standingsArray;
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

            // The standings endpoint returns response as array, and each element has a league object with standings array
            JsonNode responseNode = jsonResponse.get("response");
            if (responseNode == null || responseNode.size() == 0) {
                throw new Exception("No standings data returned from API");
            }

            // Get the first league's standings (usually there's only one)
            JsonNode leagueData = responseNode.get(0);
            JsonNode league = leagueData.get("league");
            standingsArray = league.get("standings");

            if (standingsArray == null || standingsArray.size() == 0) {
                throw new Exception("No standings array found in API response");
            }

            // Standings can have multiple groups (e.g., conference standings), we'll use the first one
            standingsArray = standingsArray.get(0);

        } catch (Exception e) {
            throw new Exception("Failed to parse API response: " + e.getMessage() + ". Response: " +
                              (responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody), e);
        }

        // Create output table spec
        DataTableSpec outputSpec = createOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(outputSpec);

        // Process each team's standing
        int teamCount = standingsArray.size();
        exec.setMessage("Processing " + teamCount + " teams...");
        for (int i = 0; i < teamCount; i++) {
            exec.checkCanceled();
            exec.setProgress((double) i / teamCount, "Processing team " + (i + 1) + " of " + teamCount);

            try {
                JsonNode standingData = standingsArray.get(i);
                int rank = standingData.get("rank").asInt();
                String teamName = standingData.get("team").get("name").asText();

                JsonNode all = standingData.get("all");
                int points = standingData.get("points").asInt();
                int played = all.get("played").asInt();
                int won = all.get("win").asInt();
                int drawn = all.get("draw").asInt();
                int lost = all.get("lose").asInt();
                int goalsFor = all.get("goals").get("for").asInt();
                int goalsAgainst = all.get("goals").get("against").asInt();
                int goalDiff = standingData.get("goalsDiff").asInt();

                // Create row
                container.addRowToTable(new DefaultRow("Row" + i,
                    new IntCell(rank),
                    new StringCell(teamName),
                    new IntCell(points),
                    new IntCell(played),
                    new IntCell(won),
                    new IntCell(drawn),
                    new IntCell(lost),
                    new IntCell(goalsFor),
                    new IntCell(goalsAgainst),
                    new IntCell(goalDiff)));
            } catch (Exception e) {
                getLogger().warn("Skipping team at index " + i + " due to error: " + e.getMessage());
            }
        }

        container.close();
        exec.setMessage("Complete - processed " + container.getTable().size() + " teams");
        return new PortObject[]{container.getTable()};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{createOutputSpec()};
    }

    private DataTableSpec createOutputSpec() {
        return new DataTableSpec(
            new String[]{"Rank", "Team", "Points", "Played", "Won", "Drawn", "Lost", "GF", "GA", "GD"},
            new org.knime.core.data.DataType[]{
                IntCell.TYPE, StringCell.TYPE, IntCell.TYPE, IntCell.TYPE, IntCell.TYPE, 
                IntCell.TYPE, IntCell.TYPE, IntCell.TYPE, IntCell.TYPE, IntCell.TYPE
            });
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_leagueId.saveSettingsTo(settings);
        m_season.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_leagueId.loadSettingsFrom(settings);
        m_season.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_leagueId.validateSettings(settings);
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
