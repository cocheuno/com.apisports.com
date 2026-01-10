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

package com.apisports.knime.football.nodes.query.players;

import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeDialog;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for Player Stats node.
 */
public class PlayerStatsNodeDialog extends AbstractFootballQueryNodeDialog {

    private JTextField fixtureIdField;

    public PlayerStatsNodeDialog() {
        super();
        addPlayerStatsSpecificComponents();
    }

    private void addPlayerStatsSpecificComponents() {
        // Add separator
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        mainPanel.add(Box.createVerticalStrut(10));

        // Fixture ID field
        JPanel fixtureIdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fixtureIdPanel.add(new JLabel("Fixture ID:"));
        fixtureIdField = new JTextField(20);
        fixtureIdPanel.add(fixtureIdField);
        fixtureIdPanel.add(new JLabel("(Optional if input port connected)"));
        mainPanel.add(fixtureIdPanel);

        // Add help text
        JTextArea helpText = new JTextArea(
            "Player Stats - Detailed match-by-match player performance statistics\n\n" +
            "Usage Modes:\n" +
            "• Standalone: Enter fixture ID(s) to get player stats for specific match(es)\n" +
            "• With Fixture IDs Input: Connect Fixtures Selector output (after filtering)\n" +
            "  to retrieve player stats for all selected fixtures\n\n" +
            "Returns 28 columns per player including:\n" +
            "• Game info: Minutes, Position, Rating, Captain/Substitute status\n" +
            "• Performance: Goals, Assists, Shots, Passes, Tackles, Dribbles\n" +
            "• Discipline: Fouls, Yellow/Red Cards\n\n" +
            "Typical workflow:\n" +
            "1. Use Fixtures Selector to browse fixtures\n" +
            "2. Filter to interesting matches (e.g., specific team, date range)\n" +
            "3. Connect filtered output to this node's optional input port\n" +
            "4. Retrieve detailed player statistics for only those matches"
        );
        helpText.setEditable(false);
        helpText.setWrapStyleWord(true);
        helpText.setLineWrap(true);
        helpText.setBackground(mainPanel.getBackground());
        helpText.setFont(new Font("SansSerif", Font.ITALIC, 10));
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(helpText);

        // Hide confusing elements
        leagueCombo.setEnabled(false);
        seasonCombo.setEnabled(false);
        teamCombo.setEnabled(false);
    }

    @Override
    protected void loadAdditionalSettings(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {
        String fixtureId = settings.getString(PlayerStatsNodeModel.CFGKEY_FIXTURE_ID, "");
        fixtureIdField.setText(fixtureId);
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        settings.addString(PlayerStatsNodeModel.CFGKEY_FIXTURE_ID, fixtureIdField.getText());
    }
}
