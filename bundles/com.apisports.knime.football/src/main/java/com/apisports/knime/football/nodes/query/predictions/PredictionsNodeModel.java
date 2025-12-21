package com.apisports.knime.football.nodes.query.predictions;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.*;
import java.util.*;

public class PredictionsNodeModel extends AbstractFootballQueryNodeModel {
    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("league", String.valueOf(m_leagueId.getIntValue()));
        params.put("season", String.valueOf(m_season.getIntValue()));

        exec.setMessage("Querying predictions from API...");
        JsonNode response = callApi(client, "/predictions", params, mapper);
        return parseResponse(response, exec);
    }

    private BufferedDataTable parseResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);
        int rowNum = 0;

        if (response != null && response.isArray()) {
            for (JsonNode item : response) {
                try {
                    JsonNode predictions = item.get("predictions");
                    JsonNode league = item.get("league");
                    JsonNode teams = item.get("teams");

                    int fixtureId = 0;
                    if (item.has("fixture") && item.get("fixture").has("id")) {
                        fixtureId = item.get("fixture").get("id").asInt();
                    }

                    String leagueName = league != null && league.has("name") ? league.get("name").asText() : "";
                    String winner = predictions != null && predictions.has("winner") && predictions.get("winner").has("name")
                        ? predictions.get("winner").get("name").asText() : "";
                    String winPercent = predictions != null && predictions.has("percent") && predictions.get("percent").has(winner.toLowerCase())
                        ? predictions.get("percent").get(winner.toLowerCase()).asText() : "";
                    String advice = predictions != null && predictions.has("advice") ? predictions.get("advice").asText() : "";

                    String homeTeam = teams != null && teams.has("home") && teams.get("home").has("name")
                        ? teams.get("home").get("name").asText() : "";
                    String awayTeam = teams != null && teams.has("away") && teams.get("away").has("name")
                        ? teams.get("away").get("name").asText() : "";

                    DataCell[] cells = new DataCell[]{
                        new IntCell(fixtureId),
                        new StringCell(leagueName),
                        new StringCell(homeTeam),
                        new StringCell(awayTeam),
                        new StringCell(winner),
                        new StringCell(winPercent),
                        new StringCell(advice)
                    };
                    container.addRowToTable(new DefaultRow(new RowKey("Row" + rowNum), cells));
                    rowNum++;
                } catch (Exception e) {
                    getLogger().warn("Failed to parse prediction: " + e.getMessage());
                }
            }
        }
        container.close();
        return container.getTable();
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Fixture_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("League", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Home_Team", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Away_Team", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Winner", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Win_Percent", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Advice", StringCell.TYPE).createSpec()
        );
    }
}
