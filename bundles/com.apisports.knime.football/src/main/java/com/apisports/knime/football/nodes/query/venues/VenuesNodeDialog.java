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

package com.apisports.knime.football.nodes.query.venues;

import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeDialog;
import org.knime.core.node.*;
import org.knime.core.node.port.PortObjectSpec;
import javax.swing.*;
import java.awt.*;

public class VenuesNodeDialog extends AbstractFootballQueryNodeDialog {
    private JTextField venueNameField;
    private JTextField cityField;

    public VenuesNodeDialog() {
        super();
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.add(new JLabel("Venue Name:"));
        venueNameField = new JTextField(20);
        namePanel.add(venueNameField);
        mainPanel.add(namePanel);

        JPanel cityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cityPanel.add(new JLabel("City:"));
        cityField = new JTextField(20);
        cityPanel.add(cityField);
        mainPanel.add(cityPanel);
    }

    @Override
    protected void loadAdditionalSettings(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {
        venueNameField.setText(settings.getString(VenuesNodeModel.CFGKEY_VENUE_NAME, ""));
        cityField.setText(settings.getString(VenuesNodeModel.CFGKEY_CITY, ""));
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        settings.addString(VenuesNodeModel.CFGKEY_VENUE_NAME, venueNameField.getText());
        settings.addString(VenuesNodeModel.CFGKEY_CITY, cityField.getText());
    }
}
