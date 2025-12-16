package com.apisports.knime.football.nodes.leagues;

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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * NodeModel for the Football Leagues node.
 */
public class FootballLeaguesNodeModel extends NodeModel {

    static final String CFGKEY_COUNTRY = "country";
    static final String CFGKEY_SEASON = "season";
    static final String CFGKEY_SELECTED_LEAGUE_ID = "selectedLeagueId";

    private final SettingsModelString m_country = new SettingsModelString(CFGKEY_COUNTRY, "England");
    private final SettingsModelInteger m_season = new SettingsModelInteger(CFGKEY_SEASON, 2024);
    private final SettingsModelInteger m_selectedLeagueId = new SettingsModelInteger(CFGKEY_SELECTED_LEAGUE_ID, 0);

    protected FootballLeaguesNodeModel() {
        super(new PortType[]{ApiSportsConnectionPortObject.TYPE},
              new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        ApiSportsConnectionPortObject connection = (ApiSportsConnectionPortObject) inObjects[0];
        ApiSportsHttpClient client = connection.getClient();
        String country = m_country.getStringValue();

        // Validate country parameter
        if (country == null || country.trim().isEmpty()) {
            throw new InvalidSettingsException("Country parameter must not be empty");
        }

        // Call API to get leagues
        exec.setMessage("Fetching leagues for " + country + "...");
        Map<String, String> params = new HashMap<>();
        params.put("country", country);

        String responseBody;
        try {
            responseBody = client.get("/leagues", params);
        } catch (Exception e) {
            throw new Exception("Failed to fetch leagues from API: " + e.getMessage(), e);
        }

        // Parse JSON response using Jackson (available in KNIME)
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResponse;
        JsonNode leaguesArray;
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

            leaguesArray = jsonResponse.get("response");
        } catch (Exception e) {
            throw new Exception("Failed to parse API response: " + e.getMessage() + ". Response: " +
                              (responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody), e);
        }

        // Create output table spec
        DataTableSpec outputSpec = createOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(outputSpec);

        // Process each league
        int leagueCount = leaguesArray.size();
        exec.setMessage("Processing " + leagueCount + " leagues...");
        for (int i = 0; i < leagueCount; i++) {
            exec.checkCanceled();
            exec.setProgress((double) i / leagueCount, "Processing league " + (i + 1) + " of " + leagueCount);

            try {
                JsonNode leagueData = leaguesArray.get(i);
                JsonNode league = leagueData.get("league");
                JsonNode countryObj = leagueData.get("country");

                int leagueId = league.get("id").asInt();
                String leagueName = league.get("name").asText();
                String leagueType = league.get("type").asText();
                String countryName = countryObj.get("name").asText();

                // Find current season
                int currentYear = 0;
                if (leagueData.has("seasons")) {
                    JsonNode seasons = leagueData.get("seasons");
                    for (JsonNode season : seasons) {
                        if (season.has("current") && season.get("current").asBoolean()) {
                            currentYear = season.get("year").asInt();
                            break;
                        }
                    }
                    // If no current season found, use the last season
                    if (currentYear == 0 && seasons.size() > 0) {
                        JsonNode lastSeason = seasons.get(seasons.size() - 1);
                        currentYear = lastSeason.get("year").asInt();
                    }
                }

                // Create row
                container.addRowToTable(new DefaultRow("Row" + i,
                    new IntCell(leagueId),
                    new StringCell(leagueName),
                    new StringCell(countryName),
                    new StringCell(leagueType),
                    new IntCell(currentYear)));
            } catch (Exception e) {
                getLogger().warn("Skipping league at index " + i + " due to error: " + e.getMessage());
            }
        }

        container.close();
        exec.setMessage("Complete - processed " + container.getTable().size() + " leagues");

        // Push flow variables for downstream nodes
        int season = m_season.getIntValue();
        pushFlowVariableInt("season", season);
        pushFlowVariableString("country", country);

        // If user selected a specific league ID, push it
        int selectedLeagueId = m_selectedLeagueId.getIntValue();
        if (selectedLeagueId > 0) {
            pushFlowVariableInt("league_id", selectedLeagueId);
            getLogger().info("Pushing league ID " + selectedLeagueId + " to flow variables");
        }

        return new PortObject[]{container.getTable()};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{createOutputSpec()};
    }

    private DataTableSpec createOutputSpec() {
        return new DataTableSpec(
            new String[]{"League ID", "Name", "Country", "Type", "Season"},
            new org.knime.core.data.DataType[]{
                IntCell.TYPE, StringCell.TYPE, StringCell.TYPE, StringCell.TYPE, IntCell.TYPE
            });
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_country.saveSettingsTo(settings);
        m_season.saveSettingsTo(settings);
        m_selectedLeagueId.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_country.loadSettingsFrom(settings);
        m_season.loadSettingsFrom(settings);
        m_selectedLeagueId.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_country.validateSettings(settings);
        m_season.validateSettings(settings);
        m_selectedLeagueId.validateSettings(settings);
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
