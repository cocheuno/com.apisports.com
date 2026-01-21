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

package com.apisports.knime.connector.nodes;

import com.apisports.knime.core.model.Sport;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.util.Arrays;

/**
 * NodeDialog for the API-Sports Connector node.
 */
public class ApiSportsConnectorNodeDialog extends DefaultNodeSettingsPane {

    protected ApiSportsConnectorNodeDialog() {
        super();

        addDialogComponent(new DialogComponentString(
            new SettingsModelString(ApiSportsConnectorNodeModel.CFGKEY_API_KEY, ""),
            "API Key:",
            true,
            50));
        
        addDialogComponent(new DialogComponentStringSelection(
            new SettingsModelString(ApiSportsConnectorNodeModel.CFGKEY_SPORT, Sport.FOOTBALL.getDisplayName()),
            "Sport:",
            Arrays.stream(Sport.values()).map(Sport::getDisplayName).toArray(String[]::new)));
    }
}
