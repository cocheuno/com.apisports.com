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
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * NodeModel for Fixtures Selector node.
 * Returns basic fixture information without expensive additional API calls.
 * Output can be filtered and fed into Fixtures Details node.
 */
public class FixturesSelectorNodeModel extends AbstractFootballQueryNodeModel {

    // Fixtures-specific settings (same as FixturesNodeModel but without include options)
    static final String CFGKEY_QUERY_TYPE = "queryType";
    static final String CFGKEY_FIXTURE_ID = "fixtureId";
    static final String CFGKEY_STATUS = "status";
    static final String CFGKEY_TEAM2_ID = "team2Id";

    // DateRangePanel settings keys
    static final String CFGKEY_DATE_MODE = "dateMode";
    static final String CFGKEY_FROM_DATE = "fromDate";
    static final String CFGKEY_TO_DATE = "toDate";
    static final String CFGKEY_RELATIVE_DIRECTION = "relativeDirection";
    static final String CFGKEY_RELATIVE_COUNT = "relativeCount";
    static final String CFGKEY_RELATIVE_UNIT = "relativeUnit";

    // DateRangePanel mode constants (must match DateRangePanel.java)
    static final String MODE_RANGE = "range";
    static final String MODE_RELATIVE = "relative";
    static final String MODE_SEASON = "season";

    // Relative mode constants
    static final String DIR_NEXT = "next";
    static final String DIR_LAST = "last";
    static final String UNIT_MATCHES = "matches";
    static final String UNIT_DAYS = "days";
    static final String UNIT_WEEKS = "weeks";

    // Query type options
    static final String QUERY_BY_LEAGUE = "By League/Season";
    static final String QUERY_BY_DATE = "By Date Range";
    static final String QUERY_BY_TEAM = "By Team";
    static final String QUERY_BY_ID = "By Fixture ID";
    static final String QUERY_LIVE = "Live Fixtures";
    static final String QUERY_H2H = "Head to Head";

    protected final SettingsModelString m_queryType =
        new SettingsModelString(CFGKEY_QUERY_TYPE, QUERY_BY_LEAGUE);
    protected final SettingsModelString m_fixtureId =
        new SettingsModelString(CFGKEY_FIXTURE_ID, "");
    protected final SettingsModelString m_status =
        new SettingsModelString(CFGKEY_STATUS, "");
    protected final SettingsModelInteger m_team2Id =
        new SettingsModelInteger(CFGKEY_TEAM2_ID, -1);

    // DateRangePanel settings models
    protected final SettingsModelString m_dateMode =
        new SettingsModelString(CFGKEY_DATE_MODE, MODE_RANGE);
    protected final SettingsModelString m_fromDate =
        new SettingsModelString(CFGKEY_FROM_DATE, LocalDate.now().toString());
    protected final SettingsModelString m_toDate =
        new SettingsModelString(CFGKEY_TO_DATE, LocalDate.now().plusDays(7).toString());
    protected final SettingsModelString m_relativeDirection =
        new SettingsModelString(CFGKEY_RELATIVE_DIRECTION, DIR_NEXT);
    protected final SettingsModelInteger m_relativeCount =
        new SettingsModelInteger(CFGKEY_RELATIVE_COUNT, 10);
    protected final SettingsModelString m_relativeUnit =
        new SettingsModelString(CFGKEY_RELATIVE_UNIT, UNIT_MATCHES);

    @Override
    protected void validateExecutionSettings() throws InvalidSettingsException {
        String queryType = m_queryType.getStringValue();

        // Validation depends on query type
        if (QUERY_BY_LEAGUE.equals(queryType) || QUERY_BY_TEAM.equals(queryType)) {
            super.validateExecutionSettings(); // Validates league and season
        } else if (QUERY_BY_DATE.equals(queryType)) {
            // Validate date mode settings
            validateDateMode();
            // Season is optional for date range queries - if not specified, API returns all fixtures for the date range
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
            // Season is optional for H2H - if not specified, returns all historical H2H matches
        }
        // QUERY_LIVE has no special validation
    }

    /**
     * Validate date mode settings.
     */
    private void validateDateMode() throws InvalidSettingsException {
        String mode = m_dateMode.getStringValue();

        if (MODE_RANGE.equals(mode)) {
            if (m_fromDate.getStringValue().isEmpty()) {
                throw new InvalidSettingsException("Please specify a start date");
            }
            validateDateFormat(m_fromDate.getStringValue(), "From Date");
            if (!m_toDate.getStringValue().isEmpty()) {
                validateDateFormat(m_toDate.getStringValue(), "To Date");
            }
        } else if (MODE_RELATIVE.equals(mode)) {
            if (m_relativeCount.getIntValue() <= 0) {
                throw new InvalidSettingsException("Relative count must be greater than 0");
            }
        } else if (MODE_SEASON.equals(mode)) {
            // Season mode: use current season dates
            // No additional validation needed
        }
    }

    /**
     * Resolve date range based on DateRangePanel mode.
     * Returns array: [fromDate, toDate] as YYYY-MM-DD strings.
     */
    private String[] resolveDateRange() throws InvalidSettingsException {
        String mode = m_dateMode.getStringValue();
        LocalDate today = LocalDate.now();

        // Log all date-related settings for debugging
        getLogger().warn("=== DATE RESOLUTION DEBUG ===");
        getLogger().warn("Date mode from settings: '" + mode + "'");
        getLogger().warn("m_fromDate value: '" + m_fromDate.getStringValue() + "'");
        getLogger().warn("m_toDate value: '" + m_toDate.getStringValue() + "'");

        if (MODE_RANGE.equals(mode)) {
            // Use dates as-is
            String from = m_fromDate.getStringValue();
            String to = m_toDate.getStringValue().isEmpty() ? from : m_toDate.getStringValue();
            getLogger().warn("Using RANGE mode - from: '" + from + "', to: '" + to + "'");
            return new String[]{from, to};

        } else if (MODE_RELATIVE.equals(mode)) {
            String direction = m_relativeDirection.getStringValue();
            int count = m_relativeCount.getIntValue();
            String unit = m_relativeUnit.getStringValue();

            if (UNIT_DAYS.equals(unit)) {
                // Days-based relative range
                if (DIR_NEXT.equals(direction)) {
                    return new String[]{today.toString(), today.plusDays(count).toString()};
                } else { // LAST
                    return new String[]{today.minusDays(count).toString(), today.toString()};
                }
            } else if (UNIT_WEEKS.equals(unit)) {
                // Weeks-based relative range
                if (DIR_NEXT.equals(direction)) {
                    return new String[]{today.toString(), today.plusWeeks(count).toString()};
                } else { // LAST
                    return new String[]{today.minusWeeks(count).toString(), today.toString()};
                }
            } else if (UNIT_MATCHES.equals(unit)) {
                // Matches-based: Query wide range, will limit results in parsing
                if (DIR_NEXT.equals(direction)) {
                    // Query next 6 months to capture enough fixtures
                    return new String[]{today.toString(), today.plusMonths(6).toString()};
                } else { // LAST
                    // Query last 6 months
                    return new String[]{today.minusMonths(6).toString(), today.toString()};
                }
            }

        } else if (MODE_SEASON.equals(mode)) {
            // Season mode: Use reasonable season boundaries (Aug 1 to May 31)
            // This covers most football leagues
            int currentYear = today.getYear();
            int startYear = today.getMonthValue() >= 8 ? currentYear : currentYear - 1;
            return new String[]{
                startYear + "-08-01",
                (startYear + 1) + "-05-31"
            };
        }

        throw new InvalidSettingsException("Unknown date mode: " + mode);
    }

    /**
     * Validate date format - accepts multiple formats.
     */
    private void validateDateFormat(String date, String fieldName) throws InvalidSettingsException {
        if (date == null || date.trim().isEmpty()) {
            throw new InvalidSettingsException(fieldName + " cannot be empty");
        }

        // Try to parse the date to validate it
        try {
            normalizeDateFormat(date);
        } catch (Exception e) {
            throw new InvalidSettingsException(fieldName + " is not a valid date. Supported formats: YYYY-MM-DD, MM/DD/YYYY, DD/MM/YYYY");
        }
    }

    /**
     * Normalize date format to YYYY-MM-DD for API.
     * Accepts: YYYY-MM-DD, YYYY/MM/DD, MM/DD/YYYY, DD/MM/YYYY, MM-DD-YYYY, DD-MM-YYYY
     */
    private String normalizeDateFormat(String date) {
        if (date == null || date.isEmpty()) {
            return date;
        }

        String trimmed = date.trim();

        // Already in YYYY-MM-DD format
        if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return trimmed;
        }

        // YYYY/MM/DD format
        if (trimmed.matches("\\d{4}/\\d{2}/\\d{2}")) {
            return trimmed.replace('/', '-');
        }

        // MM/DD/YYYY or DD/MM/YYYY format - assume MM/DD/YYYY (US format)
        if (trimmed.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            String[] parts = trimmed.split("/");
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            String year = parts[2];

            // If first number > 12, it must be DD/MM/YYYY
            if (first > 12) {
                return String.format("%s-%02d-%02d", year, second, first);
            }
            // Assume MM/DD/YYYY (US format)
            return String.format("%s-%02d-%02d", year, first, second);
        }

        // MM-DD-YYYY or DD-MM-YYYY format
        if (trimmed.matches("\\d{1,2}-\\d{1,2}-\\d{4}")) {
            String[] parts = trimmed.split("-");
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            String year = parts[2];

            // If first number > 12, it must be DD-MM-YYYY
            if (first > 12) {
                return String.format("%s-%02d-%02d", year, second, first);
            }
            // Assume MM-DD-YYYY (US format)
            return String.format("%s-%02d-%02d", year, first, second);
        }

        // Try parsing with LocalDate as fallback
        try {
            java.time.LocalDate parsed = java.time.LocalDate.parse(trimmed);
            return parsed.toString();
        } catch (Exception e) {
            // Return as-is and let API handle it
            getLogger().warn("Could not parse date format: " + trimmed + ", sending as-is");
            return trimmed;
        }
    }

    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        exec.setMessage("Building query parameters...");

        // Build query parameters based on query type
        Map<String, String> params = buildQueryParams();

        // DIAGNOSTIC: Log query parameters (using WARN level to ensure visibility)
        getLogger().warn("=== FIXTURES QUERY DIAGNOSTICS ===");
        getLogger().warn("Query Type: " + m_queryType.getStringValue());
        getLogger().warn("Full query params: " + params);

        // Determine the endpoint based on query type
        String endpoint = getEndpoint();

        // Make API call
        exec.setMessage("Querying fixtures from API...");
        JsonNode response = callApi(client, endpoint, params, mapper);

        // Parse response and create output table with basic fixture info only
        exec.setMessage("Parsing results...");
        BufferedDataTable result = parseFixturesResponse(response, exec);

        // Push flow variables with configuration for downstream nodes
        pushFlowVariables();

        getLogger().info("Retrieved " + result.size() + " fixtures");
        return result;
    }

    /**
     * Push configuration as flow variables for downstream nodes to use.
     */
    private void pushFlowVariables() {
        // Common settings
        pushFlowVariableInt("fixtures_league_id", m_leagueId.getIntValue());
        pushFlowVariableInt("fixtures_season", m_season.getIntValue());
        pushFlowVariableInt("fixtures_team_id", m_teamId.getIntValue());
        pushFlowVariableString("fixtures_query_type", m_queryType.getStringValue());
        pushFlowVariableString("fixtures_status", m_status.getStringValue());

        // DateRangePanel settings
        pushFlowVariableString("fixtures_date_mode", m_dateMode.getStringValue());
        pushFlowVariableString("fixtures_from_date", m_fromDate.getStringValue());
        pushFlowVariableString("fixtures_to_date", m_toDate.getStringValue());
        pushFlowVariableString("fixtures_relative_direction", m_relativeDirection.getStringValue());
        pushFlowVariableInt("fixtures_relative_count", m_relativeCount.getIntValue());
        pushFlowVariableString("fixtures_relative_unit", m_relativeUnit.getStringValue());

        getLogger().info("Pushed flow variables for downstream nodes");
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
    private Map<String, String> buildQueryParams() throws InvalidSettingsException {
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
            // Resolve date range from DateRangePanel settings
            String[] dateRange = resolveDateRange();
            String fromDate = normalizeDateFormat(dateRange[0]);
            String toDate = normalizeDateFormat(dateRange[1]);

            // Use 'date' parameter for single date, 'from'/'to' for date range
            if (fromDate.equals(toDate)) {
                // Single date query - API expects 'date' parameter
                params.put("date", fromDate);
            } else {
                // Date range query - API expects 'from' and 'to' parameters
                params.put("from", fromDate);
                params.put("to", toDate);
            }

            // Add timezone parameter - use system default timezone
            String timezone = java.time.ZoneId.systemDefault().getId();
            params.put("timezone", timezone);

            // Optional season filter - if not specified, returns fixtures from all seasons
            if (m_season.getIntValue() > 0) {
                params.put("season", String.valueOf(m_season.getIntValue()));
            }

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
            // Optional season filter - if not specified, returns all historical H2H matches
            if (m_season.getIntValue() > 0) {
                params.put("season", String.valueOf(m_season.getIntValue()));
            }
        }

        // Optional status filter
        if (!m_status.getStringValue().isEmpty()) {
            params.put("status", m_status.getStringValue());
        }

        return params;
    }

    /**
     * Parse fixtures API response and create output table with BASIC info only.
     * No expensive additional API calls for statistics, lineups, etc.
     */
    private BufferedDataTable parseFixturesResponse(JsonNode response, ExecutionContext exec) throws Exception {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);

        if (response != null && response.isArray()) {
            int rowNum = 0;

            for (JsonNode fixtureItem : response) {
                try {
                    DataRow row = parseBasicFixtureRow(fixtureItem, rowNum);
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
     * Parse a single fixture JSON object into a DataRow with BASIC info only.
     * Returns: Fixture_ID, Date, Timestamp, Venue_Name, Status,
     *          League_ID, League_Name, Season, Round,
     *          Home_Team_ID, Home_Team_Name, Away_Team_ID, Away_Team_Name,
     *          Goals_Home, Goals_Away
     */
    private DataRow parseBasicFixtureRow(JsonNode fixtureItem, int rowNum) {
        JsonNode fixture = fixtureItem.get("fixture");
        JsonNode league = fixtureItem.get("league");
        JsonNode teams = fixtureItem.get("teams");
        JsonNode goals = fixtureItem.get("goals");

        // 15 basic columns
        DataCell[] cells = new DataCell[15];
        int colIdx = 0;

        // Fixture info
        cells[colIdx++] = getIntCell(fixture, "id");
        cells[colIdx++] = getStringCell(fixture, "date");
        cells[colIdx++] = getLongCell(fixture, "timestamp");

        JsonNode venue = fixture != null ? fixture.get("venue") : null;
        cells[colIdx++] = getStringCell(venue, "name");

        JsonNode status = fixture != null ? fixture.get("status") : null;
        cells[colIdx++] = getStringCell(status, "short");

        // League info
        cells[colIdx++] = getIntCell(league, "id");
        cells[colIdx++] = getStringCell(league, "name");
        cells[colIdx++] = getIntCell(league, "season");
        cells[colIdx++] = getStringCell(league, "round");

        // Teams info
        JsonNode homeTeam = teams != null ? teams.get("home") : null;
        JsonNode awayTeam = teams != null ? teams.get("away") : null;

        cells[colIdx++] = getIntCell(homeTeam, "id");
        cells[colIdx++] = getStringCell(homeTeam, "name");
        cells[colIdx++] = getIntCell(awayTeam, "id");
        cells[colIdx++] = getStringCell(awayTeam, "name");

        // Goals
        cells[colIdx++] = getIntCell(goals, "home");
        cells[colIdx++] = getIntCell(goals, "away");

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

    @Override
    protected DataTableSpec getOutputSpec() {
        java.util.List<DataColumnSpec> columns = new java.util.ArrayList<>();

        // Basic fixture information (14 columns total)
        columns.add(new DataColumnSpecCreator("Fixture_ID", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Date", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Timestamp", LongCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Venue_Name", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Status", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("League_ID", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("League_Name", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Season", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Round", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Home_Team_ID", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Home_Team_Name", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Away_Team_ID", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Away_Team_Name", StringCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Goals_Home", IntCell.TYPE).createSpec());
        columns.add(new DataColumnSpecCreator("Goals_Away", IntCell.TYPE).createSpec());

        return new DataTableSpec(columns.toArray(new DataColumnSpec[0]));
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_queryType.saveSettingsTo(settings);
        m_fixtureId.saveSettingsTo(settings);
        m_status.saveSettingsTo(settings);
        m_team2Id.saveSettingsTo(settings);

        // DateRangePanel settings
        m_dateMode.saveSettingsTo(settings);
        m_fromDate.saveSettingsTo(settings);
        m_toDate.saveSettingsTo(settings);
        m_relativeDirection.saveSettingsTo(settings);
        m_relativeCount.saveSettingsTo(settings);
        m_relativeUnit.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_queryType.validateSettings(settings);
        m_fixtureId.validateSettings(settings);
        m_status.validateSettings(settings);
        m_team2Id.validateSettings(settings);

        // DateRangePanel settings
        m_dateMode.validateSettings(settings);
        m_fromDate.validateSettings(settings);
        m_toDate.validateSettings(settings);
        m_relativeDirection.validateSettings(settings);
        m_relativeCount.validateSettings(settings);
        m_relativeUnit.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_queryType.loadSettingsFrom(settings);
        m_fixtureId.loadSettingsFrom(settings);
        m_status.loadSettingsFrom(settings);
        m_team2Id.loadSettingsFrom(settings);

        // DateRangePanel settings
        m_dateMode.loadSettingsFrom(settings);
        m_fromDate.loadSettingsFrom(settings);
        m_toDate.loadSettingsFrom(settings);
        m_relativeDirection.loadSettingsFrom(settings);
        m_relativeCount.loadSettingsFrom(settings);
        m_relativeUnit.loadSettingsFrom(settings);
    }
}
