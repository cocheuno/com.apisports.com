package com.apisports.knime.football.nodes.stats;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;

public class FootballStatsNodeDialog extends DefaultNodeSettingsPane {
    protected FootballStatsNodeDialog() {
        super();

        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballStatsNodeModel.CFGKEY_LEAGUE_ID, 39),
            "League ID:", 1));

        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballStatsNodeModel.CFGKEY_TEAM_ID, 1),
            "Team ID:", 1));

        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballStatsNodeModel.CFGKEY_SEASON, 2024),
            "Season:", 1));
    }
}
