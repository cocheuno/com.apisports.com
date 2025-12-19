package com.apisports.knime.football.nodes.referencedata;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentDate;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDate;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Dialog for Reference Data Loader node.
 */
public class ReferenceDataLoaderNodeDialog extends DefaultNodeSettingsPane {

    protected ReferenceDataLoaderNodeDialog() {
        super();

        createNewGroup("Database Configuration");

        addDialogComponent(new DialogComponentLabel(
            "Database file path (will be auto-created on first execution):"));

        addDialogComponent(new DialogComponentString(
            new SettingsModelString(ReferenceDataLoaderNodeModel.CFGKEY_DB_PATH,
                ReferenceDataLoaderNodeModel.getDefaultDbPath()),
            "Database Path:",
            true,
            60));

        addDialogComponent(new DialogComponentBoolean(
            new SettingsModelBoolean(ReferenceDataLoaderNodeModel.CFGKEY_CLEAR_AND_RELOAD, false),
            "Clear & Reload (wipe existing data before loading)"));

        closeCurrentGroup();

        createNewGroup("Time Period Filter (use EITHER date range OR seasons)");

        addDialogComponent(new DialogComponentLabel(
            "Date Range: Specify beginning date (required) and optional end date"));

        addDialogComponent(new DialogComponentDate(
            new SettingsModelDate(ReferenceDataLoaderNodeModel.CFGKEY_BEGIN_DATE, null),
            "Beginning Date:"));

        addDialogComponent(new DialogComponentDate(
            new SettingsModelDate(ReferenceDataLoaderNodeModel.CFGKEY_END_DATE, null),
            "End Date (optional):"));

        addDialogComponent(new DialogComponentLabel(
            "OR select specific seasons (years):"));

        // Generate list of seasons from 2008 to current year + 1
        List<String> availableSeasons = new ArrayList<>();
        int currentYear = java.time.Year.now().getValue();
        for (int year = 2008; year <= currentYear + 1; year++) {
            availableSeasons.add(String.valueOf(year));
        }

        addDialogComponent(new DialogComponentStringListSelection(
            new SettingsModelStringArray(ReferenceDataLoaderNodeModel.CFGKEY_SELECTED_SEASONS, new String[0]),
            "Seasons:",
            availableSeasons,
            false,
            10));

        closeCurrentGroup();

        createNewGroup("Country Filter");

        // All countries in the world (195+ sovereign nations)
        List<String> availableCountries = Arrays.asList(
            "Afghanistan", "Albania", "Algeria", "Andorra", "Angola", "Antigua-and-Barbuda", "Argentina",
            "Armenia", "Australia", "Austria", "Azerbaijan", "Bahamas", "Bahrain", "Bangladesh",
            "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bhutan", "Bolivia",
            "Bosnia-and-Herzegovina", "Botswana", "Brazil", "Brunei", "Bulgaria", "Burkina-Faso",
            "Burundi", "Cabo-Verde", "Cambodia", "Cameroon", "Canada", "Central-African-Republic",
            "Chad", "Chile", "China", "Colombia", "Comoros", "Congo", "Costa-Rica", "Croatia",
            "Cuba", "Cyprus", "Czech-Republic", "Denmark", "Djibouti", "Dominica", "Dominican-Republic",
            "Ecuador", "Egypt", "El-Salvador", "England", "Equatorial-Guinea", "Eritrea", "Estonia",
            "Eswatini", "Ethiopia", "Fiji", "Finland", "France", "Gabon", "Gambia", "Georgia",
            "Germany", "Ghana", "Greece", "Grenada", "Guatemala", "Guinea", "Guinea-Bissau",
            "Guyana", "Haiti", "Honduras", "Hungary", "Iceland", "India", "Indonesia", "Iran",
            "Iraq", "Ireland", "Israel", "Italy", "Ivory-Coast", "Jamaica", "Japan", "Jordan",
            "Kazakhstan", "Kenya", "Kiribati", "Kosovo", "Kuwait", "Kyrgyzstan", "Laos", "Latvia",
            "Lebanon", "Lesotho", "Liberia", "Libya", "Liechtenstein", "Lithuania", "Luxembourg",
            "Madagascar", "Malawi", "Malaysia", "Maldives", "Mali", "Malta", "Marshall-Islands",
            "Mauritania", "Mauritius", "Mexico", "Micronesia", "Moldova", "Monaco", "Mongolia",
            "Montenegro", "Morocco", "Mozambique", "Myanmar", "Namibia", "Nauru", "Nepal",
            "Netherlands", "New-Zealand", "Nicaragua", "Niger", "Nigeria", "North-Korea",
            "North-Macedonia", "Northern-Ireland", "Norway", "Oman", "Pakistan", "Palau", "Palestine",
            "Panama", "Papua-New-Guinea", "Paraguay", "Peru", "Philippines", "Poland", "Portugal",
            "Qatar", "Romania", "Russia", "Rwanda", "Saint-Kitts-and-Nevis", "Saint-Lucia",
            "Saint-Vincent-and-the-Grenadines", "Samoa", "San-Marino", "Sao-Tome-and-Principe",
            "Saudi-Arabia", "Scotland", "Senegal", "Serbia", "Seychelles", "Sierra-Leone",
            "Singapore", "Slovakia", "Slovenia", "Solomon-Islands", "Somalia", "South-Africa",
            "South-Korea", "South-Sudan", "Spain", "Sri-Lanka", "Sudan", "Suriname", "Sweden",
            "Switzerland", "Syria", "Taiwan", "Tajikistan", "Tanzania", "Thailand", "Timor-Leste",
            "Togo", "Tonga", "Trinidad-and-Tobago", "Tunisia", "Turkey", "Turkmenistan", "Tuvalu",
            "Uganda", "Ukraine", "United-Arab-Emirates", "Uruguay", "USA", "Uzbekistan", "Vanuatu",
            "Vatican-City", "Venezuela", "Vietnam", "Wales", "Yemen", "Zambia", "Zimbabwe"
        );

        addDialogComponent(new DialogComponentStringListSelection(
            new SettingsModelStringArray(ReferenceDataLoaderNodeModel.CFGKEY_COUNTRY_FILTER, new String[0]),
            "Filter by Countries (leave empty for all):",
            availableCountries,
            false,
            10));

        closeCurrentGroup();

        createNewGroup("Data Loading Options");

        addDialogComponent(new DialogComponentBoolean(
            new SettingsModelBoolean(ReferenceDataLoaderNodeModel.CFGKEY_LOAD_TEAMS, true),
            "Load Teams (may take time for many leagues)"));

        addDialogComponent(new DialogComponentBoolean(
            new SettingsModelBoolean(ReferenceDataLoaderNodeModel.CFGKEY_LOAD_VENUES, false),
            "Load Venues (not yet implemented)"));

        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(ReferenceDataLoaderNodeModel.CFGKEY_CACHE_TTL, 86400),
            "Cache TTL (seconds):", 3600));

        closeCurrentGroup();
    }
}
