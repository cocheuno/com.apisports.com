package com.apisports.knime.football.nodes.query.standings;

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

import java.util.HashMap;
import java.util.Map;

/**
 * NodeModel for Standings query node.
 * Queries league standings/tables from the Football API.
 */
public class StandingsNodeModel extends AbstractFootballQueryNodeModel {

    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        exec.setMessage("Building query parameters...");

        // Build query parameters
        Map<String, String> params = new HashMap<>();
        params.put("league", String.valueOf(m_leagueId.getIntValue()));
        params.put("season", String.valueOf(m_season.getIntValue()));

        // Make API call
        exec.setMessage("Querying standings from API...");
        getLogger().info("Fetching standings for " + getLeagueName(m_leagueId.getIntValue()) +
                        ", season " + m_season.getIntValue());
        JsonNode response = callApi(client, "/standings", params, mapper);

        // Parse response and create output table
        exec.setMessage("Parsing results...");
        BufferedDataTable result = parseStandingsResponse(response, exec);

        getLogger().info("Retrieved standings with " + result.size() + " teams");
        return result;
    }

    /**
     * Parse standings API response and create output table.
     */
    private BufferedDataTable parseStandingsResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);

        if (response != null && response.isArray() && response.size() > 0) {
            JsonNode leagueData = response.get(0);
            JsonNode league = leagueData.get("league");
            JsonNode standingsArray = league != null ? league.get("standings") : null;

            if (standingsArray != null && standingsArray.isArray() && standingsArray.size() > 0) {
                // Get the first standings group (usually the main league table)
                JsonNode standings = standingsArray.get(0);

                if (standings.isArray()) {
                    int rowNum = 0;

                    for (JsonNode teamData : standings) {
                        try {
                            DataRow row = parseTeamStandingRow(teamData, rowNum);
                            container.addRowToTable(row);
                            rowNum++;
                        } catch (Exception e) {
                            getLogger().warn("Failed to parse standing row: " + e.getMessage());
                        }
                    }
                }
            }
        }

        container.close();
        return container.getTable();
    }

    /**
     * Parse a single team standing JSON object into a DataRow.
     */
    private DataRow parseTeamStandingRow(JsonNode teamData, int rowNum) {
        int rank = teamData.has("rank") ? teamData.get("rank").asInt() : 0;

        JsonNode team = teamData.get("team");
        int teamId = team != null && team.has("id") ? team.get("id").asInt() : 0;
        String teamName = team != null && team.has("name") ? team.get("name").asText() : "";

        String description = teamData.has("description") ? teamData.get("description").asText() : "";
        String form = teamData.has("form") ? teamData.get("form").asText() : "";

        JsonNode all = teamData.get("all");
        int played = all != null && all.has("played") ? all.get("played").asInt() : 0;
        int win = all != null && all.has("win") ? all.get("win").asInt() : 0;
        int draw = all != null && all.has("draw") ? all.get("draw").asInt() : 0;
        int lose = all != null && all.has("lose") ? all.get("lose").asInt() : 0;

        JsonNode goals = all != null ? all.get("goals") : null;
        int goalsFor = goals != null && goals.has("for") ? goals.get("for").asInt() : 0;
        int goalsAgainst = goals != null && goals.has("against") ? goals.get("against").asInt() : 0;
        int goalsDiff = teamData.has("goalsDiff") ? teamData.get("goalsDiff").asInt() : 0;

        int points = teamData.has("points") ? teamData.get("points").asInt() : 0;

        // Create row cells
        DataCell[] cells = new DataCell[]{
            new IntCell(rank),
            new IntCell(teamId),
            new StringCell(teamName),
            new IntCell(points),
            new IntCell(played),
            new IntCell(win),
            new IntCell(draw),
            new IntCell(lose),
            new IntCell(goalsFor),
            new IntCell(goalsAgainst),
            new IntCell(goalsDiff),
            new StringCell(form),
            new StringCell(description)
        };

        return new DefaultRow(new RowKey("Row" + rowNum), cells);
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Rank", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Points", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Played", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Win", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Draw", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Lose", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goals_For", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goals_Against", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Goal_Difference", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Form", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Description", StringCell.TYPE).createSpec()
        );
    }
}
