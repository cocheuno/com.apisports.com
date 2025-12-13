package com.apisports.knime.football.nodes.predictions;

import com.apisports.knime.port.ApiSportsConnectionPortObject;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
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

import java.io.File;
import java.io.IOException;

public class FootballPredictionsNodeModel extends NodeModel {
    static final String CFGKEY_FIXTURE_ID = "fixtureId";
    
    private final SettingsModelInteger m_fixtureId = new SettingsModelInteger(CFGKEY_FIXTURE_ID, 1);

    protected FootballPredictionsNodeModel() {
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
            new String[]{"Fixture ID", "Home Team", "Away Team", "Winner", "Home Win %", "Draw %", "Away Win %", "Advice"},
            new org.knime.core.data.DataType[]{
                IntCell.TYPE, StringCell.TYPE, StringCell.TYPE, StringCell.TYPE, 
                DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE, StringCell.TYPE
            });
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_fixtureId.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fixtureId.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fixtureId.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionContext exec) throws IOException {
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionContext exec) throws IOException {
    }

    @Override
    protected void reset() {
    }
}
