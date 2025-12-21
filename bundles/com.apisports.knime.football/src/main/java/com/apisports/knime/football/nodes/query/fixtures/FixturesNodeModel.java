package com.apisports.knime.football.nodes.query.fixtures;

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
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.util.HashMap;
import java.util.Map;

/**
 * NodeModel for Fixtures query node.
 * Queries fixture data from the Football API.
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
        BufferedDataTable result = parseFixturesResponse(response, exec);

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
    private BufferedDataTable parseFixturesResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);

        if (response != null && response.isArray()) {
            int rowNum = 0;

            for (JsonNode fixtureItem : response) {
                try {
                    DataRow row = parseFixtureRow(fixtureItem, rowNum);
                    container.addRowToTable(row);
                    rowNum++;
                } catch (Exception e) {
                    getLogger().warn("Failed to parse fixture row: " + e.getMessage());
                }
            }
        }

        container.close();
        return container.getTable();
    }

    /**
     * Parse a single fixture JSON object into a DataRow.
     */
    private DataRow parseFixtureRow(JsonNode fixtureItem, int rowNum) {
        JsonNode fixture = fixtureItem.get("fixture");
        JsonNode league = fixtureItem.get("league");
        JsonNode teams = fixtureItem.get("teams");
        JsonNode goals = fixtureItem.get("goals");
        JsonNode score = fixtureItem.get("score");

        // Extract fixture data
        int fixtureId = fixture != null && fixture.has("id") ? fixture.get("id").asInt() : 0;
        String date = fixture != null && fixture.has("date") ? fixture.get("date").asText() : "";
        String status = fixture != null && fixture.has("status") && fixture.get("status").has("long")
            ? fixture.get("status").get("long").asText() : "";
        String venue = fixture != null && fixture.has("venue") && fixture.get("venue").has("name")
            ? fixture.get("venue").get("name").asText() : "";

        // Extract league data
        int leagueId = league != null && league.has("id") ? league.get("id").asInt() : 0;
        String leagueName = league != null && league.has("name") ? league.get("name").asText() : "";
        int season = league != null && league.has("season") ? league.get("season").asInt() : 0;
        String round = league != null && league.has("round") ? league.get("round").asText() : "";

        // Extract teams data
        String homeTeam = teams != null && teams.has("home") && teams.get("home").has("name")
            ? teams.get("home").get("name").asText() : "";
        String awayTeam = teams != null && teams.has("away") && teams.get("away").has("name")
            ? teams.get("away").get("name").asText() : "";
        int homeTeamId = teams != null && teams.has("home") && teams.get("home").has("id")
            ? teams.get("home").get("id").asInt() : 0;
        int awayTeamId = teams != null && teams.has("away") && teams.get("away").has("id")
            ? teams.get("away").get("id").asInt() : 0;

        // Extract goals
        String homeGoals = goals != null && goals.has("home") && !goals.get("home").isNull()
            ? String.valueOf(goals.get("home").asInt()) : "";
        String awayGoals = goals != null && goals.has("away") && !goals.get("away").isNull()
            ? String.valueOf(goals.get("away").asInt()) : "";

        // Create row cells
        DataCell[] cells = new DataCell[]{
            new IntCell(fixtureId),
            new StringCell(date),
            new StringCell(status),
            new IntCell(leagueId),
            new StringCell(leagueName),
            new IntCell(season),
            new StringCell(round),
            new IntCell(homeTeamId),
            new StringCell(homeTeam),
            new IntCell(awayTeamId),
            new StringCell(awayTeam),
            new StringCell(homeGoals),
            new StringCell(awayGoals),
            new StringCell(venue)
        };

        return new DefaultRow(new RowKey("Row" + rowNum), cells);
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Fixture_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Date", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Status", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("League_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("League_Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Season", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Round", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Home_Team_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Home_Team", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Away_Team_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Away_Team", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Home_Goals", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Away_Goals", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Venue", StringCell.TYPE).createSpec()
        );
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
    }
}
