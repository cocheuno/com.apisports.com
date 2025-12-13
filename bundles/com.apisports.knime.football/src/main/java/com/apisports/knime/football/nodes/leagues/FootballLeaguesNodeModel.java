package com.apisports.knime.football.nodes.leagues;

import com.apisports.knime.port.ApiSportsConnectionPortObject;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import java.io.File;
import java.io.IOException;

/**
 * NodeModel for the Football Leagues node.
 */
public class FootballLeaguesNodeModel extends NodeModel {

    static final String CFGKEY_COUNTRY = "country";
    
    private final SettingsModelString m_country = new SettingsModelString(CFGKEY_COUNTRY, "");

    protected FootballLeaguesNodeModel() {
        super(new PortType[]{ApiSportsConnectionPortObject.TYPE}, 
              new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        ApiSportsConnectionPortObject connection = (ApiSportsConnectionPortObject) inObjects[0];
        
        // Create output table spec
        DataTableSpec outputSpec = createOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        
        // Add sample row (real implementation would fetch from API)
        container.addRowToTable(new DefaultRow("Row0", 
            new IntCell(1), 
            new StringCell("Sample League"), 
            new StringCell(m_country.getStringValue()),
            new StringCell("League"),
            new IntCell(2024)));
        
        container.close();
        return new PortObject[]{container.getTable()};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{createOutputSpec()};
    }

    private DataTableSpec createOutputSpec() {
        return new DataTableSpec(
            new String[]{"League ID", "Name", "Country", "Type", "Season"},
            new org.knime.core.data.DataType[]{
                IntCell.TYPE, StringCell.TYPE, StringCell.TYPE, StringCell.TYPE, IntCell.TYPE
            });
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_country.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_country.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_country.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException {
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException {
    }

    @Override
    protected void reset() {
    }
}
