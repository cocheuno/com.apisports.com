/*
 * Copyright 2025 Carone Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apisports.knime.football.nodes.leagues;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * NodeDialog for the Football Leagues node.
 */
public class FootballLeaguesNodeDialog extends DefaultNodeSettingsPane {

    protected FootballLeaguesNodeDialog() {
        super();

        setDefaultTabTitle("Options");
        createNewTab("Info");
        addDialogComponent(new DialogComponentLabel(
            "This node fetches all leagues for a country and pushes flow variables for downstream nodes.\n\n" +
            "Workflow Pattern:\n" +
            "1. Football Leagues → Select country, season, and optionally a league ID\n" +
            "2. Football Teams → Automatically uses league_id flow variable\n" +
            "3. Football Stats → Automatically uses team_id and league_id flow variables\n\n" +
            "Flow Variables Exported:\n" +
            "- season: The season year\n" +
            "- country: The selected country\n" +
            "- league_id: The selected league ID (if specified)"));

        selectTab("Options");
        addDialogComponent(new DialogComponentString(
            new SettingsModelString(FootballLeaguesNodeModel.CFGKEY_COUNTRY, "England"),
            "Country:",
            true,
            20));

        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballLeaguesNodeModel.CFGKEY_SEASON, 2024),
            "Season:", 1));

        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(FootballLeaguesNodeModel.CFGKEY_SELECTED_LEAGUE_ID, 0),
            "Selected League ID (0 = none):", 1));
    }
}
