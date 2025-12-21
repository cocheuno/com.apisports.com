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
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        Map<String, String> params = new HashMap<>();
        if ("player".equals("team") && m_teamId.getIntValue() > 0) {
            params.put("team", String.valueOf(m_teamId.getIntValue()));
        } else if ("player".equals("fixture")) {
            // Requires fixture ID - using league/season as fallback
            params.put("league", String.valueOf(m_leagueId.getIntValue()));
            params.put("season", String.valueOf(m_season.getIntValue()));
        } else if ("player".equals("player") && m_teamId.getIntValue() > 0) {
            params.put("player", String.valueOf(m_teamId.getIntValue()));
        }

        exec.setMessage("Querying transfers from API...");
        JsonNode response = callApi(client, "/transfers", params, mapper);
        return parseSimpleResponse(response, exec);
    }

    private BufferedDataTable parseSimpleResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);
        int rowNum = 0;

        if (response != null && response.isArray()) {
            for (JsonNode item : response) {
                try {
                    // Generic parsing - columns depend on API response
                    List<DataCell> cells = new ArrayList<>();
                    for (int i = 0; i < spec.getNumColumns(); i++) {
                        cells.add(new StringCell(""));  // Placeholder
                    }
                    container.addRowToTable(new DefaultRow(new RowKey("Row" + rowNum), cells.toArray(new DataCell[0])));
                    rowNum++;
                } catch (Exception e) {
                    getLogger().warn("Failed to parse row: " + e.getMessage());
                }
            }
        }
        container.close();
        return container.getTable();
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        List<DataColumnSpec> colSpecs = new ArrayList<>();
        String[] cols = {"['Player_ID:int', 'Player_Name:str', 'Team_In:str', 'Team_Out:str', 'Date:str', 'Type:str']"}.split(", ");
        for (String col : cols) {
            String[] parts = col.split(":");
            if (parts[1].equals("int")) {
                colSpecs.add(new DataColumnSpecCreator(parts[0], IntCell.TYPE).createSpec());
            } else {
                colSpecs.add(new DataColumnSpecCreator(parts[0], StringCell.TYPE).createSpec());
            }
        }
        return new DataTableSpec(colSpecs.toArray(new DataColumnSpec[0]));
    }
}
