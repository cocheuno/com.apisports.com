package com.apisports.knime.football.nodes.teams;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;

/**
 * NodeDialog for the Football Teams node.
 * League ID is read from the input table (from Football Leagues node).
 */
public class FootballTeamsNodeDialog extends DefaultNodeSettingsPane {
    protected FootballTeamsNodeDialog() {
        super();

        setDefaultTabTitle("Options");
        createNewTab("Info");
        addDialogComponent(new org.knime.core.node.defaultnodesettings.DialogComponentLabel(
            "This node requires a connection to Football Leagues node.\n\n" +
            "Workflow Pattern:\n" +
            "1. Football Leagues → outputs leagues table\n" +
            "2. (Optional) Row Filter → select one league\n" +
            "3. Football Teams → reads League ID from first row\n\n" +
            "The first row's League ID will be used automatically."));

        selectTab("Options");
        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballTeamsNodeModel.CFGKEY_SEASON, 2024),
            "Season:", 1));
    }
}
