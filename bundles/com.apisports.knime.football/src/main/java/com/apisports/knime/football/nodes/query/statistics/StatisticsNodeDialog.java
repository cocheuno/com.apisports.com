package com.apisports.knime.football.nodes.query.statistics;

import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeDialog;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.*;
import java.awt.*;

public class StatisticsNodeDialog extends AbstractFootballQueryNodeDialog {

    private JTextField fixtureIdField;

    public StatisticsNodeDialog() {
        super();
        addStatisticsSpecificComponents();
    }

    private void addStatisticsSpecificComponents() {
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        mainPanel.add(Box.createVerticalStrut(10));

        JPanel fixturePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fixturePanel.add(new JLabel("Fixture ID:"));
        fixtureIdField = new JTextField(15);
        fixturePanel.add(fixtureIdField);
        mainPanel.add(fixturePanel);

        JTextArea helpText = new JTextArea(
            "Retrieves detailed match statistics for a specific fixture.\n\n" +
            "Statistics include: shots, possession, passes, fouls, corners, etc."
        );
        helpText.setEditable(false);
        helpText.setWrapStyleWord(true);
        helpText.setLineWrap(true);
        helpText.setBackground(mainPanel.getBackground());
        helpText.setFont(new Font("SansSerif", Font.ITALIC, 10));
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(helpText);

        leagueCombo.setEnabled(false);
        seasonCombo.setEnabled(false);
        teamCombo.setEnabled(false);
    }

    @Override
    protected void loadAdditionalSettings(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {
        String fixtureId = settings.getString(StatisticsNodeModel.CFGKEY_FIXTURE_ID, "");
        fixtureIdField.setText(fixtureId);
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        settings.addString(StatisticsNodeModel.CFGKEY_FIXTURE_ID, fixtureIdField.getText());
    }
}
