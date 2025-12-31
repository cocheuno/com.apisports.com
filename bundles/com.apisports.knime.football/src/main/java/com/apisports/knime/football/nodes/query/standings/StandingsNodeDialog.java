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

package com.apisports.knime.football.nodes.query.standings;

import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeDialog;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for Standings query node.
 * Very simple - only needs league and season selection (provided by base class).
 */
public class StandingsNodeDialog extends AbstractFootballQueryNodeDialog {

    public StandingsNodeDialog() {
        super();
        addStandingsSpecificComponents();
    }

    private void addStandingsSpecificComponents() {
        // Add info text
        JTextArea infoText = new JTextArea(
            "Standings Node retrieves the league table/rankings for the selected league and season.\n\n" +
            "Output includes:\n" +
            "• Team rankings and points\n" +
            "• Matches played, won, drawn, lost\n" +
            "• Goals for, against, and goal difference\n" +
            "• Recent form (W/D/L sequence)\n" +
            "• Position description (e.g., Champions League, Relegation)"
        );
        infoText.setEditable(false);
        infoText.setWrapStyleWord(true);
        infoText.setLineWrap(true);
        infoText.setBackground(mainPanel.getBackground());
        infoText.setFont(new Font("SansSerif", Font.ITALIC, 10));
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(infoText);

        // Disable team selection (not applicable for standings)
        teamCombo.setEnabled(false);
        teamOptionalCheckbox.setVisible(false);
    }

    @Override
    protected void loadAdditionalSettings(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {
        // No additional settings for standings node
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        // No additional settings for standings node
    }
}
