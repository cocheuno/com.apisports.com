package com.apisports.knime.football.nodes.fixtures;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;

public class FootballFixturesNodeDialog extends DefaultNodeSettingsPane {
    protected FootballFixturesNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballFixturesNodeModel.CFGKEY_LEAGUE_ID, 1),
            "League ID:", 1));
        
        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballFixturesNodeModel.CFGKEY_SEASON, 2024),
            "Season:", 1));
    }
}
