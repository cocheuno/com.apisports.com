package com.apisports.knime.core.descriptor;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registry for loading and managing endpoint descriptors.
 * Singleton pattern for accessing descriptors across nodes.
 */
public class DescriptorRegistry {

    private static DescriptorRegistry instance;

    private final Map<String, EndpointDescriptor> descriptorsById;
    private final Map<String, List<EndpointDescriptor>> descriptorsByCategory;
    private String version;
    private String sport;

    private DescriptorRegistry() {
        this.descriptorsById = new HashMap<>();
        this.descriptorsByCategory = new HashMap<>();
    }

    /**
     * Get singleton instance.
     */
    public static synchronized DescriptorRegistry getInstance() {
        if (instance == null) {
            instance = new DescriptorRegistry();
        }
        return instance;
    }

    /**
     * Load descriptors from a YAML resource file.
     *
     * @param resourcePath Path to YAML file in classpath (e.g., "/descriptors/football-endpoints.yaml")
     */
    public synchronized void loadFromResource(String resourcePath) throws Exception {
        // Use manual loader for more robust parsing
        List<EndpointDescriptor> descriptors = DescriptorLoader.loadFromResource(resourcePath);

        // Clear existing
        descriptorsById.clear();
        descriptorsByCategory.clear();

        // Index descriptors
        for (EndpointDescriptor descriptor : descriptors) {
            // Validate required fields
            validateDescriptor(descriptor);

            // Index by ID
            descriptorsById.put(descriptor.getId(), descriptor);

            // Index by category
            String category = descriptor.getCategory();
            descriptorsByCategory
                .computeIfAbsent(category, k -> new ArrayList<>())
                .add(descriptor);
        }

        this.version = "1.0"; // TODO: extract from YAML
        this.sport = "football"; // TODO: extract from YAML
    }

    /**
     * Validate descriptor has required fields.
     */
    private void validateDescriptor(EndpointDescriptor descriptor) throws Exception {
        if (descriptor.getId() == null || descriptor.getId().isEmpty()) {
            throw new Exception("Descriptor missing required field: id");
        }
        if (descriptor.getPath() == null || descriptor.getPath().isEmpty()) {
            throw new Exception("Descriptor " + descriptor.getId() + " missing required field: path");
        }
        if (descriptor.getCategory() == null || descriptor.getCategory().isEmpty()) {
            throw new Exception("Descriptor " + descriptor.getId() + " missing required field: category");
        }
    }

    /**
     * Get descriptor by ID.
     */
    public EndpointDescriptor getDescriptor(String id) {
        return descriptorsById.get(id);
    }

    /**
     * Get all descriptors.
     */
    public List<EndpointDescriptor> getAllDescriptors() {
        return new ArrayList<>(descriptorsById.values());
    }

    /**
     * Get all categories.
     */
    public List<String> getCategories() {
        return new ArrayList<>(descriptorsByCategory.keySet());
    }

    /**
     * Get descriptors by category.
     */
    public List<EndpointDescriptor> getDescriptorsByCategory(String category) {
        return descriptorsByCategory.getOrDefault(category, new ArrayList<>());
    }

    /**
     * Search descriptors by keyword.
     */
    public List<EndpointDescriptor> searchDescriptors(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllDescriptors();
        }

        String lowerQuery = query.toLowerCase();
        return descriptorsById.values().stream()
            .filter(d -> matchesQuery(d, lowerQuery))
            .collect(Collectors.toList());
    }

    private boolean matchesQuery(EndpointDescriptor descriptor, String query) {
        // Match ID
        if (descriptor.getId().toLowerCase().contains(query)) {
            return true;
        }
        // Match description
        if (descriptor.getDescription() != null &&
            descriptor.getDescription().toLowerCase().contains(query)) {
            return true;
        }
        // Match keywords
        for (String keyword : descriptor.getKeywords()) {
            if (keyword.toLowerCase().contains(query)) {
                return true;
            }
        }
        // Match category
        if (descriptor.getCategory().toLowerCase().contains(query)) {
            return true;
        }
        return false;
    }

    public String getVersion() {
        return version;
    }

    public String getSport() {
        return sport;
    }

    /**
     * YAML file structure for parsing.
     */
    public static class DescriptorFile {
        private String version;
        private String sport;
        private List<EndpointDescriptor> endpoints;

        public DescriptorFile() {
            this.endpoints = new ArrayList<>();
        }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getSport() { return sport; }
        public void setSport(String sport) { this.sport = sport; }
        public List<EndpointDescriptor> getEndpoints() { return endpoints; }
        public void setEndpoints(List<EndpointDescriptor> endpoints) { this.endpoints = endpoints; }
    }
}
