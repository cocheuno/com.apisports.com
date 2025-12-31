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
    protected void validateExecutionSettings() throws InvalidSettingsException {
        // Override parent validation - we don't require league/season/team
        // If "All Teams" is selected, we'll query coaches for each team in the reference data
    }

    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);
        int rowNum = 0;

        if (m_teamId.getIntValue() > 0) {
            // Query coaches for specific team
            System.out.println("Querying /coachs for specific team: " + m_teamId.getIntValue());
            exec.setMessage("Querying coaches for team " + m_teamId.getIntValue());

            Map<String, String> params = new HashMap<>();
            params.put("team", String.valueOf(m_teamId.getIntValue()));

            JsonNode response = callApi(client, "/coachs", params, mapper);
            rowNum = parseResponse(response, container, rowNum);
        } else {
            // Query coaches for all teams in reference data
            if (m_teams == null || m_teams.isEmpty()) {
                throw new Exception("No teams available in reference data. Please load teams first.");
            }

            System.out.println("=============================================================");
            System.out.println("COACHES NODE: Querying coaches for " + m_teams.size() + " teams");
            System.out.println("=============================================================");

            int teamCount = 0;
            for (com.apisports.knime.port.ReferenceData.Team team : m_teams) {
                exec.checkCanceled();
                exec.setProgress((double) teamCount / m_teams.size(),
                    "Querying coaches for team: " + team.getName());

                try {
                    Map<String, String> params = new HashMap<>();
                    params.put("team", String.valueOf(team.getId()));

                    System.out.println("Querying /coachs for team: " + team.getName() + " (ID: " + team.getId() + ")");

                    JsonNode response = callApi(client, "/coachs", params, mapper);
                    rowNum = parseResponse(response, container, rowNum);
                } catch (Exception e) {
                    getLogger().warn("Failed to query coaches for team " + team.getName() + ": " + e.getMessage());
                    System.out.println("  ERROR: " + e.getMessage());
                }

                teamCount++;
            }

            System.out.println("=============================================================");
            System.out.println("COACHES NODE: Retrieved " + rowNum + " total coaches");
            System.out.println("=============================================================");
        }

        container.close();
        return container.getTable();
    }

    private int parseResponse(JsonNode response, BufferedDataContainer container, int startRowNum) {
        int rowNum = startRowNum;

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
        return rowNum;
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
