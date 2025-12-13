package com.apisports.knime.connector.nodes;

import com.apisports.knime.core.model.Sport;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.util.Arrays;

/**
 * NodeDialog for the API-Sports Connector node.
 */
public class ApiSportsConnectorNodeDialog extends DefaultNodeSettingsPane {

    protected ApiSportsConnectorNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentPasswordField(
            new SettingsModelString(ApiSportsConnectorNodeModel.CFGKEY_API_KEY, ""),
            "API Key:",
            20));
        
        addDialogComponent(new DialogComponentStringSelection(
            new SettingsModelString(ApiSportsConnectorNodeModel.CFGKEY_SPORT, Sport.FOOTBALL.getId()),
            "Sport:",
            Arrays.stream(Sport.values()).map(Sport::getDisplayName).toArray(String[]::new)));
        
        addDialogComponent(new DialogComponentStringSelection(
            new SettingsModelString(ApiSportsConnectorNodeModel.CFGKEY_TIER, "free"),
            "Tier:",
            "Free", "Basic", "Pro", "Ultra"));
    }
}
