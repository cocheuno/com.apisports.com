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

        // Fixture selection dropdown
        JPanel fixturePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fixturePanel.add(new JLabel("Fixture:"));
        fixtureCombo = new JComboBox<>();
        fixtureCombo.setPreferredSize(new Dimension(400, 25));
        fixturePanel.add(fixtureCombo);
        mainPanel.add(fixturePanel);

        // Add help text
        JTextArea helpText = new JTextArea(
            "Select a League, Season, and Team to see available fixtures.\n" +
            "Then select a fixture to get match predictions.\n\n" +
            "Note: Only upcoming or recent fixtures will show predictions."
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

        // Set the fixture ID in the combo if it exists
        for (int i = 0; i < fixtureCombo.getItemCount(); i++) {
            String item = fixtureCombo.getItemAt(i);
            if (item != null && item.startsWith(fixtureId + ":")) {
                fixtureCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        // Extract fixture ID from selected item (format: "123: Team A vs Team B")
        String selected = (String) fixtureCombo.getSelectedItem();
        String fixtureId = "";
        if (selected != null && selected.contains(":")) {
            fixtureId = selected.substring(0, selected.indexOf(":"));
        }
        settings.addString(PredictionsNodeModel.CFGKEY_FIXTURE_ID, fixtureId);
    }
}
