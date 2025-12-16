package com.apisports.knime.football.nodes.teams;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;

/**
 * NodeDialog for the Football Teams node.
 * Reads league_id and season from flow variables.
 */
public class FootballTeamsNodeDialog extends DefaultNodeSettingsPane {
    protected FootballTeamsNodeDialog() {
        super();

        setDefaultTabTitle("Options");
        createNewTab("Info");
        addDialogComponent(new DialogComponentLabel(
            "This node reads league_id and season from flow variables (from Football Leagues node).\n\n" +
            "Workflow Pattern:\n" +
            "1. Football Leagues → Select country/season/league, exports flow variables\n" +
            "2. Football Teams → Automatically reads league_id and season from flow variables\n" +
            "3. Football Stats → Automatically reads team_id from flow variable\n\n" +
            "Flow Variables:\n" +
            "- Reads: season, league_id\n" +
            "- Exports: season, league_id, team_id (if selected)\n\n" +
            "Override: You can manually enter values below if flow variables are not available."));

        selectTab("Options");
        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballTeamsNodeModel.CFGKEY_SEASON, 2024),
            "Season (override):", 1));

        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballTeamsNodeModel.CFGKEY_LEAGUE_ID, 0),
            "League ID (override, 0 = use flow var):", 1));

        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballTeamsNodeModel.CFGKEY_SELECTED_TEAM_ID, 0),
            "Selected Team ID (0 = none):", 1));
    }
}
