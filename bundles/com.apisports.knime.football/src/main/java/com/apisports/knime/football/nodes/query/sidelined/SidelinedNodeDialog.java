package com.apisports.knime.football.nodes.query.sidelined;

import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeDialog;
import org.knime.core.node.*;
import org.knime.core.node.port.PortObjectSpec;

public class SidelinedNodeDialog extends AbstractFootballQueryNodeDialog {
    public SidelinedNodeDialog() {
        super();
    }

    @Override
    protected void loadAdditionalSettings(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
    }
}
