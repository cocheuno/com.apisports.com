package com.apisports.knime.football.nodes.teams;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;

/**
 * NodeDialog for the Football Teams node.
 */
public class FootballTeamsNodeDialog extends DefaultNodeSettingsPane {
    protected FootballTeamsNodeDialog() {
        super();

        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballTeamsNodeModel.CFGKEY_LEAGUE_ID, 39),
            "League ID:", 1));

        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballTeamsNodeModel.CFGKEY_SEASON, 2024),
            "Season:", 1));
    }
}
