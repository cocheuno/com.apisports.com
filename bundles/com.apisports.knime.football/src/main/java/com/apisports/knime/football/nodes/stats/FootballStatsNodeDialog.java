package com.apisports.knime.football.nodes.stats;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;

/**
 * NodeDialog for the Football Stats node.
 * Team ID and League ID are read from the input table (from Football Teams node).
 */
public class FootballStatsNodeDialog extends DefaultNodeSettingsPane {
    protected FootballStatsNodeDialog() {
        super();

        setDefaultTabTitle("Options");
        createNewTab("Info");
        addDialogComponent(new DialogComponentLabel(
            "This node requires a connection to Football Teams node.\n\n" +
            "Workflow Pattern:\n" +
            "1. Football Leagues → outputs leagues table\n" +
            "2. (Optional) Row Filter → select one league\n" +
            "3. Football Teams → reads League ID, outputs teams table\n" +
            "4. (Optional) Row Filter → select one team\n" +
            "5. Football Stats → reads Team ID & League ID from first row\n\n" +
            "The first row's Team ID and League ID will be used automatically."));

        selectTab("Options");
        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballStatsNodeModel.CFGKEY_SEASON, 2024),
            "Season:", 1));
    }
}
