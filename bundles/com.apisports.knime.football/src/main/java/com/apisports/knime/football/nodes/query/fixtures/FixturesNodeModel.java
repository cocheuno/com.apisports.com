package com.apisports.knime.football.nodes.query.fixtures;

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
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NodeModel for Fixtures query node.
 * Queries fixture data from the Football API.
 *
 * This node can work in two modes:
 * 1. Standalone: Uses dialog settings to query fixtures
 * 2. Details mode: Accepts optional input table with Fixture_ID column
 *    and retrieves detailed data for those specific fixtures
 */
public class FixturesNodeModel extends AbstractFootballQueryNodeModel {

    // Fixtures-specific settings
    static final String CFGKEY_QUERY_TYPE = "queryType";
    static final String CFGKEY_FROM_DATE = "fromDate";
    static final String CFGKEY_TO_DATE = "toDate";
    static final String CFGKEY_FIXTURE_ID = "fixtureId";
    static final String CFGKEY_STATUS = "status";
    static final String CFGKEY_TEAM2_ID = "team2Id";
    static final String CFGKEY_INCLUDE_EVENTS = "includeEvents";
    static final String CFGKEY_INCLUDE_LINEUPS = "includeLineups";
    static final String CFGKEY_INCLUDE_STATISTICS = "includeStatistics";
    static final String CFGKEY_INCLUDE_PLAYER_STATS = "includePlayerStats";

    // Query type options
    static final String QUERY_BY_LEAGUE = "By League/Season";
    static final String QUERY_BY_DATE = "By Date Range";
    static final String QUERY_BY_TEAM = "By Team";
    static final String QUERY_BY_ID = "By Fixture ID";
    static final String QUERY_LIVE = "Live Fixtures";
    static final String QUERY_H2H = "Head to Head";

    protected final SettingsModelString m_queryType =
        new SettingsModelString(CFGKEY_QUERY_TYPE, QUERY_BY_LEAGUE);
    protected final SettingsModelString m_fromDate =
        new SettingsModelString(CFGKEY_FROM_DATE, "");
    protected final SettingsModelString m_toDate =
        new SettingsModelString(CFGKEY_TO_DATE, "");
    protected final SettingsModelString m_fixtureId =
        new SettingsModelString(CFGKEY_FIXTURE_ID, "");
    protected final SettingsModelString m_status =
        new SettingsModelString(CFGKEY_STATUS, "");
    protected final SettingsModelInteger m_team2Id =
        new SettingsModelInteger(CFGKEY_TEAM2_ID, -1);
    protected final SettingsModelBoolean m_includeEvents =
        new SettingsModelBoolean(CFGKEY_INCLUDE_EVENTS, false);
    protected final SettingsModelBoolean m_includeLineups =
        new SettingsModelBoolean(CFGKEY_INCLUDE_LINEUPS, false);
    protected final SettingsModelBoolean m_includeStatistics =
        new SettingsModelBoolean(CFGKEY_INCLUDE_STATISTICS, false);
    protected final SettingsModelBoolean m_includePlayerStats =
        new SettingsModelBoolean(CFGKEY_INCLUDE_PLAYER_STATS, false);

    /**
     * Constructor with optional third input port for Fixture IDs.
     */
    public FixturesNodeModel() {
        super(
            new PortType[]{
                ApiSportsConnectionPortObject.TYPE,
                ReferenceDataPortObject.TYPE,
                BufferedDataTable.TYPE_OPTIONAL  // Optional fixture IDs input
            },
            new PortType[]{
                BufferedDataTable.TYPE
            }
        );
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // Check if optional Fixture IDs input port is connected
        if (inSpecs.length > 2 && inSpecs[2] != null) {
            // Optional port IS connected - will use input fixture IDs at execution
            // No need to validate dialog settings
            setWarningMessage("Using Fixture IDs from input port - dialog settings will be ignored");
            getLogger().info("CONFIGURE: Fixture IDs input detected - will use input data, ignoring dialog settings");

            // Verify the input is a DataTableSpec with Fixture_ID column
            if (inSpecs[2] instanceof DataTableSpec) {
                DataTableSpec inputSpec = (DataTableSpec) inSpecs[2];
                int fixtureIdCol = inputSpec.findColumnIndex("Fixture_ID");
                if (fixtureIdCol < 0) {
                    throw new InvalidSettingsException(
                        "Input table must contain a 'Fixture_ID' column");
                }
                getLogger().info("CONFIGURE: Found Fixture_ID column in input at index " + fixtureIdCol);
            }

            // Return output spec (dynamic based on include settings)
            return new PortObjectSpec[]{getOutputSpec()};
        } else {
            // Optional port NOT connected - validate dialog settings
            getLogger().info("CONFIGURE: No input port connected, validating dialog settings");
            setWarningMessage(null); // Clear any previous warning
            validateExecutionSettings();
            return new PortObjectSpec[]{getOutputSpec()};
        }
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

        // Check if optional fixture IDs port is connected
        BufferedDataTable fixtureIdsTable = null;
        if (inObjects.length > 2 && inObjects[2] != null) {
            fixtureIdsTable = (BufferedDataTable) inObjects[2];
            getLogger().info("Fixture IDs input port connected - using " + fixtureIdsTable.size() + " fixture IDs");
        }

        // If fixture IDs provided via input port, override query type to use those IDs
        if (fixtureIdsTable != null) {
            // Extract Fixture_ID column from input table
            List<Integer> fixtureIds = extractFixtureIds(fixtureIdsTable);
            if (fixtureIds.isEmpty()) {
                throw new InvalidSettingsException(
                    "Input table is connected but contains no Fixture_ID values");
            }

            // Convert to comma-separated string and override fixtureId setting
            String fixtureIdList = fixtureIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

            getLogger().info("Querying details for " + fixtureIds.size() + " fixtures: " + fixtureIdList);

            // Temporarily override settings to use fixture IDs
            String originalQueryType = m_queryType.getStringValue();
            String originalFixtureId = m_fixtureId.getStringValue();

            m_queryType.setStringValue(QUERY_BY_ID);
            m_fixtureId.setStringValue(fixtureIdList);

            try {
                // Execute query with overridden settings
                BufferedDataTable result = executeQuery(client, new ObjectMapper(), exec);
                return new PortObject[]{result};
            } finally {
                // Restore original settings
                m_queryType.setStringValue(originalQueryType);
                m_fixtureId.setStringValue(originalFixtureId);
            }
        } else {
            // No fixture IDs input - validate settings and use dialog configuration
            validateExecutionSettings();
            BufferedDataTable result = executeQuery(client, new ObjectMapper(), exec);
            return new PortObject[]{result};
        }
    }

    /**
     * Extract Fixture_ID column from input table.
     */
    private List<Integer> extractFixtureIds(BufferedDataTable table) throws InvalidSettingsException {
        DataTableSpec spec = table.getDataTableSpec();

        // Find Fixture_ID column
        int fixtureIdColIndex = spec.findColumnIndex("Fixture_ID");
        if (fixtureIdColIndex < 0) {
            throw new InvalidSettingsException(
                "Input table must contain a 'Fixture_ID' column");
        }

        List<Integer> fixtureIds = new ArrayList<>();
        for (DataRow row : table) {
            DataCell cell = row.getCell(fixtureIdColIndex);
            if (!cell.isMissing() && cell instanceof IntCell) {
                fixtureIds.add(((IntCell) cell).getIntValue());
            }
        }

        return fixtureIds;
    }

    @Override
    protected void validateExecutionSettings() throws InvalidSettingsException {
        String queryType = m_queryType.getStringValue();

        // Validation depends on query type
        if (QUERY_BY_LEAGUE.equals(queryType) || QUERY_BY_TEAM.equals(queryType)) {
            super.validateExecutionSettings(); // Validates league and season
        } else if (QUERY_BY_DATE.equals(queryType)) {
            if (m_fromDate.getStringValue().isEmpty()) {
                throw new InvalidSettingsException("Please specify a start date for date range query");
            }
            // Date range queries require season
            if (m_season.getIntValue() <= 0) {
                throw new InvalidSettingsException("Please select a season for date range query");
            }
            // Validate date format
            validateDateFormat(m_fromDate.getStringValue(), "From Date");
            if (!m_toDate.getStringValue().isEmpty()) {
                validateDateFormat(m_toDate.getStringValue(), "To Date");
            }
        } else if (QUERY_BY_ID.equals(queryType)) {
            if (m_fixtureId.getStringValue().isEmpty()) {
                throw new InvalidSettingsException("Please specify a fixture ID");
            }
        } else if (QUERY_H2H.equals(queryType)) {
            if (m_teamId.getIntValue() <= 0) {
                throw new InvalidSettingsException("Please select Team 1 for Head to Head query");
            }
            if (m_team2Id.getIntValue() <= 0) {
                throw new InvalidSettingsException("Please select Team 2 for Head to Head query");
            }
            if (m_season.getIntValue() <= 0) {
                throw new InvalidSettingsException("Please select a season for Head to Head query");
            }
        }
        // QUERY_LIVE has no special validation
    }

    /**
     * Validate and normalize date format to YYYY-MM-DD.
     */
    private void validateDateFormat(String date, String fieldName) throws InvalidSettingsException {
        if (date == null || date.trim().isEmpty()) {
            return;
        }

        // Check if format matches YYYY-MM-DD or YYYY/MM/DD
        if (!date.matches("\\d{4}[-/]\\d{2}[-/]\\d{2}")) {
            throw new InvalidSettingsException(
                fieldName + " must be in format YYYY-MM-DD (e.g., 2025-10-01). Got: " + date);
        }
    }

    /**
     * Normalize date format from YYYY/MM/DD to YYYY-MM-DD.
     */
    private String normalizeDateFormat(String date) {
        if (date == null || date.isEmpty()) {
            return date;
        }
        return date.replace('/', '-');
    }

    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        exec.setMessage("Building query parameters...");

        // Build query parameters based on query type
        Map<String, String> params = buildQueryParams();

        // Determine the endpoint based on query type
        String endpoint = getEndpoint();

        // Make API call
        exec.setMessage("Querying fixtures from API...");
        JsonNode response = callApi(client, endpoint, params, mapper);

        // Parse response and create output table
        exec.setMessage("Parsing results...");
        BufferedDataTable result = parseFixturesResponse(response, client, mapper, exec);

        getLogger().info("Retrieved " + result.size() + " fixtures");
        return result;
    }

    /**
     * Get the appropriate endpoint based on query type.
     */
    private String getEndpoint() {
        String queryType = m_queryType.getStringValue();
        if (QUERY_H2H.equals(queryType)) {
            return "/fixtures/headtohead";
        }
        return "/fixtures";
    }

    /**
     * Build query parameters based on selected query type.
     */
    private Map<String, String> buildQueryParams() {
        Map<String, String> params = new HashMap<>();
        String queryType = m_queryType.getStringValue();

        if (QUERY_BY_LEAGUE.equals(queryType)) {
            params.put("league", String.valueOf(m_leagueId.getIntValue()));
            params.put("season", String.valueOf(m_season.getIntValue()));

            // Optional team filter
            if (m_teamId.getIntValue() > 0) {
                params.put("team", String.valueOf(m_teamId.getIntValue()));
            }

        } else if (QUERY_BY_DATE.equals(queryType)) {
            // Normalize date format (replace / with -)
            params.put("from", normalizeDateFormat(m_fromDate.getStringValue()));
            if (!m_toDate.getStringValue().isEmpty()) {
                params.put("to", normalizeDateFormat(m_toDate.getStringValue()));
            }

            // Season is required for date queries
            params.put("season", String.valueOf(m_season.getIntValue()));

            // Optional league filter
            if (m_leagueId.getIntValue() > 0) {
                params.put("league", String.valueOf(m_leagueId.getIntValue()));
            }

            // Optional team filter
            if (m_teamId.getIntValue() > 0) {
                params.put("team", String.valueOf(m_teamId.getIntValue()));
            }

        } else if (QUERY_BY_TEAM.equals(queryType)) {
            params.put("team", String.valueOf(m_teamId.getIntValue()));
            params.put("season", String.valueOf(m_season.getIntValue()));

        } else if (QUERY_BY_ID.equals(queryType)) {
            params.put("id", m_fixtureId.getStringValue());

        } else if (QUERY_LIVE.equals(queryType)) {
            params.put("live", "all");

        } else if (QUERY_H2H.equals(queryType)) {
            // H2H requires two team IDs in format "teamId1-teamId2"
            params.put("h2h", String.valueOf(m_teamId.getIntValue()) + "-" +
                              String.valueOf(m_team2Id.getIntValue()));
            // Add season to filter H2H results to specific season
            params.put("season", String.valueOf(m_season.getIntValue()));
        }

        // Optional status filter
        if (!m_status.getStringValue().isEmpty()) {
            params.put("status", m_status.getStringValue());
        }

        return params;
    }

    /**
     * Parse fixtures API response and create output table.
     */
    private BufferedDataTable parseFixturesResponse(JsonNode response, ApiSportsHttpClient client,
                                                     ObjectMapper mapper, ExecutionContext exec) throws Exception {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);

        if (response != null && response.isArray()) {
            int rowNum = 0;
            int totalFixtures = response.size();

            for (JsonNode fixtureItem : response) {
                try {
                    // Extract fixture ID and team IDs for additional API calls
                    JsonNode fixture = fixtureItem.get("fixture");
                    JsonNode teams = fixtureItem.get("teams");
                    int fixtureId = fixture != null && fixture.has("id") ? fixture.get("id").asInt() : 0;

                    // Extract team IDs
                    int homeTeamId = 0, awayTeamId = 0;
                    if (teams != null) {
                        JsonNode home = teams.get("home");
                        JsonNode away = teams.get("away");
                        if (home != null && home.has("id")) homeTeamId = home.get("id").asInt();
                        if (away != null && away.has("id")) awayTeamId = away.get("id").asInt();
                    }

                    // Fetch additional data if checkboxes are enabled
                    JsonNode events = null;
                    JsonNode statistics = null;
                    JsonNode lineups = null;
                    JsonNode players = null;

                    if (fixtureId > 0) {
                        if (m_includeEvents.getBooleanValue()) {
                            exec.setMessage(String.format("Fetching events for fixture %d (%d/%d)...",
                                                         fixtureId, rowNum + 1, totalFixtures));
                            events = callApi(client, "/fixtures/events",
                                           Map.of("fixture", String.valueOf(fixtureId)), mapper);
                        }

                        if (m_includeStatistics.getBooleanValue() && homeTeamId > 0 && awayTeamId > 0) {
                            exec.setMessage(String.format("Fetching statistics for fixture %d (%d/%d)...",
                                                         fixtureId, rowNum + 1, totalFixtures));

                            // Make TWO API calls - one for each team
                            JsonNode homeStats = callApi(client, "/fixtures/statistics",
                                               Map.of("fixture", String.valueOf(fixtureId),
                                                     "team", String.valueOf(homeTeamId)), mapper);
                            JsonNode awayStats = callApi(client, "/fixtures/statistics",
                                               Map.of("fixture", String.valueOf(fixtureId),
                                                     "team", String.valueOf(awayTeamId)), mapper);

                            // Combine into single array: [homeStats, awayStats]
                            statistics = mapper.createArrayNode();
                            if (homeStats != null && homeStats.isArray() && homeStats.size() > 0) {
                                ((com.fasterxml.jackson.databind.node.ArrayNode)statistics).add(homeStats.get(0));
                            }
                            if (awayStats != null && awayStats.isArray() && awayStats.size() > 0) {
                                ((com.fasterxml.jackson.databind.node.ArrayNode)statistics).add(awayStats.get(0));
                            }
                        }

                        if (m_includeLineups.getBooleanValue()) {
                            exec.setMessage(String.format("Fetching lineups for fixture %d (%d/%d)...",
                                                         fixtureId, rowNum + 1, totalFixtures));
                            lineups = callApi(client, "/fixtures/lineups",
                                            Map.of("fixture", String.valueOf(fixtureId)), mapper);
                        }

                        if (m_includePlayerStats.getBooleanValue() && homeTeamId > 0 && awayTeamId > 0) {
                            exec.setMessage(String.format("Fetching player stats for fixture %d (%d/%d)...",
                                                         fixtureId, rowNum + 1, totalFixtures));

                            // Make TWO API calls - one for each team
                            JsonNode homePlayers = callApi(client, "/fixtures/players",
                                            Map.of("fixture", String.valueOf(fixtureId),
                                                  "team", String.valueOf(homeTeamId)), mapper);
                            JsonNode awayPlayers = callApi(client, "/fixtures/players",
                                            Map.of("fixture", String.valueOf(fixtureId),
                                                  "team", String.valueOf(awayTeamId)), mapper);

                            // Combine into single array: [homePlayers, awayPlayers]
                            players = mapper.createArrayNode();
                            if (homePlayers != null && homePlayers.isArray() && homePlayers.size() > 0) {
                                ((com.fasterxml.jackson.databind.node.ArrayNode)players).add(homePlayers.get(0));
                            }
                            if (awayPlayers != null && awayPlayers.isArray() && awayPlayers.size() > 0) {
                                ((com.fasterxml.jackson.databind.node.ArrayNode)players).add(awayPlayers.get(0));
                            }
                        }
                    }

                    DataRow row = parseFixtureRow(fixtureItem, events, statistics, lineups, players, rowNum);
                    container.addRowToTable(row);
                    rowNum++;
                } catch (Exception e) {
                    getLogger().warn("Failed to parse fixture row " + rowNum + ": " + e.getMessage());
                }
            }
        }

        container.close();
        return container.getTable();
    }

    /**
     * Parse a single fixture JSON object into a DataRow.
     */
    private DataRow parseFixtureRow(JsonNode fixtureItem, JsonNode events, JsonNode statistics,
                                    JsonNode lineups, JsonNode players, int rowNum) {
        JsonNode fixture = fixtureItem.get("fixture");
        JsonNode league = fixtureItem.get("league");
        JsonNode teams = fixtureItem.get("teams");
        JsonNode goals = fixtureItem.get("goals");
        JsonNode score = fixtureItem.get("score");

        // Calculate total column count based on enabled options
        int totalColumns = 39;  // Base columns (14 fixture + 7 league + 8 teams + 2 goals + 8 score)
        if (m_includeEvents.getBooleanValue()) totalColumns += 18;
        if (m_includeStatistics.getBooleanValue()) totalColumns += 32;
        if (m_includeLineups.getBooleanValue()) totalColumns += 20;
        if (m_includePlayerStats.getBooleanValue()) totalColumns += 28;

        // Create cells array
        DataCell[] cells = new DataCell[totalColumns];
        int colIdx = 0;

        // Fixture Information (14 columns)
        cells[colIdx++] = getIntCell(fixture, "id");
        cells[colIdx++] = getStringCell(fixture, "referee");
        cells[colIdx++] = getStringCell(fixture, "timezone");
        cells[colIdx++] = getStringCell(fixture, "date");
        cells[colIdx++] = getLongCell(fixture, "timestamp");

        JsonNode periods = fixture != null ? fixture.get("periods") : null;
        cells[colIdx++] = getLongCell(periods, "first");
        cells[colIdx++] = getLongCell(periods, "second");

        JsonNode venue = fixture != null ? fixture.get("venue") : null;
        cells[colIdx++] = getIntCell(venue, "id");
        cells[colIdx++] = getStringCell(venue, "name");
        cells[colIdx++] = getStringCell(venue, "city");

        JsonNode status = fixture != null ? fixture.get("status") : null;
        cells[colIdx++] = getStringCell(status, "long");
        cells[colIdx++] = getStringCell(status, "short");
        cells[colIdx++] = getIntCell(status, "elapsed");
        cells[colIdx++] = getIntCell(status, "extra");

        // League Information (7 columns)
        cells[colIdx++] = getIntCell(league, "id");
        cells[colIdx++] = getStringCell(league, "name");
        cells[colIdx++] = getStringCell(league, "country");
        cells[colIdx++] = getStringCell(league, "logo");
        cells[colIdx++] = getStringCell(league, "flag");
        cells[colIdx++] = getIntCell(league, "season");
        cells[colIdx++] = getStringCell(league, "round");

        // Teams Information (8 columns)
        JsonNode homeTeam = teams != null ? teams.get("home") : null;
        JsonNode awayTeam = teams != null ? teams.get("away") : null;

        cells[colIdx++] = getIntCell(homeTeam, "id");
        cells[colIdx++] = getStringCell(homeTeam, "name");
        cells[colIdx++] = getStringCell(homeTeam, "logo");
        cells[colIdx++] = getBooleanCell(homeTeam, "winner");

        cells[colIdx++] = getIntCell(awayTeam, "id");
        cells[colIdx++] = getStringCell(awayTeam, "name");
        cells[colIdx++] = getStringCell(awayTeam, "logo");
        cells[colIdx++] = getBooleanCell(awayTeam, "winner");

        // Goals (2 columns)
        cells[colIdx++] = getIntCell(goals, "home");
        cells[colIdx++] = getIntCell(goals, "away");

        // Score Breakdown (8 columns)
        JsonNode halftime = score != null ? score.get("halftime") : null;
        JsonNode fulltime = score != null ? score.get("fulltime") : null;
        JsonNode extratime = score != null ? score.get("extratime") : null;
        JsonNode penalty = score != null ? score.get("penalty") : null;

        cells[colIdx++] = getIntCell(halftime, "home");
        cells[colIdx++] = getIntCell(halftime, "away");
        cells[colIdx++] = getIntCell(fulltime, "home");
        cells[colIdx++] = getIntCell(fulltime, "away");
        cells[colIdx++] = getIntCell(extratime, "home");
        cells[colIdx++] = getIntCell(extratime, "away");
        cells[colIdx++] = getIntCell(penalty, "home");
        cells[colIdx++] = getIntCell(penalty, "away");

        // Events (18 columns - if enabled)
        if (m_includeEvents.getBooleanValue()) {
            EventsData eventsData = parseEvents(events);
            cells[colIdx++] = new StringCell(eventsData.goalsHomeScorers);
            cells[colIdx++] = new StringCell(eventsData.goalsHomeAssists);
            cells[colIdx++] = new StringCell(eventsData.goalsHomeTimes);
            cells[colIdx++] = new StringCell(eventsData.goalsAwayScorers);
            cells[colIdx++] = new StringCell(eventsData.goalsAwayAssists);
            cells[colIdx++] = new StringCell(eventsData.goalsAwayTimes);
            cells[colIdx++] = new IntCell(eventsData.yellowCardsHome);
            cells[colIdx++] = new StringCell(eventsData.yellowCardsHomePlayers);
            cells[colIdx++] = new IntCell(eventsData.yellowCardsAway);
            cells[colIdx++] = new StringCell(eventsData.yellowCardsAwayPlayers);
            cells[colIdx++] = new IntCell(eventsData.redCardsHome);
            cells[colIdx++] = new StringCell(eventsData.redCardsHomePlayers);
            cells[colIdx++] = new IntCell(eventsData.redCardsAway);
            cells[colIdx++] = new StringCell(eventsData.redCardsAwayPlayers);
            cells[colIdx++] = new IntCell(eventsData.substitutionsHomeCount);
            cells[colIdx++] = new StringCell(eventsData.substitutionsHomeDetails);
            cells[colIdx++] = new IntCell(eventsData.substitutionsAwayCount);
            cells[colIdx++] = new StringCell(eventsData.substitutionsAwayDetails);
        }

        // Statistics (32 columns - if enabled)
        if (m_includeStatistics.getBooleanValue()) {
            StatisticsData statsData = parseStatistics(statistics);
            // Home team stats (16 columns)
            cells[colIdx++] = new IntCell(statsData.homeShotsOnGoal);
            cells[colIdx++] = new IntCell(statsData.homeShotsOffGoal);
            cells[colIdx++] = new IntCell(statsData.homeTotalShots);
            cells[colIdx++] = new IntCell(statsData.homeBlockedShots);
            cells[colIdx++] = new IntCell(statsData.homeShotsInsideBox);
            cells[colIdx++] = new IntCell(statsData.homeShotsOutsideBox);
            cells[colIdx++] = new IntCell(statsData.homeFouls);
            cells[colIdx++] = new IntCell(statsData.homeCornerKicks);
            cells[colIdx++] = new IntCell(statsData.homeOffsides);
            cells[colIdx++] = new StringCell(statsData.homeBallPossession);
            cells[colIdx++] = new IntCell(statsData.homeYellowCards);
            cells[colIdx++] = new IntCell(statsData.homeRedCards);
            cells[colIdx++] = new IntCell(statsData.homeGoalkeeperSaves);
            cells[colIdx++] = new IntCell(statsData.homeTotalPasses);
            cells[colIdx++] = new IntCell(statsData.homePassesAccurate);
            cells[colIdx++] = new StringCell(statsData.homePassesPercentage);
            // Away team stats (16 columns)
            cells[colIdx++] = new IntCell(statsData.awayShotsOnGoal);
            cells[colIdx++] = new IntCell(statsData.awayShotsOffGoal);
            cells[colIdx++] = new IntCell(statsData.awayTotalShots);
            cells[colIdx++] = new IntCell(statsData.awayBlockedShots);
            cells[colIdx++] = new IntCell(statsData.awayShotsInsideBox);
            cells[colIdx++] = new IntCell(statsData.awayShotsOutsideBox);
            cells[colIdx++] = new IntCell(statsData.awayFouls);
            cells[colIdx++] = new IntCell(statsData.awayCornerKicks);
            cells[colIdx++] = new IntCell(statsData.awayOffsides);
            cells[colIdx++] = new StringCell(statsData.awayBallPossession);
            cells[colIdx++] = new IntCell(statsData.awayYellowCards);
            cells[colIdx++] = new IntCell(statsData.awayRedCards);
            cells[colIdx++] = new IntCell(statsData.awayGoalkeeperSaves);
            cells[colIdx++] = new IntCell(statsData.awayTotalPasses);
            cells[colIdx++] = new IntCell(statsData.awayPassesAccurate);
            cells[colIdx++] = new StringCell(statsData.awayPassesPercentage);
        }

        // Lineups (20 columns - if enabled)
        if (m_includeLineups.getBooleanValue()) {
            LineupsData lineupsData = parseLineups(lineups);
            // Home team lineup (10 columns)
            cells[colIdx++] = new StringCell(lineupsData.homeFormation);
            cells[colIdx++] = new StringCell(lineupsData.homeStartingXIPlayers);
            cells[colIdx++] = new StringCell(lineupsData.homeStartingXINumbers);
            cells[colIdx++] = new StringCell(lineupsData.homeStartingXIPositions);
            cells[colIdx++] = new StringCell(lineupsData.homeSubstitutesPlayers);
            cells[colIdx++] = new StringCell(lineupsData.homeSubstitutesNumbers);
            cells[colIdx++] = new StringCell(lineupsData.homeSubstitutesPositions);
            cells[colIdx++] = new IntCell(lineupsData.homeCoachId);
            cells[colIdx++] = new StringCell(lineupsData.homeCoachName);
            cells[colIdx++] = new StringCell(lineupsData.homeCoachPhoto);
            // Away team lineup (10 columns)
            cells[colIdx++] = new StringCell(lineupsData.awayFormation);
            cells[colIdx++] = new StringCell(lineupsData.awayStartingXIPlayers);
            cells[colIdx++] = new StringCell(lineupsData.awayStartingXINumbers);
            cells[colIdx++] = new StringCell(lineupsData.awayStartingXIPositions);
            cells[colIdx++] = new StringCell(lineupsData.awaySubstitutesPlayers);
            cells[colIdx++] = new StringCell(lineupsData.awaySubstitutesNumbers);
            cells[colIdx++] = new StringCell(lineupsData.awaySubstitutesPositions);
            cells[colIdx++] = new IntCell(lineupsData.awayCoachId);
            cells[colIdx++] = new StringCell(lineupsData.awayCoachName);
            cells[colIdx++] = new StringCell(lineupsData.awayCoachPhoto);
        }

        // Player Stats (28 columns - if enabled)
        if (m_includePlayerStats.getBooleanValue()) {
            PlayerStatsData playerStatsData = parsePlayerStats(players);
            // Top performers (12 columns)
            cells[colIdx++] = new StringCell(playerStatsData.topRatedPlayerHome);
            cells[colIdx++] = new StringCell(playerStatsData.topRatedPlayerHomeRating);
            cells[colIdx++] = new StringCell(playerStatsData.topRatedPlayerAway);
            cells[colIdx++] = new StringCell(playerStatsData.topRatedPlayerAwayRating);
            cells[colIdx++] = new StringCell(playerStatsData.topScorerHome);
            cells[colIdx++] = new IntCell(playerStatsData.topScorerHomeGoals);
            cells[colIdx++] = new StringCell(playerStatsData.topScorerAway);
            cells[colIdx++] = new IntCell(playerStatsData.topScorerAwayGoals);
            cells[colIdx++] = new StringCell(playerStatsData.topAssistHome);
            cells[colIdx++] = new IntCell(playerStatsData.topAssistHomeAssists);
            cells[colIdx++] = new StringCell(playerStatsData.topAssistAway);
            cells[colIdx++] = new IntCell(playerStatsData.topAssistAwayAssists);
            // Team aggregates (16 columns)
            cells[colIdx++] = new IntCell(playerStatsData.totalShotsHome);
            cells[colIdx++] = new IntCell(playerStatsData.totalShotsAway);
            cells[colIdx++] = new IntCell(playerStatsData.totalPassesHome);
            cells[colIdx++] = new IntCell(playerStatsData.totalPassesAway);
            cells[colIdx++] = new IntCell(playerStatsData.totalTacklesHome);
            cells[colIdx++] = new IntCell(playerStatsData.totalTacklesAway);
            cells[colIdx++] = new IntCell(playerStatsData.totalDribblesHome);
            cells[colIdx++] = new IntCell(playerStatsData.totalDribblesAway);
            cells[colIdx++] = new IntCell(playerStatsData.totalFoulsCommittedHome);
            cells[colIdx++] = new IntCell(playerStatsData.totalFoulsCommittedAway);
            cells[colIdx++] = new IntCell(playerStatsData.totalFoulsDrawnHome);
            cells[colIdx++] = new IntCell(playerStatsData.totalFoulsDrawnAway);
            cells[colIdx++] = new StringCell(playerStatsData.averageRatingHome);
            cells[colIdx++] = new StringCell(playerStatsData.averageRatingAway);
            cells[colIdx++] = new StringCell(playerStatsData.captainHome);
            cells[colIdx++] = new StringCell(playerStatsData.captainAway);
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

    private DataCell getLongCell(JsonNode node, String field) {
        if (node != null && node.has(field) && !node.get(field).isNull()) {
            return new LongCell(node.get(field).asLong());
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
            return BooleanCell.get(node.get(field).asBoolean());
        }
        return DataType.getMissingCell();
    }

    // Data classes for structured parsing
    private static class EventsData {
        String goalsHomeScorers = "";
        String goalsHomeAssists = "";
        String goalsHomeTimes = "";
        String goalsAwayScorers = "";
        String goalsAwayAssists = "";
        String goalsAwayTimes = "";
        int yellowCardsHome = 0;
        String yellowCardsHomePlayers = "";
        int yellowCardsAway = 0;
        String yellowCardsAwayPlayers = "";
        int redCardsHome = 0;
        String redCardsHomePlayers = "";
        int redCardsAway = 0;
        String redCardsAwayPlayers = "";
        int substitutionsHomeCount = 0;
        String substitutionsHomeDetails = "";
        int substitutionsAwayCount = 0;
        String substitutionsAwayDetails = "";
    }

    private static class StatisticsData {
        int homeShotsOnGoal = 0, homeShotsOffGoal = 0, homeTotalShots = 0, homeBlockedShots = 0;
        int homeShotsInsideBox = 0, homeShotsOutsideBox = 0, homeFouls = 0, homeCornerKicks = 0;
        int homeOffsides = 0, homeYellowCards = 0, homeRedCards = 0, homeGoalkeeperSaves = 0;
        int homeTotalPasses = 0, homePassesAccurate = 0;
        String homeBallPossession = "", homePassesPercentage = "";
        int awayShotsOnGoal = 0, awayShotsOffGoal = 0, awayTotalShots = 0, awayBlockedShots = 0;
        int awayShotsInsideBox = 0, awayShotsOutsideBox = 0, awayFouls = 0, awayCornerKicks = 0;
        int awayOffsides = 0, awayYellowCards = 0, awayRedCards = 0, awayGoalkeeperSaves = 0;
        int awayTotalPasses = 0, awayPassesAccurate = 0;
        String awayBallPossession = "", awayPassesPercentage = "";
    }

    private static class LineupsData {
        String homeFormation = "", homeStartingXIPlayers = "", homeStartingXINumbers = "";
        String homeStartingXIPositions = "", homeSubstitutesPlayers = "", homeSubstitutesNumbers = "";
        String homeSubstitutesPositions = "", homeCoachName = "", homeCoachPhoto = "";
        int homeCoachId = 0;
        String awayFormation = "", awayStartingXIPlayers = "", awayStartingXINumbers = "";
        String awayStartingXIPositions = "", awaySubstitutesPlayers = "", awaySubstitutesNumbers = "";
        String awaySubstitutesPositions = "", awayCoachName = "", awayCoachPhoto = "";
        int awayCoachId = 0;
    }

    private static class PlayerStatsData {
        // Top performers
        String topRatedPlayerHome = "", topRatedPlayerHomeRating = "";
        String topRatedPlayerAway = "", topRatedPlayerAwayRating = "";
        String topScorerHome = "", topScorerAway = "";
        int topScorerHomeGoals = 0, topScorerAwayGoals = 0;
        String topAssistHome = "", topAssistAway = "";
        int topAssistHomeAssists = 0, topAssistAwayAssists = 0;
        // Team aggregates
        int totalShotsHome = 0, totalShotsAway = 0;
        int totalPassesHome = 0, totalPassesAway = 0;
        int totalTacklesHome = 0, totalTacklesAway = 0;
        int totalDribblesHome = 0, totalDribblesAway = 0;
        int totalFoulsCommittedHome = 0, totalFoulsCommittedAway = 0;
        int totalFoulsDrawnHome = 0, totalFoulsDrawnAway = 0;
        String averageRatingHome = "", averageRatingAway = "";
        String captainHome = "", captainAway = "";
    }

    /**
     * Parse events data from the API response.
     */
    private EventsData parseEvents(JsonNode events) {
        EventsData data = new EventsData();
        if (events == null || !events.isArray()) return data;

        StringBuilder homeGoals = new StringBuilder(), homeAssists = new StringBuilder(), homeTimes = new StringBuilder();
        StringBuilder awayGoals = new StringBuilder(), awayAssists = new StringBuilder(), awayTimes = new StringBuilder();
        StringBuilder homeYellow = new StringBuilder(), awayYellow = new StringBuilder();
        StringBuilder homeRed = new StringBuilder(), awayRed = new StringBuilder();
        StringBuilder homeSubs = new StringBuilder(), awaySubs = new StringBuilder();

        // Track team IDs to determine home vs away
        Integer homeTeamId = null, awayTeamId = null;

        for (JsonNode event : events) {
            String type = event.has("type") ? event.get("type").asText() : "";
            String detail = event.has("detail") ? event.get("detail").asText() : "";
            JsonNode team = event.get("team");
            int teamId = team != null && team.has("id") ? team.get("id").asInt() : 0;

            // Determine home/away based on order (first team is typically home)
            if (homeTeamId == null) homeTeamId = teamId;
            else if (awayTeamId == null && teamId != homeTeamId) awayTeamId = teamId;

            boolean isHome = teamId == homeTeamId;

            if ("Goal".equals(type)) {
                JsonNode player = event.get("player");
                JsonNode assist = event.get("assist");
                JsonNode time = event.get("time");
                String playerName = player != null && player.has("name") ? player.get("name").asText() : "Unknown";
                String assistName = assist != null && assist.has("name") ? assist.get("name").asText() : "";
                int minute = time != null && time.has("elapsed") ? time.get("elapsed").asInt() : 0;

                if (isHome) {
                    if (homeGoals.length() > 0) homeGoals.append(", ");
                    homeGoals.append(playerName).append(" (").append(minute).append("')");
                    if (homeAssists.length() > 0) homeAssists.append(", ");
                    homeAssists.append(assistName.isEmpty() ? "-" : assistName);
                    if (homeTimes.length() > 0) homeTimes.append(", ");
                    homeTimes.append(minute);
                } else {
                    if (awayGoals.length() > 0) awayGoals.append(", ");
                    awayGoals.append(playerName).append(" (").append(minute).append("')");
                    if (awayAssists.length() > 0) awayAssists.append(", ");
                    awayAssists.append(assistName.isEmpty() ? "-" : assistName);
                    if (awayTimes.length() > 0) awayTimes.append(", ");
                    awayTimes.append(minute);
                }
            } else if ("Card".equals(type)) {
                JsonNode player = event.get("player");
                String playerName = player != null && player.has("name") ? player.get("name").asText() : "Unknown";

                if ("Yellow Card".equals(detail)) {
                    if (isHome) {
                        data.yellowCardsHome++;
                        if (homeYellow.length() > 0) homeYellow.append(", ");
                        homeYellow.append(playerName);
                    } else {
                        data.yellowCardsAway++;
                        if (awayYellow.length() > 0) awayYellow.append(", ");
                        awayYellow.append(playerName);
                    }
                } else if ("Red Card".equals(detail)) {
                    if (isHome) {
                        data.redCardsHome++;
                        if (homeRed.length() > 0) homeRed.append(", ");
                        homeRed.append(playerName);
                    } else {
                        data.redCardsAway++;
                        if (awayRed.length() > 0) awayRed.append(", ");
                        awayRed.append(playerName);
                    }
                }
            } else if ("subst".equals(type)) {
                JsonNode player = event.get("player");
                JsonNode assist = event.get("assist");  // Player coming in
                JsonNode time = event.get("time");
                String playerOut = player != null && player.has("name") ? player.get("name").asText() : "?";
                String playerIn = assist != null && assist.has("name") ? assist.get("name").asText() : "?";
                int minute = time != null && time.has("elapsed") ? time.get("elapsed").asInt() : 0;

                String subDetail = playerOut + "→" + playerIn + " (" + minute + "')";
                if (isHome) {
                    data.substitutionsHomeCount++;
                    if (homeSubs.length() > 0) homeSubs.append(", ");
                    homeSubs.append(subDetail);
                } else {
                    data.substitutionsAwayCount++;
                    if (awaySubs.length() > 0) awaySubs.append(", ");
                    awaySubs.append(subDetail);
                }
            }
        }

        data.goalsHomeScorers = homeGoals.toString();
        data.goalsHomeAssists = homeAssists.toString();
        data.goalsHomeTimes = homeTimes.toString();
        data.goalsAwayScorers = awayGoals.toString();
        data.goalsAwayAssists = awayAssists.toString();
        data.goalsAwayTimes = awayTimes.toString();
        data.yellowCardsHomePlayers = homeYellow.toString();
        data.yellowCardsAwayPlayers = awayYellow.toString();
        data.redCardsHomePlayers = homeRed.toString();
        data.redCardsAwayPlayers = awayRed.toString();
        data.substitutionsHomeDetails = homeSubs.toString();
        data.substitutionsAwayDetails = awaySubs.toString();

        return data;
    }

    /**
     * Parse statistics data from the API response.
     */
    private StatisticsData parseStatistics(JsonNode statistics) {
        StatisticsData data = new StatisticsData();
        if (statistics == null || !statistics.isArray()) return data;

        // Statistics response is an array with 2 elements (home and away teams)
        for (int i = 0; i < statistics.size(); i++) {
            JsonNode teamStats = statistics.get(i);
            JsonNode statsArray = teamStats.get("statistics");
            boolean isHome = i == 0;  // First team is home

            if (statsArray != null && statsArray.isArray()) {
                for (JsonNode stat : statsArray) {
                    String type = stat.has("type") ? stat.get("type").asText() : "";
                    JsonNode valueNode = stat.get("value");

                    if (valueNode == null || valueNode.isNull()) continue;

                    if (isHome) {
                        parseStatValue(type, valueNode, data, true);
                    } else {
                        parseStatValue(type, valueNode, data, false);
                    }
                }
            }
        }

        return data;
    }

    private void parseStatValue(String type, JsonNode valueNode, StatisticsData data, boolean isHome) {
        String value = valueNode.isTextual() ? valueNode.asText() : String.valueOf(valueNode.asInt());
        int intValue = 0;
        try {
            intValue = Integer.parseInt(value.replace("%", "").trim());
        } catch (NumberFormatException e) {
            // Keep as 0
        }

        if (isHome) {
            switch (type) {
                case "Shots on Goal": data.homeShotsOnGoal = intValue; break;
                case "Shots off Goal": data.homeShotsOffGoal = intValue; break;
                case "Total Shots": data.homeTotalShots = intValue; break;
                case "Blocked Shots": data.homeBlockedShots = intValue; break;
                case "Shots insidebox": data.homeShotsInsideBox = intValue; break;
                case "Shots outsidebox": data.homeShotsOutsideBox = intValue; break;
                case "Fouls": data.homeFouls = intValue; break;
                case "Corner Kicks": data.homeCornerKicks = intValue; break;
                case "Offsides": data.homeOffsides = intValue; break;
                case "Ball Possession": data.homeBallPossession = value; break;
                case "Yellow Cards": data.homeYellowCards = intValue; break;
                case "Red Cards": data.homeRedCards = intValue; break;
                case "Goalkeeper Saves": data.homeGoalkeeperSaves = intValue; break;
                case "Total passes": data.homeTotalPasses = intValue; break;
                case "Passes accurate": data.homePassesAccurate = intValue; break;
                case "Passes %": data.homePassesPercentage = value; break;
            }
        } else {
            switch (type) {
                case "Shots on Goal": data.awayShotsOnGoal = intValue; break;
                case "Shots off Goal": data.awayShotsOffGoal = intValue; break;
                case "Total Shots": data.awayTotalShots = intValue; break;
                case "Blocked Shots": data.awayBlockedShots = intValue; break;
                case "Shots insidebox": data.awayShotsInsideBox = intValue; break;
                case "Shots outsidebox": data.awayShotsOutsideBox = intValue; break;
                case "Fouls": data.awayFouls = intValue; break;
                case "Corner Kicks": data.awayCornerKicks = intValue; break;
                case "Offsides": data.awayOffsides = intValue; break;
                case "Ball Possession": data.awayBallPossession = value; break;
                case "Yellow Cards": data.awayYellowCards = intValue; break;
                case "Red Cards": data.awayRedCards = intValue; break;
                case "Goalkeeper Saves": data.awayGoalkeeperSaves = intValue; break;
                case "Total passes": data.awayTotalPasses = intValue; break;
                case "Passes accurate": data.awayPassesAccurate = intValue; break;
                case "Passes %": data.awayPassesPercentage = value; break;
            }
        }
    }

    /**
     * Parse lineups data from the API response.
     */
    private LineupsData parseLineups(JsonNode lineups) {
        LineupsData data = new LineupsData();
        if (lineups == null || !lineups.isArray()) return data;

        for (int i = 0; i < lineups.size(); i++) {
            JsonNode teamLineup = lineups.get(i);
            boolean isHome = i == 0;  // First team is home

            String formation = teamLineup.has("formation") ? teamLineup.get("formation").asText() : "";

            StringBuilder startPlayers = new StringBuilder(), startNumbers = new StringBuilder(), startPos = new StringBuilder();
            StringBuilder subPlayers = new StringBuilder(), subNumbers = new StringBuilder(), subPos = new StringBuilder();

            JsonNode startXI = teamLineup.get("startXI");
            if (startXI != null && startXI.isArray()) {
                for (JsonNode playerNode : startXI) {
                    JsonNode player = playerNode.get("player");
                    if (player != null) {
                        if (startPlayers.length() > 0) startPlayers.append(", ");
                        startPlayers.append(player.has("name") ? player.get("name").asText() : "");
                        if (startNumbers.length() > 0) startNumbers.append(", ");
                        startNumbers.append(player.has("number") ? player.get("number").asInt() : 0);
                        if (startPos.length() > 0) startPos.append(", ");
                        startPos.append(player.has("pos") ? player.get("pos").asText() : "");
                    }
                }
            }

            JsonNode substitutes = teamLineup.get("substitutes");
            if (substitutes != null && substitutes.isArray()) {
                for (JsonNode playerNode : substitutes) {
                    JsonNode player = playerNode.get("player");
                    if (player != null) {
                        if (subPlayers.length() > 0) subPlayers.append(", ");
                        subPlayers.append(player.has("name") ? player.get("name").asText() : "");
                        if (subNumbers.length() > 0) subNumbers.append(", ");
                        subNumbers.append(player.has("number") ? player.get("number").asInt() : 0);
                        if (subPos.length() > 0) subPos.append(", ");
                        subPos.append(player.has("pos") ? player.get("pos").asText() : "");
                    }
                }
            }

            JsonNode coach = teamLineup.get("coach");
            int coachId = 0;
            String coachName = "", coachPhoto = "";
            if (coach != null) {
                coachId = coach.has("id") ? coach.get("id").asInt() : 0;
                coachName = coach.has("name") ? coach.get("name").asText() : "";
                coachPhoto = coach.has("photo") ? coach.get("photo").asText() : "";
            }

            if (isHome) {
                data.homeFormation = formation;
                data.homeStartingXIPlayers = startPlayers.toString();
                data.homeStartingXINumbers = startNumbers.toString();
                data.homeStartingXIPositions = startPos.toString();
                data.homeSubstitutesPlayers = subPlayers.toString();
                data.homeSubstitutesNumbers = subNumbers.toString();
                data.homeSubstitutesPositions = subPos.toString();
                data.homeCoachId = coachId;
                data.homeCoachName = coachName;
                data.homeCoachPhoto = coachPhoto;
            } else {
                data.awayFormation = formation;
                data.awayStartingXIPlayers = startPlayers.toString();
                data.awayStartingXINumbers = startNumbers.toString();
                data.awayStartingXIPositions = startPos.toString();
                data.awaySubstitutesPlayers = subPlayers.toString();
                data.awaySubstitutesNumbers = subNumbers.toString();
                data.awaySubstitutesPositions = subPos.toString();
                data.awayCoachId = coachId;
                data.awayCoachName = coachName;
                data.awayCoachPhoto = coachPhoto;
            }
        }

        return data;
    }

    /**
     * Parse player statistics data from the API response.
     */
    private PlayerStatsData parsePlayerStats(JsonNode players) {
        PlayerStatsData data = new PlayerStatsData();
        if (players == null || !players.isArray()) return data;

        // Track best performers and aggregates per team
        double maxRatingHome = 0.0, maxRatingAway = 0.0;
        int maxGoalsHome = 0, maxGoalsAway = 0;
        int maxAssistsHome = 0, maxAssistsAway = 0;

        double totalRatingHome = 0.0, totalRatingAway = 0.0;
        int ratingCountHome = 0, ratingCountAway = 0;

        for (int teamIdx = 0; teamIdx < players.size(); teamIdx++) {
            JsonNode teamData = players.get(teamIdx);
            boolean isHome = teamIdx == 0;  // First team is home

            JsonNode playersArray = teamData.get("players");
            if (playersArray == null || !playersArray.isArray()) continue;

            for (JsonNode playerNode : playersArray) {
                JsonNode player = playerNode.get("player");
                JsonNode statistics = playerNode.get("statistics");
                if (player == null || statistics == null || !statistics.isArray() || statistics.size() == 0) continue;

                JsonNode stats = statistics.get(0);  // First statistics element
                String playerName = player.has("name") ? player.get("name").asText() : "";

                // Extract key statistics
                JsonNode games = stats.get("games");
                boolean isCaptain = games != null && games.has("captain") && games.get("captain").asBoolean();

                double rating = 0.0;
                if (games != null && games.has("rating") && !games.get("rating").isNull()) {
                    try {
                        rating = Double.parseDouble(games.get("rating").asText());
                    } catch (NumberFormatException e) {
                        // Ignore invalid rating
                    }
                }

                JsonNode goalsNode = stats.get("goals");
                int goals = 0;
                int assists = 0;
                if (goalsNode != null) {
                    if (goalsNode.has("total") && !goalsNode.get("total").isNull()) {
                        goals = goalsNode.get("total").asInt();
                    }
                    if (goalsNode.has("assists") && !goalsNode.get("assists").isNull()) {
                        assists = goalsNode.get("assists").asInt();
                    }
                }

                // Extract shots
                JsonNode shotsNode = stats.get("shots");
                int shots = 0;
                if (shotsNode != null && shotsNode.has("total") && !shotsNode.get("total").isNull()) {
                    shots = shotsNode.get("total").asInt();
                }

                // Extract passes
                JsonNode passesNode = stats.get("passes");
                int passes = 0;
                if (passesNode != null && passesNode.has("total") && !passesNode.get("total").isNull()) {
                    passes = passesNode.get("total").asInt();
                }

                // Extract tackles
                JsonNode tacklesNode = stats.get("tackles");
                int tackles = 0;
                if (tacklesNode != null && tacklesNode.has("total") && !tacklesNode.get("total").isNull()) {
                    tackles = tacklesNode.get("total").asInt();
                }

                // Extract dribbles
                JsonNode dribblesNode = stats.get("dribbles");
                int dribbles = 0;
                if (dribblesNode != null && dribblesNode.has("attempts") && !dribblesNode.get("attempts").isNull()) {
                    dribbles = dribblesNode.get("attempts").asInt();
                }

                // Extract fouls
                JsonNode foulsNode = stats.get("fouls");
                int foulsCommitted = 0, foulsDrawn = 0;
                if (foulsNode != null) {
                    if (foulsNode.has("committed") && !foulsNode.get("committed").isNull()) {
                        foulsCommitted = foulsNode.get("committed").asInt();
                    }
                    if (foulsNode.has("drawn") && !foulsNode.get("drawn").isNull()) {
                        foulsDrawn = foulsNode.get("drawn").asInt();
                    }
                }

                // Update team-specific data
                if (isHome) {
                    // Captain
                    if (isCaptain) {
                        data.captainHome = playerName;
                    }

                    // Top rated player
                    if (rating > maxRatingHome) {
                        maxRatingHome = rating;
                        data.topRatedPlayerHome = playerName;
                        data.topRatedPlayerHomeRating = String.format("%.1f", rating);
                    }

                    // Top scorer
                    if (goals > maxGoalsHome) {
                        maxGoalsHome = goals;
                        data.topScorerHome = playerName;
                        data.topScorerHomeGoals = goals;
                    }

                    // Top assist provider
                    if (assists > maxAssistsHome) {
                        maxAssistsHome = assists;
                        data.topAssistHome = playerName;
                        data.topAssistHomeAssists = assists;
                    }

                    // Aggregate statistics
                    data.totalShotsHome += shots;
                    data.totalPassesHome += passes;
                    data.totalTacklesHome += tackles;
                    data.totalDribblesHome += dribbles;
                    data.totalFoulsCommittedHome += foulsCommitted;
                    data.totalFoulsDrawnHome += foulsDrawn;

                    if (rating > 0) {
                        totalRatingHome += rating;
                        ratingCountHome++;
                    }
                } else {
                    // Captain
                    if (isCaptain) {
                        data.captainAway = playerName;
                    }

                    // Top rated player
                    if (rating > maxRatingAway) {
                        maxRatingAway = rating;
                        data.topRatedPlayerAway = playerName;
                        data.topRatedPlayerAwayRating = String.format("%.1f", rating);
                    }

                    // Top scorer
                    if (goals > maxGoalsAway) {
                        maxGoalsAway = goals;
                        data.topScorerAway = playerName;
                        data.topScorerAwayGoals = goals;
                    }

                    // Top assist provider
                    if (assists > maxAssistsAway) {
                        maxAssistsAway = assists;
                        data.topAssistAway = playerName;
                        data.topAssistAwayAssists = assists;
                    }

                    // Aggregate statistics
                    data.totalShotsAway += shots;
                    data.totalPassesAway += passes;
                    data.totalTacklesAway += tackles;
                    data.totalDribblesAway += dribbles;
                    data.totalFoulsCommittedAway += foulsCommitted;
                    data.totalFoulsDrawnAway += foulsDrawn;

                    if (rating > 0) {
                        totalRatingAway += rating;
                        ratingCountAway++;
                    }
                }
            }
        }

        // Calculate average ratings
        if (ratingCountHome > 0) {
            data.averageRatingHome = String.format("%.1f", totalRatingHome / ratingCountHome);
        }
        if (ratingCountAway > 0) {
            data.averageRatingAway = String.format("%.1f", totalRatingAway / ratingCountAway);
        }

        return data;
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        java.util.List<DataColumnSpec> columns = new java.util.ArrayList<>();

        // Base Fixture Information (14 columns)
        columns.add(new DataColumnSpecCreator("Fixture_ID", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Referee", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Timezone", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Date", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Timestamp", LongCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Period_First", LongCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Period_Second", LongCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Venue_ID", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Venue_Name", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Venue_City", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Status_Long", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Status_Short", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Status_Elapsed", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Status_Extra", IntCell.TYPE).createSpec());

        // League Information (7 columns)
        columns.add(new DataColumnSpecCreator("League_ID", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("League_Name", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("League_Country", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("League_Logo", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("League_Flag", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Season", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Round", StringCell.TYPE).createSpec());

        // Teams Information (8 columns)
        columns.add(new DataColumnSpecCreator("Home_Team_ID", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Home_Team_Name", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Home_Team_Logo", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Home_Team_Winner", BooleanCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Away_Team_ID", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Away_Team_Name", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Away_Team_Logo", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Away_Team_Winner", BooleanCell.TYPE).createSpec());

        // Goals (2 columns)
        columns.add(new DataColumnSpecCreator("Goals_Home", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Goals_Away", IntCell.TYPE).createSpec());

        // Score Breakdown (8 columns) = 39 base columns total
        columns.add(new DataColumnSpecCreator("Halftime_Home", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Halftime_Away", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Fulltime_Home", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Fulltime_Away", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Extratime_Home", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Extratime_Away", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Penalty_Home", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Penalty_Away", IntCell.TYPE).createSpec());

        // Events columns (18 - if enabled)
        if (m_includeEvents.getBooleanValue()) {
            columns.add(new DataColumnSpecCreator("Goals_Home_Scorers", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Goals_Home_Assists", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Goals_Home_Times", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Goals_Away_Scorers", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Goals_Away_Assists", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Goals_Away_Times", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Yellow_Cards_Home", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Yellow_Cards_Home_Players", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Yellow_Cards_Away", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Yellow_Cards_Away_Players", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Red_Cards_Home", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Red_Cards_Home_Players", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Red_Cards_Away", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Red_Cards_Away_Players", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Substitutions_Home_Count", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Substitutions_Home_Details", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Substitutions_Away_Count", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Substitutions_Away_Details", StringCell.TYPE).createSpec());
        }

        // Statistics columns (32 - if enabled)
        if (m_includeStatistics.getBooleanValue()) {
            // Home team stats (16)
            columns.add(new DataColumnSpecCreator("Stat_Home_Shots_On_Goal", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Home_Shots_Off_Goal", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Home_Total_Shots", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Home_Blocked_Shots", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Home_Shots_Inside_Box", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Home_Shots_Outside_Box", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Home_Fouls", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Home_Corner_Kicks", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Home_Offsides", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Home_Ball_Possession", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Home_Yellow_Cards", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Home_Red_Cards", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Home_Goalkeeper_Saves", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Home_Total_Passes", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Home_Passes_Accurate", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Home_Passes_Percentage", StringCell.TYPE).createSpec());
            // Away team stats (16)
            columns.add(new DataColumnSpecCreator("Stat_Away_Shots_On_Goal", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Away_Shots_Off_Goal", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Away_Total_Shots", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Away_Blocked_Shots", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Away_Shots_Inside_Box", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Away_Shots_Outside_Box", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Away_Fouls", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Away_Corner_Kicks", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Away_Offsides", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Away_Ball_Possession", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Away_Yellow_Cards", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Away_Red_Cards", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Away_Goalkeeper_Saves", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Away_Total_Passes", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Away_Passes_Accurate", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Stat_Away_Passes_Percentage", StringCell.TYPE).createSpec());
        }

        // Lineups columns (20 - if enabled)
        if (m_includeLineups.getBooleanValue()) {
            // Home team lineup (10)
            columns.add(new DataColumnSpecCreator("Lineup_Home_Formation", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Home_Starting_XI_Players", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Home_Starting_XI_Numbers", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Home_Starting_XI_Positions", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Home_Substitutes_Players", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Home_Substitutes_Numbers", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Home_Substitutes_Positions", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Home_Coach_ID", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Home_Coach_Name", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Home_Coach_Photo", StringCell.TYPE).createSpec());
            // Away team lineup (10)
            columns.add(new DataColumnSpecCreator("Lineup_Away_Formation", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Away_Starting_XI_Players", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Away_Starting_XI_Numbers", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Away_Starting_XI_Positions", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Away_Substitutes_Players", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Away_Substitutes_Numbers", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Away_Substitutes_Positions", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Away_Coach_ID", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Away_Coach_Name", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Lineup_Away_Coach_Photo", StringCell.TYPE).createSpec());
        }

        // Player Stats columns (28 - if enabled)
        if (m_includePlayerStats.getBooleanValue()) {
            // Top performers (12 columns)
            columns.add(new DataColumnSpecCreator("Top_Rated_Player_Home", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Top_Rated_Player_Home_Rating", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Top_Rated_Player_Away", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Top_Rated_Player_Away_Rating", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Top_Scorer_Home", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Top_Scorer_Home_Goals", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Top_Scorer_Away", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Top_Scorer_Away_Goals", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Top_Assist_Home", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Top_Assist_Home_Assists", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Top_Assist_Away", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Top_Assist_Away_Assists", IntCell.TYPE).createSpec());
            // Team aggregates (16 columns)
            columns.add(new DataColumnSpecCreator("Total_Shots_Home_Players", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Total_Shots_Away_Players", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Total_Passes_Home_Players", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Total_Passes_Away_Players", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Total_Tackles_Home_Players", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Total_Tackles_Away_Players", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Total_Dribbles_Home_Players", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Total_Dribbles_Away_Players", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Total_Fouls_Committed_Home", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Total_Fouls_Committed_Away", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Total_Fouls_Drawn_Home", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Total_Fouls_Drawn_Away", IntCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Average_Rating_Home", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Average_Rating_Away", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Captain_Home", StringCell.TYPE).createSpec());
            columns.add(new DataColumnSpecCreator("Captain_Away", StringCell.TYPE).createSpec());
        }

        return new DataTableSpec(columns.toArray(new DataColumnSpec[0]));
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_queryType.saveSettingsTo(settings);
        m_fromDate.saveSettingsTo(settings);
        m_toDate.saveSettingsTo(settings);
        m_fixtureId.saveSettingsTo(settings);
        m_status.saveSettingsTo(settings);
        m_team2Id.saveSettingsTo(settings);
        m_includeEvents.saveSettingsTo(settings);
        m_includeLineups.saveSettingsTo(settings);
        m_includeStatistics.saveSettingsTo(settings);
        m_includePlayerStats.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_queryType.validateSettings(settings);
        m_fromDate.validateSettings(settings);
        m_toDate.validateSettings(settings);
        m_fixtureId.validateSettings(settings);
        m_status.validateSettings(settings);
        m_team2Id.validateSettings(settings);
        m_includeEvents.validateSettings(settings);
        m_includeLineups.validateSettings(settings);
        m_includeStatistics.validateSettings(settings);
        // Backward compatibility: only validate if setting exists
        if (settings.containsKey(CFGKEY_INCLUDE_PLAYER_STATS)) {
            m_includePlayerStats.validateSettings(settings);
        }
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_queryType.loadSettingsFrom(settings);
        m_fromDate.loadSettingsFrom(settings);
        m_toDate.loadSettingsFrom(settings);
        m_fixtureId.loadSettingsFrom(settings);
        m_status.loadSettingsFrom(settings);
        m_team2Id.loadSettingsFrom(settings);
        m_includeEvents.loadSettingsFrom(settings);
        m_includeLineups.loadSettingsFrom(settings);
        m_includeStatistics.loadSettingsFrom(settings);
        // Backward compatibility: only load if setting exists (default is false)
        if (settings.containsKey(CFGKEY_INCLUDE_PLAYER_STATS)) {
            m_includePlayerStats.loadSettingsFrom(settings);
        }
    }
}
