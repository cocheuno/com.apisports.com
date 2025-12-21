package com.apisports.knime.football.nodes.query.coaches;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.*;
import java.util.*;

public class CoachesNodeModel extends AbstractFootballQueryNodeModel {
    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        Map<String, String> params = new HashMap<>();
        if (m_teamId.getIntValue() > 0) {
            params.put("team", String.valueOf(m_teamId.getIntValue()));
        }

        exec.setMessage("Querying coaches from API...");
        JsonNode response = callApi(client, "/coachs", params, mapper);
        return parseResponse(response, exec);
    }

    private BufferedDataTable parseResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);
        int rowNum = 0;

        if (response != null && response.isArray()) {
            for (JsonNode item : response) {
                try {
                    int id = item.has("id") ? item.get("id").asInt() : 0;
                    String name = item.has("name") ? item.get("name").asText() : "";
                    String firstname = item.has("firstname") ? item.get("firstname").asText() : "";
                    String lastname = item.has("lastname") ? item.get("lastname").asText() : "";
                    String age = item.has("age") && !item.get("age").isNull() ? String.valueOf(item.get("age").asInt()) : "";
                    String nationality = item.has("nationality") ? item.get("nationality").asText() : "";
                    
                    JsonNode team = item.get("team");
                    String teamName = team != null && team.has("name") ? team.get("name").asText() : "";

                    DataCell[] cells = new DataCell[]{
                        new IntCell(id), new StringCell(name), new StringCell(firstname),
                        new StringCell(lastname), new StringCell(age), new StringCell(nationality),
                        new StringCell(teamName)
                    };
                    container.addRowToTable(new DefaultRow(new RowKey("Row" + rowNum), cells));
                    rowNum++;
                } catch (Exception e) {
                    getLogger().warn("Failed to parse coach: " + e.getMessage());
                }
            }
        }
        container.close();
        return container.getTable();
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Coach_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Firstname", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Lastname", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Age", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Nationality", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team", StringCell.TYPE).createSpec()
        );
    }
}
