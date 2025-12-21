package com.apisports.knime.football.nodes.query.statistics;

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
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.util.HashMap;
import java.util.Map;

/**
 * NodeModel for Fixture Statistics query node.
 */
public class StatisticsNodeModel extends AbstractFootballQueryNodeModel {

    static final String CFGKEY_FIXTURE_ID = "fixtureId";

    protected final SettingsModelString m_fixtureId =
        new SettingsModelString(CFGKEY_FIXTURE_ID, "");

    @Override
    protected void validateExecutionSettings() throws InvalidSettingsException {
        if (m_fixtureId.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("Please specify a fixture ID");
        }
    }

    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("fixture", m_fixtureId.getStringValue());

        exec.setMessage("Querying fixture statistics from API...");
        JsonNode response = callApi(client, "/fixtures/statistics", params, mapper);

        exec.setMessage("Parsing results...");
        BufferedDataTable result = parseStatisticsResponse(response, exec);

        getLogger().info("Retrieved statistics for fixture " + m_fixtureId.getStringValue());
        return result;
    }

    private BufferedDataTable parseStatisticsResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);

        if (response != null && response.isArray()) {
            int rowNum = 0;

            for (JsonNode teamStats : response) {
                JsonNode team = teamStats.get("team");
                JsonNode statistics = teamStats.get("statistics");

                int teamId = team != null && team.has("id") ? team.get("id").asInt() : 0;
                String teamName = team != null && team.has("name") ? team.get("name").asText() : "";

                if (statistics != null && statistics.isArray()) {
                    for (JsonNode stat : statistics) {
                        try {
                            String type = stat.has("type") ? stat.get("type").asText() : "";
                            String value = stat.has("value") && !stat.get("value").isNull()
                                ? stat.get("value").toString() : "";

                            DataCell[] cells = new DataCell[]{
                                new IntCell(teamId),
                                new StringCell(teamName),
                                new StringCell(type),
                                new StringCell(value)
                            };

                            container.addRowToTable(new DefaultRow(new RowKey("Row" + rowNum), cells));
                            rowNum++;
                        } catch (Exception e) {
                            getLogger().warn("Failed to parse statistic: " + e.getMessage());
                        }
                    }
                }
            }
        }

        container.close();
        return container.getTable();
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Team_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Team_Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Statistic_Type", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Value", StringCell.TYPE).createSpec()
        );
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_fixtureId.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_fixtureId.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_fixtureId.loadSettingsFrom(settings);
    }
}
