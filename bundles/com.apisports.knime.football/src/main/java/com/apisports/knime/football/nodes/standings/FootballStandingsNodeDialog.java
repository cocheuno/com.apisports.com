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
