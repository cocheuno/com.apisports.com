package com.apisports.knime.football.nodes.leagues;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * NodeDialog for the Football Leagues node.
 */
public class FootballLeaguesNodeDialog extends DefaultNodeSettingsPane {

    protected FootballLeaguesNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentString(
            new SettingsModelString(FootballLeaguesNodeModel.CFGKEY_COUNTRY, ""),
            "Country (optional):",
            true,
            20));
    }
}
