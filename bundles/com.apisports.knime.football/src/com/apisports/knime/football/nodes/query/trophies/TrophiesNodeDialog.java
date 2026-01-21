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

package com.apisports.knime.football.nodes.query.trophies;

import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * Node dialog for Trophies node.
 *
 * This node has no configuration options - it automatically processes
 * player or coach IDs from the connected input node.
 */
public class TrophiesNodeDialog extends DefaultNodeSettingsPane {
    public TrophiesNodeDialog() {
        super();
        // No configuration needed - node auto-configures based on input
        addDialogComponent(new org.knime.core.node.defaultnodesettings.DialogComponentLabel(
            "This node automatically queries trophies for all players or coaches " +
            "from the connected input node.\n\n" +
            "Connect a Players or Coaches node to the third input port.\n\n" +
            "The node will detect whether the input is from Players or Coaches " +
            "and query the appropriate trophies."
        ));
    }
}
