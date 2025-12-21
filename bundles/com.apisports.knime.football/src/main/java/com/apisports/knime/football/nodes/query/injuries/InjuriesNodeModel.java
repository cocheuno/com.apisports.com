package com.apisports.knime.football.nodes.query.injuries;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.*;
import java.util.*;

public class InjuriesNodeModel extends AbstractFootballQueryNodeModel {

    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("league", String.valueOf(m_leagueId.getIntValue()));
        params.put("season", String.valueOf(m_season.getIntValue()));
        
        if (m_teamId.getIntValue() > 0) {
            params.put("team", String.valueOf(m_teamId.getIntValue()));
        }

        exec.setMessage("Querying injuries from API...");
        JsonNode response = callApi(client, "/injuries", params, mapper);

        return parseInjuriesResponse(response, exec);
    }

    private BufferedDataTable parseInjuriesResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);
        int rowNum = 0;

        if (response != null && response.isArray()) {
            for (JsonNode item : response) {
                try {
                    JsonNode player = item.get("player");
                    JsonNode team = item.get("team");
                    JsonNode fixture = item.get("fixture");

                    int playerId = player != null && player.has("id") ? player.get("id").asInt() : 0;
                    String playerName = player != null && player.has("name") ? player.get("name").asText() : "";
                    String playerType = player != null && player.has("type") ? player.get("type").asText() : "";
                    String reason = player != null && player.has("reason") ? player.get("reason").asText() : "";
                    
                    int teamId = team != null && team.has("id") ? team.get("id").asInt() : 0;
                    String teamName = team != null && team.has("name") ? team.get("name").asText() : "";
                    
                    int fixtureId = fixture != null && fixture.has("id") ? fixture.get("id").asInt() : 0;
                    String fixtureDate = fixture != null && fixture.has("date") ? fixture.get("date").asText() : "";

                    DataCell[] cells = new DataCell[]{
                        new IntCell(playerId),
                        new StringCell(playerName),
                        new StringCell(playerType),
                        new StringCell(reason),
                        new IntCell(teamId),
                        new StringCell(teamName),
                        new IntCell(fixtureId),
                        new StringCell(fixtureDate)
                    };

                    container.addRowToTable(new DefaultRow(new RowKey("Row" + rowNum), cells));
                    rowNum++;
                } catch (Exception e) {
                    getLogger().warn("Failed to parse injury: " + e.getMessage());
                }
            }
        }

        container.close();
        return container.getTable();
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Player_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Player_Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Injury_Type", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Reason", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Fixture_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Fixture_Date", StringCell.TYPE).createSpec()
        );
    }
}
