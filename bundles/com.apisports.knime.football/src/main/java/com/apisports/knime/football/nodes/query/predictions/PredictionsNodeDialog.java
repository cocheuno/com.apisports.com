package com.apisports.knime.football.nodes.query.predictions;

import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeDialog;
import org.knime.core.node.*;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.*;
import java.awt.*;

public class PredictionsNodeDialog extends AbstractFootballQueryNodeDialog {

    private JTextField fixtureIdField;

    public PredictionsNodeDialog() {
        super();
        addPredictionsSpecificComponents();

        // Hide league/season/team controls since predictions work per-fixture
        leagueCombo.setEnabled(false);
        seasonCombo.setEnabled(false);
        teamCombo.setEnabled(false);
        teamOptionalCheckbox.setVisible(false);
    }

    private void addPredictionsSpecificComponents() {
        // Add separator
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        mainPanel.add(Box.createVerticalStrut(10));

        // Fixture ID input
        JPanel fixtureIdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fixtureIdPanel.add(new JLabel("Fixture ID:"));
        fixtureIdField = new JTextField(15);
        fixtureIdPanel.add(fixtureIdField);
        mainPanel.add(fixtureIdPanel);

        // Add help text
        JTextArea helpText = new JTextArea(
            "Enter a Fixture ID to get match predictions.\n\n" +
            "To find fixture IDs, use the Fixtures node to query upcoming matches.\n" +
            "The Fixture_ID column from the Fixtures node output can be used here."
        );
        helpText.setEditable(false);
        helpText.setWrapStyleWord(true);
        helpText.setLineWrap(true);
        helpText.setBackground(mainPanel.getBackground());
        helpText.setFont(new Font("SansSerif", Font.ITALIC, 10));
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(helpText);
    }

    @Override
    protected void loadAdditionalSettings(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {
        String fixtureId = settings.getString(PredictionsNodeModel.CFGKEY_FIXTURE_ID, "");
        fixtureIdField.setText(fixtureId);
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        settings.addString(PredictionsNodeModel.CFGKEY_FIXTURE_ID, fixtureIdField.getText());
    }
}
