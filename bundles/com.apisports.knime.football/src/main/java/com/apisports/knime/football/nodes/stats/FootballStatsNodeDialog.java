package com.apisports.knime.football.nodes.stats;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;

/**
 * NodeDialog for the Football Stats node.
 * Reads team_id, league_id, and season from flow variables.
 */
public class FootballStatsNodeDialog extends DefaultNodeSettingsPane {
    protected FootballStatsNodeDialog() {
        super();

        setDefaultTabTitle("Options");
        createNewTab("Info");
        addDialogComponent(new DialogComponentLabel(
            "This node reads team_id, league_id, and season from flow variables.\n\n" +
            "Workflow Pattern:\n" +
            "1. Football Leagues → Select country/season/league, exports flow variables\n" +
            "2. Football Teams → Reads flow variables, exports team_id\n" +
            "3. Football Stats → Reads all flow variables, outputs team statistics\n\n" +
            "Flow Variables:\n" +
            "- Reads: season, league_id, team_id\n\n" +
            "Override: You can manually enter values below if flow variables are not available."));

        selectTab("Options");
        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballStatsNodeModel.CFGKEY_SEASON, 2024),
            "Season (override):", 1));

        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballStatsNodeModel.CFGKEY_LEAGUE_ID, 0),
            "League ID (override, 0 = use flow var):", 1));

        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballStatsNodeModel.CFGKEY_TEAM_ID, 0),
            "Team ID (override, 0 = use flow var):", 1));
    }
}
