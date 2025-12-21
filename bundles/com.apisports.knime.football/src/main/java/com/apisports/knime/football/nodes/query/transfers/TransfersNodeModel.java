package com.apisports.knime.football.nodes.query.transfers;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.*;
import java.util.*;

public class TransfersNodeModel extends AbstractFootballQueryNodeModel {
    @Override
    protected void validateExecutionSettings() throws InvalidSettingsException {
        if (m_teamId.getIntValue() <= 0) {
            throw new InvalidSettingsException("Please select a team");
        }
    }

    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("team", String.valueOf(m_teamId.getIntValue()));

        exec.setMessage("Querying transfers from API...");
        JsonNode response = callApi(client, "/transfers", params, mapper);
        return parseResponse(response, exec);
    }

    private BufferedDataTable parseResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);
        int rowNum = 0;

        if (response != null && response.isArray()) {
            for (JsonNode item : response) {
                try {
                    JsonNode player = item.get("player");
                    JsonNode transfers = item.get("transfers");

                    int playerId = player != null && player.has("id") ? player.get("id").asInt() : 0;
                    String playerName = player != null && player.has("name") ? player.get("name").asText() : "";

                    if (transfers != null && transfers.isArray()) {
                        for (JsonNode transfer : transfers) {
                            String date = transfer.has("date") ? transfer.get("date").asText() : "";
                            String type = transfer.has("type") ? transfer.get("type").asText() : "";

                            JsonNode teamsNode = transfer.get("teams");
                            String teamIn = teamsNode != null && teamsNode.has("in") && teamsNode.get("in").has("name")
                                ? teamsNode.get("in").get("name").asText() : "";
                            String teamOut = teamsNode != null && teamsNode.has("out") && teamsNode.get("out").has("name")
                                ? teamsNode.get("out").get("name").asText() : "";

                            DataCell[] cells = new DataCell[]{
                                new IntCell(playerId),
                                new StringCell(playerName),
                                new StringCell(date),
                                new StringCell(type),
                                new StringCell(teamOut),
                                new StringCell(teamIn)
                            };
                            container.addRowToTable(new DefaultRow(new RowKey("Row" + rowNum), cells));
                            rowNum++;
                        }
                    }
                } catch (Exception e) {
                    getLogger().warn("Failed to parse transfer: " + e.getMessage());
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
            new DataColumnSpecCreator("Date", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Type", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_Out", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_In", StringCell.TYPE).createSpec()
        );
    }
}
