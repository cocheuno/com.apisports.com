package com.apisports.knime.football.nodes.query.teams;

import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeDialog;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for Teams query node.
 */
public class TeamsNodeDialog extends AbstractFootballQueryNodeDialog {

    private JCheckBox includeStatisticsCheck;
    private JTextField teamNameField;

    public TeamsNodeDialog() {
        super();
        addTeamsSpecificComponents();
    }

    private void addTeamsSpecificComponents() {
        // Add separator
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        mainPanel.add(Box.createVerticalStrut(10));

        // Team name search (optional)
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.add(new JLabel("Team Name Search (optional):"));
        teamNameField = new JTextField(25);
        namePanel.add(teamNameField);
        mainPanel.add(namePanel);

        // Include statistics option
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        includeStatisticsCheck = new JCheckBox("Include Team Statistics (wins, losses, goals)");
        statsPanel.add(includeStatisticsCheck);
        mainPanel.add(statsPanel);

        // Add warning about statistics
        JTextArea warningText = new JTextArea(
            "Note: Including statistics will make additional API calls (one per team) " +
            "and may significantly increase execution time for queries with many teams."
        );
        warningText.setEditable(false);
        warningText.setWrapStyleWord(true);
        warningText.setLineWrap(true);
        warningText.setBackground(mainPanel.getBackground());
        warningText.setForeground(Color.RED.darker());
        warningText.setFont(new Font("SansSerif", Font.ITALIC, 10));
        mainPanel.add(warningText);

        mainPanel.add(Box.createVerticalStrut(10));

        // Add help text
        JTextArea helpText = new JTextArea(
            "Teams Node retrieves team information for a league and season.\n\n" +
            "Basic info includes: name, code, country, founded year, venue details.\n" +
            "With statistics: adds wins, draws, losses, goals for/against."
        );
        helpText.setEditable(false);
        helpText.setWrapStyleWord(true);
        helpText.setLineWrap(true);
        helpText.setBackground(mainPanel.getBackground());
        helpText.setFont(new Font("SansSerif", Font.ITALIC, 10));
        mainPanel.add(helpText);
    }

    @Override
    protected void loadAdditionalSettings(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {
        boolean includeStats = settings.getBoolean(TeamsNodeModel.CFGKEY_INCLUDE_STATISTICS, false);
        String teamName = settings.getString(TeamsNodeModel.CFGKEY_TEAM_NAME, "");

        includeStatisticsCheck.setSelected(includeStats);
        teamNameField.setText(teamName);
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        settings.addBoolean(TeamsNodeModel.CFGKEY_INCLUDE_STATISTICS, includeStatisticsCheck.isSelected());
        settings.addString(TeamsNodeModel.CFGKEY_TEAM_NAME, teamNameField.getText());
    }
}
