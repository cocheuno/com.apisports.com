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

package com.apisports.knime.football.nodes.query;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.port.ApiSportsConnectionPortObject;
import com.apisports.knime.port.ReferenceDAO;
import com.apisports.knime.port.ReferenceData;
import com.apisports.knime.port.ReferenceDataPortObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import java.util.List;
import java.util.Map;

/**
 * Abstract base class for all Football API query nodes.
 * Provides common functionality for accessing reference data and making API calls.
 *
 * Input Ports:
 *   0: ApiSportsConnectionPortObject (API connection)
 *   1: ReferenceDataPortObject (reference data with leagues, seasons, teams)
 *
 * Output Ports:
 *   0: BufferedDataTable (query results)
 */
public abstract class AbstractFootballQueryNodeModel extends NodeModel {

    // Common settings shared by all query nodes
    protected static final String CFGKEY_LEAGUE_ID = "leagueId";
    protected static final String CFGKEY_SEASON = "season";
    protected static final String CFGKEY_TEAM_ID = "teamId";

    protected final SettingsModelInteger m_leagueId =
        new SettingsModelInteger(CFGKEY_LEAGUE_ID, -1);
    protected final SettingsModelInteger m_season =
        new SettingsModelInteger(CFGKEY_SEASON, -1);
    protected final SettingsModelInteger m_teamId =
        new SettingsModelInteger(CFGKEY_TEAM_ID, -1);

    // Reference data loaded from input port
    protected String m_dbPath;
    protected List<ReferenceData.League> m_leagues;
    protected List<ReferenceData.Season> m_seasons;
    protected List<ReferenceData.Team> m_teams;

    /**
     * Default constructor for nodes with standard ports.
     */
    protected AbstractFootballQueryNodeModel() {
        this(
            new PortType[]{
                ApiSportsConnectionPortObject.TYPE,
                ReferenceDataPortObject.TYPE
            },
            new PortType[]{
                BufferedDataTable.TYPE
            }
        );
    }

    /**
     * Constructor for nodes with custom port configurations.
     * Subclasses can override ports (e.g., to add optional input ports).
     */
    protected AbstractFootballQueryNodeModel(PortType[] inPortTypes, PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        // Get API client from connection port
        if (inObjects[0] == null) {
            throw new InvalidSettingsException("API connection port is not connected. Please connect an API-Sports Connector node.");
        }
        ApiSportsConnectionPortObject connectionPort = (ApiSportsConnectionPortObject) inObjects[0];
        ApiSportsHttpClient client = connectionPort.getClient();

        // Get reference data from port
        if (inObjects[1] == null) {
            throw new InvalidSettingsException("Reference data port is not connected. Please connect a Reference Data Loader node.");
        }
        ReferenceDataPortObject refDataPort = (ReferenceDataPortObject) inObjects[1];
        m_dbPath = refDataPort.getDbPath();

        // Load reference data from database
        loadReferenceData();

        // Validate settings
        validateExecutionSettings();

        // Execute endpoint-specific query (implemented by subclass)
        BufferedDataTable result = executeQuery(client, new ObjectMapper(), exec);

        return new PortObject[]{result};
    }

    /**
     * Load reference data from SQLite database.
     */
    protected void loadReferenceData() throws Exception {
        try (ReferenceDAO dao = new ReferenceDAO(m_dbPath)) {
            m_leagues = dao.getAllLeagues();
            m_seasons = dao.getAllSeasons();
            m_teams = dao.getAllTeams();

            getLogger().info("Loaded reference data: " + m_leagues.size() + " leagues, " +
                           m_seasons.size() + " seasons, " + m_teams.size() + " teams");
        }
    }

    /**
     * Validate that required settings are configured.
     * Subclasses can override to add additional validation.
     */
    protected void validateExecutionSettings() throws InvalidSettingsException {
        // Base validation - league and season typically required
        if (m_leagueId.getIntValue() <= 0) {
            throw new InvalidSettingsException("Please select a league");
        }
        if (m_season.getIntValue() <= 0) {
            throw new InvalidSettingsException("Please select a season");
        }
    }

    /**
     * Execute the endpoint-specific query.
     * Implemented by subclasses for each specific endpoint.
     *
     * @param client API client for making requests
     * @param mapper JSON mapper for parsing responses
     * @param exec Execution context for progress/cancellation
     * @return BufferedDataTable with query results
     */
    protected abstract BufferedDataTable executeQuery(ApiSportsHttpClient client,
                                                       ObjectMapper mapper,
                                                       ExecutionContext exec) throws Exception;

    /**
     * Get the output table spec.
     * Implemented by subclasses to define the schema of their result table.
     */
    protected abstract DataTableSpec getOutputSpec();

    /**
     * Helper method to make API call and parse JSON response.
     */
    protected JsonNode callApi(ApiSportsHttpClient client, String endpoint,
                               Map<String, String> params, ObjectMapper mapper) throws Exception {
        getLogger().info("Making API call: GET " + endpoint + " with params: " + params);
        String response = client.get(endpoint, params);
        getLogger().debug("Raw API response: " + response);

        JsonNode root = mapper.readTree(response);

        // Log response metadata
        JsonNode results = root.get("results");
        if (results != null) {
            getLogger().info("API returned " + results.asInt() + " results");
        }

        // Check for errors in response
        JsonNode errors = root.get("errors");
        if (errors != null && !errors.isEmpty()) {
            getLogger().error("API returned errors: " + errors.toString());
            throw new Exception("API returned errors: " + errors.toString());
        }

        JsonNode responseNode = root.get("response");
        if (responseNode == null) {
            getLogger().warn("API response node is null");
        } else if (responseNode.isEmpty()) {
            getLogger().warn("API returned empty response array. Check your query parameters.");
        }

        return responseNode;
    }

    /**
     * Get league name by ID (for logging/display).
     */
    protected String getLeagueName(int leagueId) {
        for (ReferenceData.League league : m_leagues) {
            if (league.getId() == leagueId) {
                return league.getName() + " (" + league.getCountryName() + ")";
            }
        }
        return "League ID " + leagueId;
    }

    /**
     * Get team name by ID (for logging/display).
     */
    protected String getTeamName(int teamId) {
        for (ReferenceData.Team team : m_teams) {
            if (team.getId() == teamId) {
                return team.getName();
            }
        }
        return "Team ID " + teamId;
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // Return the output spec
        return new PortObjectSpec[]{getOutputSpec()};
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_leagueId.saveSettingsTo(settings);
        m_season.saveSettingsTo(settings);
        m_teamId.saveSettingsTo(settings);
        // Subclasses should override and call super.saveSettingsTo()
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_leagueId.validateSettings(settings);
        m_season.validateSettings(settings);
        m_teamId.validateSettings(settings);
        // Subclasses should override and call super.validateSettings()
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_leagueId.loadSettingsFrom(settings);
        m_season.loadSettingsFrom(settings);
        m_teamId.loadSettingsFrom(settings);
        // Subclasses should override and call super.loadValidatedSettingsFrom()
    }

    @Override
    protected void reset() {
        m_leagues = null;
        m_seasons = null;
        m_teams = null;
        m_dbPath = null;
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
