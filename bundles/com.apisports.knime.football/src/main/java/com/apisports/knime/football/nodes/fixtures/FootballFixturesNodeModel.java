package com.apisports.knime.football.nodes.fixtures;

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

public class FootballFixturesNodeModel extends NodeModel {
    static final String CFGKEY_LEAGUE_ID = "leagueId";
    static final String CFGKEY_SEASON = "season";
    
    private final SettingsModelInteger m_leagueId = new SettingsModelInteger(CFGKEY_LEAGUE_ID, 1);
    private final SettingsModelInteger m_season = new SettingsModelInteger(CFGKEY_SEASON, 2024);

    protected FootballFixturesNodeModel() {
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
            new String[]{"Fixture ID", "Home Team", "Away Team", "Date", "Status"},
            new org.knime.core.data.DataType[]{
                IntCell.TYPE, StringCell.TYPE, StringCell.TYPE, StringCell.TYPE, StringCell.TYPE
            });
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_leagueId.saveSettingsTo(settings);
        m_season.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_leagueId.loadSettingsFrom(settings);
        m_season.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_leagueId.validateSettings(settings);
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
