package com.apisports.knime.connector.nodes.stats;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.port.ApiSportsConnectionPortObject;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.*;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import java.io.File;
import java.io.IOException;

/**
 * NodeModel for the API Statistics node.
 * Displays API usage statistics from the connection.
 */
public class ApiStatisticsNodeModel extends NodeModel {

    protected ApiStatisticsNodeModel() {
        super(
            new PortType[]{ApiSportsConnectionPortObject.TYPE},
            new PortType[]{BufferedDataTable.TYPE}
        );
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        ApiSportsConnectionPortObject connectionPort = (ApiSportsConnectionPortObject) inObjects[0];
        ApiSportsHttpClient client = connectionPort.getClient();

        BufferedDataTable statsTable = createStatisticsTable(exec, client);
        return new PortObject[]{statsTable};
    }

    private BufferedDataTable createStatisticsTable(ExecutionContext exec, ApiSportsHttpClient client) {
        DataTableSpec statsSpec = createStatisticsTableSpec();
        BufferedDataContainer container = exec.createDataContainer(statsSpec);

        // API Calls row
        DataCell[] cells = new DataCell[]{
            new StringCell("API Calls"),
            new IntCell(client.getApiCallCount())
        };
        container.addRowToTable(new DefaultRow(new RowKey("API_Calls"), cells));

        // Cache Hits row
        cells = new DataCell[]{
            new StringCell("Cache Hits"),
            new IntCell(client.getCacheHitCount())
        };
        container.addRowToTable(new DefaultRow(new RowKey("Cache_Hits"), cells));

        // Total Requests row
        cells = new DataCell[]{
            new StringCell("Total Requests"),
            new IntCell(client.getTotalRequestCount())
        };
        container.addRowToTable(new DefaultRow(new RowKey("Total_Requests"), cells));

        container.close();
        return container.getTable();
    }

    private DataTableSpec createStatisticsTableSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Metric", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Value", IntCell.TYPE).createSpec()
        );
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{createStatisticsTableSpec()};
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // No settings
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // No settings
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // No settings
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException {
        // No internals
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException {
        // No internals
    }

    @Override
    protected void reset() {
        // Nothing to reset
    }
}
