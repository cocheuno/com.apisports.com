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

package com.apisports.knime.football.nodes.teams;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.port.ApiSportsConnectionPortObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.DataRow;
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
 * NodeModel for the Football Teams node.
 * Fetches all teams for a specific league and season.
 * Reads league_id and season from flow variables (or falls back to manual settings).
 * Pushes team_id flow variable for downstream nodes if selected.
 */
public class FootballTeamsNodeModel extends NodeModel {

    static final String CFGKEY_SEASON = "season";
    static final String CFGKEY_LEAGUE_ID = "leagueId";
    static final String CFGKEY_SELECTED_TEAM_ID = "selectedTeamId";

    private final SettingsModelInteger m_season = new SettingsModelInteger(CFGKEY_SEASON, 2024);
    private final SettingsModelInteger m_leagueId = new SettingsModelInteger(CFGKEY_LEAGUE_ID, 0);
    private final SettingsModelInteger m_selectedTeamId = new SettingsModelInteger(CFGKEY_SELECTED_TEAM_ID, 0);

    protected FootballTeamsNodeModel() {
        super(new PortType[]{ApiSportsConnectionPortObject.TYPE},
              new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        ApiSportsConnectionPortObject connection = (ApiSportsConnectionPortObject) inObjects[0];
        ApiSportsHttpClient client = connection.getClient();

        // Try to read season from flow variable, fall back to settings
        int season;
        try {
            season = peekFlowVariableInt("season");
            getLogger().info("Using season " + season + " from flow variable");
        } catch (Exception e) {
            season = m_season.getIntValue();
            getLogger().info("Using season " + season + " from node settings");
        }

        // Try to read league ID from flow variable, fall back to settings
        int leagueId;
        try {
            leagueId = peekFlowVariableInt("league_id");
            getLogger().info("Using league ID " + leagueId + " from flow variable");
        } catch (Exception e) {
            leagueId = m_leagueId.getIntValue();
            getLogger().info("Using league ID " + leagueId + " from node settings");
        }

        // Validate parameters
        if (leagueId <= 0) {
            throw new InvalidSettingsException("League ID must be greater than 0");
        }
        if (season < 2000 || season > 2100) {
            throw new InvalidSettingsException("Season must be a valid year (2000-2100)");
        }

        // Call API to get teams
        exec.setMessage("Fetching teams for league " + leagueId + " season " + season + "...");
        Map<String, String> params = new HashMap<>();
        params.put("league", String.valueOf(leagueId));
        params.put("season", String.valueOf(season));

        String responseBody;
        try {
            responseBody = client.get("/teams", params);
        } catch (Exception e) {
            throw new Exception("Failed to fetch teams from API: " + e.getMessage(), e);
        }

        // Parse JSON response using Jackson
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResponse;
        JsonNode teamsArray;
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

            teamsArray = jsonResponse.get("response");
            if (teamsArray == null) {
                throw new Exception("No teams data returned from API");
            }

        } catch (Exception e) {
            throw new Exception("Failed to parse API response: " + e.getMessage() + ". Response: " +
                              (responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody), e);
        }

        // Create output table spec
        DataTableSpec outputSpec = createOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(outputSpec);

        // Process each team
        int teamCount = teamsArray.size();
        exec.setMessage("Processing " + teamCount + " teams...");
        for (int i = 0; i < teamCount; i++) {
            exec.checkCanceled();
            exec.setProgress((double) i / teamCount, "Processing team " + (i + 1) + " of " + teamCount);

            try {
                JsonNode teamData = teamsArray.get(i);
                JsonNode team = teamData.get("team");
                JsonNode venue = teamData.get("venue");

                int teamId = team.get("id").asInt();
                String teamName = team.get("name").asText();
                String teamCode = team.has("code") && !team.get("code").isNull() ?
                    team.get("code").asText() : "";
                String country = team.get("country").asText();
                int founded = team.has("founded") && !team.get("founded").isNull() ?
                    team.get("founded").asInt() : 0;
                String logo = team.get("logo").asText();

                String venueName = venue.has("name") && !venue.get("name").isNull() ?
                    venue.get("name").asText() : "";
                String venueCity = venue.has("city") && !venue.get("city").isNull() ?
                    venue.get("city").asText() : "";

                // Create row (include league ID for downstream nodes)
                container.addRowToTable(new DefaultRow("Row" + i,
                    new IntCell(leagueId),
                    new IntCell(teamId),
                    new StringCell(teamName),
                    new StringCell(teamCode),
                    new StringCell(country),
                    new IntCell(founded),
                    new StringCell(venueName),
                    new StringCell(venueCity),
                    new StringCell(logo)));

            } catch (Exception e) {
                getLogger().warn("Skipping team at index " + i + " due to error: " + e.getMessage());
            }
        }

        container.close();
        exec.setMessage("Complete - processed " + container.getTable().size() + " teams");

        // Push flow variables for downstream nodes
        pushFlowVariableInt("season", season);
        pushFlowVariableInt("league_id", leagueId);

        // If user selected a specific team ID, push it
        int selectedTeamId = m_selectedTeamId.getIntValue();
        if (selectedTeamId > 0) {
            pushFlowVariableInt("team_id", selectedTeamId);
            getLogger().info("Pushing team ID " + selectedTeamId + " to flow variables");
        }

        return new PortObject[]{container.getTable()};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{createOutputSpec()};
    }

    private DataTableSpec createOutputSpec() {
        return new DataTableSpec(
            new String[]{"League ID", "Team ID", "Name", "Code", "Country", "Founded", "Venue", "City", "Logo URL"},
            new org.knime.core.data.DataType[]{
                IntCell.TYPE, IntCell.TYPE, StringCell.TYPE, StringCell.TYPE, StringCell.TYPE,
                IntCell.TYPE, StringCell.TYPE, StringCell.TYPE, StringCell.TYPE
            });
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_season.saveSettingsTo(settings);
        m_leagueId.saveSettingsTo(settings);
        m_selectedTeamId.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_season.loadSettingsFrom(settings);
        m_leagueId.loadSettingsFrom(settings);
        m_selectedTeamId.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_season.validateSettings(settings);
        m_leagueId.validateSettings(settings);
        m_selectedTeamId.validateSettings(settings);
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
