package com.apisports.knime.football.nodes.standings;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;

public class FootballStandingsNodeDialog extends DefaultNodeSettingsPane {
    protected FootballStandingsNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballStandingsNodeModel.CFGKEY_LEAGUE_ID, 1),
            "League ID:", 1));
        
        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballStandingsNodeModel.CFGKEY_SEASON, 2024),
            "Season:", 1));
    }
}
