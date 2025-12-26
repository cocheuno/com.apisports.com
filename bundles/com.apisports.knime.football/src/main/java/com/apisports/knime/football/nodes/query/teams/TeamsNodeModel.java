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
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.DataType;
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

        // Create cells array (67 columns total)
        DataCell[] cells = new DataCell[67];
        int colIdx = 0;

        // Team Information (7 columns)
        cells[colIdx++] = getIntCell(team, "id");
        cells[colIdx++] = getStringCell(team, "name");
        cells[colIdx++] = getStringCell(team, "code");
        cells[colIdx++] = getStringCell(team, "country");
        cells[colIdx++] = getIntCell(team, "founded");
        cells[colIdx++] = getBooleanCell(team, "national");
        cells[colIdx++] = getStringCell(team, "logo");

        // Venue Information (7 columns)
        cells[colIdx++] = getIntCell(venue, "id");
        cells[colIdx++] = getStringCell(venue, "name");
        cells[colIdx++] = getStringCell(venue, "address");
        cells[colIdx++] = getStringCell(venue, "city");
        cells[colIdx++] = getIntCell(venue, "capacity");
        cells[colIdx++] = getStringCell(venue, "surface");
        cells[colIdx++] = getStringCell(venue, "image");

        // Statistics (remaining 53 columns - only populated if statistics are provided)
        if (statistics != null) {
            // Form
            cells[colIdx++] = getStringCell(statistics, "form");

            // Fixtures (12 columns)
            JsonNode fixtures = statistics.get("fixtures");
            if (fixtures != null) {
                cells[colIdx++] = getNestedIntCell(fixtures, "played", "home");
                cells[colIdx++] = getNestedIntCell(fixtures, "played", "away");
                cells[colIdx++] = getNestedIntCell(fixtures, "played", "total");
                cells[colIdx++] = getNestedIntCell(fixtures, "wins", "home");
                cells[colIdx++] = getNestedIntCell(fixtures, "wins", "away");
                cells[colIdx++] = getNestedIntCell(fixtures, "wins", "total");
                cells[colIdx++] = getNestedIntCell(fixtures, "draws", "home");
                cells[colIdx++] = getNestedIntCell(fixtures, "draws", "away");
                cells[colIdx++] = getNestedIntCell(fixtures, "draws", "total");
                cells[colIdx++] = getNestedIntCell(fixtures, "losses", "home");
                cells[colIdx++] = getNestedIntCell(fixtures, "losses", "away");
                cells[colIdx++] = getNestedIntCell(fixtures, "losses", "total");
            } else {
                colIdx += 12;
            }

            // Goals For (6 columns)
            JsonNode goals = statistics.get("goals");
            if (goals != null) {
                JsonNode goalsFor = goals.get("for");
                if (goalsFor != null) {
                    JsonNode forTotal = goalsFor.get("total");
                    JsonNode forAverage = goalsFor.get("average");
                    cells[colIdx++] = getIntCell(forTotal, "home");
                    cells[colIdx++] = getIntCell(forTotal, "away");
                    cells[colIdx++] = getIntCell(forTotal, "total");
                    cells[colIdx++] = getDoubleCell(forAverage, "home");
                    cells[colIdx++] = getDoubleCell(forAverage, "away");
                    cells[colIdx++] = getDoubleCell(forAverage, "total");
                } else {
                    colIdx += 6;
                }

                // Goals Against (6 columns)
                JsonNode goalsAgainst = goals.get("against");
                if (goalsAgainst != null) {
                    JsonNode againstTotal = goalsAgainst.get("total");
                    JsonNode againstAverage = goalsAgainst.get("average");
                    cells[colIdx++] = getIntCell(againstTotal, "home");
                    cells[colIdx++] = getIntCell(againstTotal, "away");
                    cells[colIdx++] = getIntCell(againstTotal, "total");
                    cells[colIdx++] = getDoubleCell(againstAverage, "home");
                    cells[colIdx++] = getDoubleCell(againstAverage, "away");
                    cells[colIdx++] = getDoubleCell(againstAverage, "total");
                } else {
                    colIdx += 6;
                }
            } else {
                colIdx += 12;
            }

            // Biggest Stats (11 columns)
            JsonNode biggest = statistics.get("biggest");
            if (biggest != null) {
                JsonNode streak = biggest.get("streak");
                cells[colIdx++] = getIntCell(streak, "wins");
                cells[colIdx++] = getIntCell(streak, "draws");
                cells[colIdx++] = getIntCell(streak, "loses");

                JsonNode wins = biggest.get("wins");
                cells[colIdx++] = getStringCell(wins, "home");
                cells[colIdx++] = getStringCell(wins, "away");

                JsonNode loses = biggest.get("loses");
                cells[colIdx++] = getStringCell(loses, "home");
                cells[colIdx++] = getStringCell(loses, "away");

                JsonNode bigGoals = biggest.get("goals");
                if (bigGoals != null) {
                    JsonNode bigFor = bigGoals.get("for");
                    JsonNode bigAgainst = bigGoals.get("against");
                    cells[colIdx++] = getIntCell(bigFor, "home");
                    cells[colIdx++] = getIntCell(bigFor, "away");
                    cells[colIdx++] = getIntCell(bigAgainst, "home");
                    cells[colIdx++] = getIntCell(bigAgainst, "away");
                } else {
                    colIdx += 4;
                }
            } else {
                colIdx += 11;
            }

            // Clean Sheets & Failed to Score (6 columns)
            JsonNode cleanSheet = statistics.get("clean_sheet");
            cells[colIdx++] = getIntCell(cleanSheet, "home");
            cells[colIdx++] = getIntCell(cleanSheet, "away");
            cells[colIdx++] = getIntCell(cleanSheet, "total");

            JsonNode failedToScore = statistics.get("failed_to_score");
            cells[colIdx++] = getIntCell(failedToScore, "home");
            cells[colIdx++] = getIntCell(failedToScore, "away");
            cells[colIdx++] = getIntCell(failedToScore, "total");

            // Penalties (5 columns)
            JsonNode penalty = statistics.get("penalty");
            if (penalty != null) {
                JsonNode scored = penalty.get("scored");
                JsonNode missed = penalty.get("missed");
                cells[colIdx++] = getIntCell(scored, "total");
                cells[colIdx++] = getStringCell(scored, "percentage");
                cells[colIdx++] = getIntCell(missed, "total");
                cells[colIdx++] = getStringCell(missed, "percentage");
                cells[colIdx++] = getIntCell(penalty, "total");
            } else {
                colIdx += 5;
            }

            // Lineups (1 column - formatted as "formation:count, formation:count")
            JsonNode lineups = statistics.get("lineups");
            if (lineups != null && lineups.isArray()) {
                StringBuilder lineupsStr = new StringBuilder();
                for (JsonNode lineup : lineups) {
                    if (lineupsStr.length() > 0) lineupsStr.append(", ");
                    lineupsStr.append(lineup.get("formation").asText())
                              .append(":")
                              .append(lineup.get("played").asInt());
                }
                cells[colIdx++] = new StringCell(lineupsStr.toString());
            } else {
                cells[colIdx++] = DataType.getMissingCell();
            }
        } else {
            // No statistics - fill remaining columns with missing cells
            while (colIdx < 67) {
                cells[colIdx++] = DataType.getMissingCell();
            }
        }

        return new DefaultRow(new RowKey("Row" + rowNum), cells);
    }

    // Helper methods for extracting values safely
    private DataCell getIntCell(JsonNode node, String field) {
        if (node != null && node.has(field) && !node.get(field).isNull()) {
            return new IntCell(node.get(field).asInt());
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

    private DataCell getDoubleCell(JsonNode node, String field) {
        if (node != null && node.has(field) && !node.get(field).isNull()) {
            String value = node.get(field).asText();
            try {
                return new DoubleCell(Double.parseDouble(value));
            } catch (NumberFormatException e) {
                return DataType.getMissingCell();
            }
        }
        return DataType.getMissingCell();
    }

    private DataCell getNestedIntCell(JsonNode parent, String field1, String field2) {
        if (parent != null && parent.has(field1)) {
            JsonNode child = parent.get(field1);
            if (child != null && child.has(field2) && !child.get(field2).isNull()) {
                return new IntCell(child.get(field2).asInt());
            }
        }
        return DataType.getMissingCell();
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            // Team Information
            new DataColumnSpecCreator("Team_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_Code", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_Country", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_Founded", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_National", BooleanCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_Logo", StringCell.TYPE).createSpec(),

            // Venue Information
            new DataColumnSpecCreator("Venue_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Venue_Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Venue_Address", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Venue_City", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Venue_Capacity", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Venue_Surface", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Venue_Image", StringCell.TYPE).createSpec(),

            // Statistics (when enabled)
            new DataColumnSpecCreator("Form", StringCell.TYPE).createSpec(),

            // Fixtures
            new DataColumnSpecCreator("Fixtures_Played_Home", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Fixtures_Played_Away", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Fixtures_Played_Total", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Fixtures_Wins_Home", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Fixtures_Wins_Away", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Fixtures_Wins_Total", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Fixtures_Draws_Home", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Fixtures_Draws_Away", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Fixtures_Draws_Total", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Fixtures_Losses_Home", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Fixtures_Losses_Away", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Fixtures_Losses_Total", IntCell.TYPE).createSpec(),

            // Goals For
            new DataColumnSpecCreator("Goals_For_Total_Home", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goals_For_Total_Away", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goals_For_Total_Total", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goals_For_Average_Home", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goals_For_Average_Away", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goals_For_Average_Total", DoubleCell.TYPE).createSpec(),

            // Goals Against
            new DataColumnSpecCreator("Goals_Against_Total_Home", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goals_Against_Total_Away", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goals_Against_Total_Total", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goals_Against_Average_Home", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goals_Against_Average_Away", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goals_Against_Average_Total", DoubleCell.TYPE).createSpec(),

            // Biggest Stats
            new DataColumnSpecCreator("Biggest_Streak_Wins", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Biggest_Streak_Draws", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Biggest_Streak_Losses", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Biggest_Wins_Home", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Biggest_Wins_Away", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Biggest_Losses_Home", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Biggest_Losses_Away", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Biggest_Goals_For_Home", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Biggest_Goals_For_Away", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Biggest_Goals_Against_Home", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Biggest_Goals_Against_Away", IntCell.TYPE).createSpec(),

            // Clean Sheets & Failed to Score
            new DataColumnSpecCreator("Clean_Sheet_Home", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Clean_Sheet_Away", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Clean_Sheet_Total", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Failed_To_Score_Home", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Failed_To_Score_Away", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Failed_To_Score_Total", IntCell.TYPE).createSpec(),

            // Penalties
            new DataColumnSpecCreator("Penalty_Scored_Total", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Penalty_Scored_Percentage", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Penalty_Missed_Total", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Penalty_Missed_Percentage", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Penalty_Total", IntCell.TYPE).createSpec(),

            // Lineups (most used formations)
            new DataColumnSpecCreator("Lineups", StringCell.TYPE).createSpec()
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
