package com.apisports.knime.football.nodes.universal;

import com.apisports.knime.core.descriptor.DescriptorRegistry;
import com.apisports.knime.core.descriptor.EndpointDescriptor;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Universal Node Dialog - prototype implementation.
 * Allows selection of endpoint and entering parameters as JSON.
 */
public class UniversalNodeDialog extends DefaultNodeSettingsPane {

    protected UniversalNodeDialog() {
        super();

        // Load descriptors
        List<String> endpointIds = new ArrayList<>();
        try {
            DescriptorRegistry registry = DescriptorRegistry.getInstance();
            if (registry.getAllDescriptors().isEmpty()) {
                registry.loadFromResource("/descriptors/football-endpoints.yaml");
            }

            endpointIds = registry.getAllDescriptors()
                .stream()
                .map(EndpointDescriptor::getId)
                .sorted()
                .collect(Collectors.toList());

            if (endpointIds.isEmpty()) {
                endpointIds.add("<no endpoints loaded>");
            }
        } catch (Exception e) {
            endpointIds.add("<error loading endpoints>");
            // Print to console for debugging
            e.printStackTrace();
        }

        // Add endpoint selector
        addDialogComponent(new DialogComponentStringSelection(
            new SettingsModelString(UniversalNodeModel.CFGKEY_ENDPOINT_ID,
                endpointIds.isEmpty() ? "" : endpointIds.get(0)),
            "Endpoint:",
            endpointIds));

        // Add parameters input
        addDialogComponent(new DialogComponentMultiLineString(
            new SettingsModelString(UniversalNodeModel.CFGKEY_PARAMETERS, "{}"),
            "Parameters (JSON):",
            true,
            60,
            10));

        // Add help text
        addDialogComponent(new DialogComponentLabel(
            "Enter parameters as JSON. Example: {\"league\":39, \"season\":2024}\n" +
            "Available endpoints: leagues_all, fixtures_by_league, teams_by_league, standings_by_league"));
    }
}
