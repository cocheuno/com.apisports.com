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

package com.apisports.knime.football.nodes.query.predictions;

import org.knime.core.node.*;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for Predictions node.
 *
 * This node has no configuration - it processes all fixtures from the input table.
 * The dialog just shows instructions for the user.
 */
public class PredictionsNodeDialog extends NodeDialogPane {

    public PredictionsNodeDialog() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("Predictions Node");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(15));

        // Instructions
        JTextArea instructions = new JTextArea(
            "This node queries match predictions for all fixtures from the connected Fixtures node.\n\n" +
            "Workflow:\n" +
            "1. Configure and execute a Fixtures node to get fixtures for your League/Season/Team\n" +
            "2. Connect the Fixtures node output to this node's second input port (triangle port)\n" +
            "3. Execute this node to get predictions for all fixtures in the input table\n\n" +
            "The node will automatically:\n" +
            "• Extract all Fixture IDs from the input table\n" +
            "• Query predictions for each fixture\n" +
            "• Combine results into a single output table\n\n" +
            "Note: Only upcoming or recent fixtures will have predictions available.\n" +
            "Fixtures without predictions will be logged as warnings but won't fail execution."
        );
        instructions.setEditable(false);
        instructions.setWrapStyleWord(true);
        instructions.setLineWrap(true);
        instructions.setBackground(mainPanel.getBackground());
        instructions.setFont(new Font("SansSerif", Font.PLAIN, 12));
        instructions.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(instructions);

        addTab("Configuration", new JScrollPane(mainPanel));
    }

    @Override
    protected void loadSettingsFrom(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {
        // No settings to load
    }

    @Override
    protected void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
        // No settings to save
    }
}
