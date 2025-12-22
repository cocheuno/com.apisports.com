package com.apisports.knime.football.nodes.query.trophies;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.*;
import java.util.*;

public class TrophiesNodeModel extends AbstractFootballQueryNodeModel {
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

        exec.setMessage("Querying trophies from API...");
        JsonNode response = callApi(client, "/trophies", params, mapper);
        return parseResponse(response, exec);
    }

    private BufferedDataTable parseResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);
        int rowNum = 0;

        if (response != null && response.isArray()) {
            for (JsonNode item : response) {
                try {
                    String league = item.has("league") ? item.get("league").asText() : "";
                    String country = item.has("country") ? item.get("country").asText() : "";
                    String season = item.has("season") ? item.get("season").asText() : "";
                    String place = item.has("place") ? item.get("place").asText() : "";

                    DataCell[] cells = new DataCell[]{
                        new StringCell(league),
                        new StringCell(country),
                        new StringCell(season),
                        new StringCell(place)
                    };
                    container.addRowToTable(new DefaultRow(new RowKey("Row" + rowNum), cells));
                    rowNum++;
                } catch (Exception e) {
                    getLogger().warn("Failed to parse trophy: " + e.getMessage());
                }
            }
        }
        container.close();
        return container.getTable();
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("League", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Country", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Season", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Place", StringCell.TYPE).createSpec()
        );
    }
}
