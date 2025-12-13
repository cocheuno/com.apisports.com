package com.apisports.knime.football.nodes.stats;

import com.apisports.knime.port.ApiSportsConnectionPortObject;
import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import java.io.File;
import java.io.IOException;

public class FootballStatsNodeModel extends NodeModel {
    static final String CFGKEY_TEAM_ID = "teamId";
    static final String CFGKEY_SEASON = "season";
    
    private final SettingsModelInteger m_teamId = new SettingsModelInteger(CFGKEY_TEAM_ID, 1);
    private final SettingsModelInteger m_season = new SettingsModelInteger(CFGKEY_SEASON, 2024);

    protected FootballStatsNodeModel() {
        super(new PortType[]{ApiSportsConnectionPortObject.TYPE}, 
              new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        DataTableSpec outputSpec = createOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        container.close();
        return new PortObject[]{container.getTable()};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{createOutputSpec()};
    }

    private DataTableSpec createOutputSpec() {
        return new DataTableSpec(
            new String[]{"Team", "Matches", "Wins", "Draws", "Losses", "Goals For", "Goals Against"},
            new org.knime.core.data.DataType[]{
                StringCell.TYPE, IntCell.TYPE, IntCell.TYPE, IntCell.TYPE, IntCell.TYPE, IntCell.TYPE, IntCell.TYPE
            });
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_teamId.saveSettingsTo(settings);
        m_season.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_teamId.loadSettingsFrom(settings);
        m_season.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_teamId.validateSettings(settings);
        m_season.validateSettings(settings);
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
