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

package com.apisports.knime.football.nodes.query.sidelined;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.*;
import java.util.*;

public class SidelinedNodeModel extends AbstractFootballQueryNodeModel {
    @Override
    protected void validateExecutionSettings() throws InvalidSettingsException {
        if (m_teamId.getIntValue() <= 0) {
            throw new InvalidSettingsException("Please select a team (used as player ID for sidelined)");
        }
    }

    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("player", String.valueOf(m_teamId.getIntValue()));

        exec.setMessage("Querying sidelined from API...");
        JsonNode response = callApi(client, "/sidelined", params, mapper);
        return parseResponse(response, exec);
    }

    private BufferedDataTable parseResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);
        int rowNum = 0;

        if (response != null && response.isArray()) {
            for (JsonNode item : response) {
                try {
                    String type = item.has("type") ? item.get("type").asText() : "";
                    String start = item.has("start") ? item.get("start").asText() : "";
                    String end = item.has("end") ? item.get("end").asText() : "";

                    DataCell[] cells = new DataCell[]{
                        new StringCell(type),
                        new StringCell(start),
                        new StringCell(end)
                    };
                    container.addRowToTable(new DefaultRow(new RowKey("Row" + rowNum), cells));
                    rowNum++;
                } catch (Exception e) {
                    getLogger().warn("Failed to parse sidelined record: " + e.getMessage());
                }
            }
        }
        container.close();
        return container.getTable();
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Type", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Start_Date", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("End_Date", StringCell.TYPE).createSpec()
        );
    }
}
