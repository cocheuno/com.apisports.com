package com.apisports.knime.football.nodes.predictions;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;

public class FootballPredictionsNodeDialog extends DefaultNodeSettingsPane {
    protected FootballPredictionsNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballPredictionsNodeModel.CFGKEY_FIXTURE_ID, 1),
            "Fixture ID:", 1));
    }
}
