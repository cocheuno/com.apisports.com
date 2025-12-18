package com.apisports.knime.football.nodes.teamstats;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.port.ApiSportsConnectionPortObject;
import com.apisports.knime.port.ReferenceDataPortObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * NodeModel for Team Statistics query node.
 * Takes reference data and API connection, outputs team statistics.
 */
public class TeamStatisticsNodeModel extends NodeModel {

    static final String CFGKEY_LEAGUE = "league";
    static final String CFGKEY_SEASON = "season";
    static final String CFGKEY_TEAM = "team";

    private final SettingsModelInteger m_league =
        new SettingsModelInteger(CFGKEY_LEAGUE, 39); // Default to Premier League
    private final SettingsModelInteger m_season =
        new SettingsModelInteger(CFGKEY_SEASON, 2024);
    private final SettingsModelInteger m_team =
        new SettingsModelInteger(CFGKEY_TEAM, 42); // Default to Arsenal

    protected TeamStatisticsNodeModel() {
        super(new PortType[]{ReferenceDataPortObject.TYPE, ApiSportsConnectionPortObject.TYPE},
              new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        // Get reference data (not used in execution, but available for validation)
        ReferenceDataPortObject refDataPort = (ReferenceDataPortObject) inObjects[0];

        // Get API client
        ApiSportsConnectionPortObject connectionPort = (ApiSportsConnectionPortObject) inObjects[1];
        ApiSportsHttpClient client = connectionPort.getClient();

        // Build parameters
        Map<String, String> params = new HashMap<>();
        params.put("league", String.valueOf(m_league.getIntValue()));
        params.put("season", String.valueOf(m_season.getIntValue()));
        params.put("team", String.valueOf(m_team.getIntValue()));

        // Execute API call
        exec.setMessage("Fetching team statistics...");
        String responseBody = client.get("/teams/statistics", params);

        // Parse JSON response
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(responseBody);
        JsonNode data = root.get("response");

        // Convert to table
        BufferedDataTable outputTable = jsonObjectToTable(data, exec);

        return new PortObject[]{outputTable};
    }

    /**
     * Convert a single JSON object to a one-row table.
     */
    private BufferedDataTable jsonObjectToTable(JsonNode data, ExecutionContext exec) throws Exception {
        if (data == null || !data.isObject()) {
            // Return empty table
            DataTableSpec emptySpec = new DataTableSpec(
                new DataColumnSpecCreator("_empty", StringCell.TYPE).createSpec());
            BufferedDataContainer container = exec.createDataContainer(emptySpec);
            container.close();
            return container.getTable();
        }

        List<DataColumnSpec> columnSpecs = new ArrayList<>();
        List<DataCell> cells = new ArrayList<>();

        // Flatten the object
        Iterator<String> fieldNames = data.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode fieldValue = data.get(fieldName);

            // Determine type and create cell
            if (fieldValue.isInt()) {
                columnSpecs.add(new DataColumnSpecCreator(fieldName, IntCell.TYPE).createSpec());
                cells.add(new IntCell(fieldValue.asInt()));
            } else if (fieldValue.isObject() || fieldValue.isArray()) {
                // Convert complex types to JSON string
                columnSpecs.add(new DataColumnSpecCreator(fieldName, StringCell.TYPE).createSpec());
                cells.add(new StringCell(fieldValue.toString()));
            } else if (fieldValue.isNull()) {
                columnSpecs.add(new DataColumnSpecCreator(fieldName, StringCell.TYPE).createSpec());
                cells.add(StringCell.TYPE.getMissingCell());
            } else {
                columnSpecs.add(new DataColumnSpecCreator(fieldName, StringCell.TYPE).createSpec());
                cells.add(new StringCell(fieldValue.asText()));
            }
        }

        // Create table
        DataTableSpec outputSpec = new DataTableSpec(columnSpecs.toArray(new DataColumnSpec[0]));
        BufferedDataContainer container = exec.createDataContainer(outputSpec);

        // Add single row
        container.addRowToTable(new DefaultRow("Row0", cells.toArray(new DataCell[0])));

        container.close();
        return container.getTable();
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // Cannot determine output spec without execution
        return new PortObjectSpec[]{null};
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_league.saveSettingsTo(settings);
        m_season.saveSettingsTo(settings);
        m_team.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_league.validateSettings(settings);
        m_season.validateSettings(settings);
        m_team.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_league.loadSettingsFrom(settings);
        m_season.loadSettingsFrom(settings);
        m_team.loadSettingsFrom(settings);
    }

    @Override
    protected void reset() {
        // Nothing to reset
    }

    @Override
    protected void loadInternals(final java.io.File nodeInternDir,
                                 final org.knime.core.node.ExecutionMonitor exec)
            throws java.io.IOException, org.knime.core.node.CanceledExecutionException {
        // No internal state to load
    }

    @Override
    protected void saveInternals(final java.io.File nodeInternDir,
                                 final org.knime.core.node.ExecutionMonitor exec)
            throws java.io.IOException, org.knime.core.node.CanceledExecutionException {
        // No internal state to save
    }
}
