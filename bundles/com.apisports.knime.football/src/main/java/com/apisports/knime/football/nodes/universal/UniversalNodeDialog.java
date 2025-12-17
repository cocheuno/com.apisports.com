package com.apisports.knime.football.nodes.universal;

import com.apisports.knime.core.descriptor.DescriptorRegistry;
import com.apisports.knime.core.descriptor.EndpointDescriptor;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Universal Node Dialog - prototype implementation.
 * Allows selection of endpoint and entering parameters as JSON.
 */
public class UniversalNodeDialog extends DefaultNodeSettingsPane {

    protected UniversalNodeDialog() {
        super();

        // Load descriptors if not already loaded
        try {
            DescriptorRegistry registry = DescriptorRegistry.getInstance();
            if (registry.getAllDescriptors().isEmpty()) {
                registry.loadFromResource("/descriptors/football-endpoints.yaml");
            }
        } catch (Exception e) {
            // Log error but don't fail dialog creation
            System.err.println("Failed to load descriptors: " + e.getMessage());
        }

        setDefaultTabTitle("Configuration");

        // Info tab
        createNewTab("Info");
        addDialogComponent(new DialogComponentLabel(
            "Universal API Request Node (Prototype)\n\n" +
            "This node dynamically executes any API endpoint defined in the descriptor registry.\n\n" +
            "1. Select an endpoint from the dropdown\n" +
            "2. Enter parameters as JSON (e.g., {\"league\":39, \"season\":2024})\n" +
            "3. Execute to fetch data\n\n" +
            "Available endpoints are loaded from descriptors/football-endpoints.yaml"));

        // Configuration tab
        selectTab("Configuration");

        // Endpoint selector
        List<String> endpointIds = DescriptorRegistry.getInstance()
            .getAllDescriptors()
            .stream()
            .map(EndpointDescriptor::getId)
            .sorted()
            .collect(Collectors.toList());

        if (endpointIds.isEmpty()) {
            endpointIds.add("<no endpoints loaded>");
        }

        addDialogComponent(new DialogComponentStringSelection(
            new SettingsModelString(UniversalNodeModel.CFGKEY_ENDPOINT_ID, ""),
            "Endpoint:",
            endpointIds));

        // Parameters (JSON format for prototype)
        addDialogComponent(new DialogComponentMultiLineString(
            new SettingsModelString(UniversalNodeModel.CFGKEY_PARAMETERS, "{}"),
            "Parameters (JSON):",
            true,
            60,
            10));

        // Endpoint details
        createNewTab("Selected Endpoint Details");
        addDialogComponent(new DialogComponentLabel(
            "Endpoint details will be shown here after selection.\n" +
            "Future enhancement: Dynamic parameter UI based on descriptor."));
    }
}
