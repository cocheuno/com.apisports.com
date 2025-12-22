package com.apisports.knime.football.nodes.query.predictions;

import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeDialog;
import org.knime.core.node.*;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.*;
import java.awt.*;

public class PredictionsNodeDialog extends AbstractFootballQueryNodeDialog {

    private JComboBox<String> fixtureCombo;

    public PredictionsNodeDialog() {
        super();
        addPredictionsSpecificComponents();
    }

    private void addPredictionsSpecificComponents() {
        // Add separator
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        mainPanel.add(Box.createVerticalStrut(10));

        // Fixture selection dropdown (editable for manual input)
        JPanel fixturePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fixturePanel.add(new JLabel("Fixture ID:"));
        fixtureCombo = new JComboBox<>();
        fixtureCombo.setEditable(true); // Allow users to type fixture ID directly
        fixtureCombo.setPreferredSize(new Dimension(400, 25));
        fixturePanel.add(fixtureCombo);
        mainPanel.add(fixturePanel);

        // Add help text
        JTextArea helpText = new JTextArea(
            "Workflow:\n" +
            "1. Configure and execute a Fixtures node for your desired League/Season/Team\n" +
            "2. Look at the Fixture_ID column in the Fixtures output to find fixtures\n" +
            "3. Enter the Fixture ID above\n" +
            "4. Execute this node to get predictions for that fixture\n\n" +
            "Note: Only upcoming or recent fixtures will have predictions available."
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
        // Restore saved fixture ID in the editable combo
        String savedFixtureId = settings.getString(PredictionsNodeModel.CFGKEY_FIXTURE_ID, "");
        if (!savedFixtureId.isEmpty()) {
            fixtureCombo.getEditor().setItem(savedFixtureId);
        }
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        // Get fixture ID from the editable combo box
        Object selected = fixtureCombo.getEditor().getItem();
        String fixtureId = selected != null ? selected.toString().trim() : "";
        settings.addString(PredictionsNodeModel.CFGKEY_FIXTURE_ID, fixtureId);
    }
}
