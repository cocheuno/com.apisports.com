package com.apisports.knime.football.nodes.universal;

import com.apisports.knime.core.dao.ReferenceDAO;
import com.apisports.knime.core.descriptor.DescriptorRegistry;
import com.apisports.knime.core.descriptor.EndpointDescriptor;
import com.apisports.knime.port.ReferenceData;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerArray;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Universal Node Dialog with simplified dropdown interface.
 *
 * Note: For full cascading dropdown functionality (where League selection
 * filters Seasons and Teams), a custom dialog with Swing listeners would be required.
 * This implementation provides a functional interface where users can enter IDs manually
 * or use the JSON parameters field for additional flexibility.
 */
public class UniversalNodeDialog extends DefaultNodeSettingsPane {

    protected UniversalNodeDialog() {
        super();

        createNewGroup("Endpoint Selection");

        // Load descriptors
        List<String> endpointIds = new ArrayList<>();
        String errorMessage = "";
        try {
            DescriptorRegistry registry = DescriptorRegistry.getInstance();
            if (registry.getAllDescriptors().isEmpty()) {
                // Load from this bundle's resources (football bundle, not core)
                java.io.InputStream stream = getClass().getResourceAsStream("/descriptors/football-endpoints.yaml");
                if (stream == null) {
                    throw new Exception("Descriptor file not found in football bundle");
                }
                registry.loadFromStream(stream);
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
            errorMessage = "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            endpointIds.add("<error loading endpoints>");
            // Print detailed stack trace to console for debugging
            System.err.println("============================================");
            System.err.println("Failed to load endpoint descriptors:");
            System.err.println("============================================");
            e.printStackTrace();
            System.err.println("============================================");
        }

        // Add endpoint selector
        addDialogComponent(new DialogComponentStringSelection(
            new SettingsModelString(UniversalNodeModel.CFGKEY_ENDPOINT_ID,
                endpointIds.isEmpty() ? "" : endpointIds.get(0)),
            "Endpoint:",
            endpointIds));

        closeCurrentGroup();

        createNewGroup("Reference Data Selections");

        addDialogComponent(new DialogComponentLabel(
            "Select reference data from connected Reference Data Loader node:"));

        // League ID input
        addDialogComponent(new DialogComponentNumberEdit(
            new SettingsModelInteger(UniversalNodeModel.CFGKEY_SELECTED_LEAGUE, -1),
            "League ID (e.g., 39 for Premier League):",
            10));

        // Season input (simple number for now, multi-select would require custom component)
        addDialogComponent(new DialogComponentLabel(
            "Season Year(s) - enter comma-separated years in Additional Parameters below"));

        // Team ID input
        addDialogComponent(new DialogComponentNumberEdit(
            new SettingsModelInteger(UniversalNodeModel.CFGKEY_SELECTED_TEAM, -1),
            "Team ID (optional, e.g., 40 for Liverpool):",
            10));

        addDialogComponent(new DialogComponentLabel(
            "Tip: To find IDs, query the Reference Data Loader output table, " +
            "or use endpoints like /leagues, /teams with the API."));

        closeCurrentGroup();

        createNewGroup("Additional Parameters (Optional)");

        // Add parameters input for additional fields like date, status, etc.
        addDialogComponent(new DialogComponentMultiLineString(
            new SettingsModelString(UniversalNodeModel.CFGKEY_PARAMETERS, "{}"),
            "Additional Parameters (JSON):",
            true,
            60,
            5));

        String helpText = "Enter additional parameters as JSON. Examples:\n" +
            "  {\"date\":\"2024-12-20\", \"status\":\"FT\"}\n" +
            "  {\"season\":\"2024\"}\n" +
            "  {\"live\":\"all\"}\n\n" +
            "Common endpoints:\n" +
            "  /fixtures - requires league, season; optional: team, date, status\n" +
            "  /standings - requires league, season\n" +
            "  /teams/statistics - requires league, season, team\n" +
            "  /players/topscorers - requires league, season";

        if (!errorMessage.isEmpty()) {
            helpText = errorMessage + "\n\nCheck Eclipse console for details.\n\n" + helpText;
        }

        addDialogComponent(new DialogComponentLabel(helpText));

        closeCurrentGroup();
    }
}
