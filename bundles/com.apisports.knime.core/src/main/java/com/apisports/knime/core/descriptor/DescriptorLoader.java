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

package com.apisports.knime.core.descriptor;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads endpoint descriptors from YAML using manual parsing.
 * More robust than automatic bean mapping.
 */
public class DescriptorLoader {

    /**
     * Load descriptors from YAML InputStream.
     */
    public static List<EndpointDescriptor> loadFromStream(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream is null");
        }

        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(inputStream);

        List<EndpointDescriptor> descriptors = new ArrayList<>();
        List<Map<String, Object>> endpointsList = (List<Map<String, Object>>) data.get("endpoints");

        if (endpointsList == null) {
            throw new Exception("No 'endpoints' key found in YAML");
        }

        for (Map<String, Object> endpointMap : endpointsList) {
            EndpointDescriptor descriptor = parseEndpoint(endpointMap);
            descriptors.add(descriptor);
        }

        return descriptors;
    }

    private static EndpointDescriptor parseEndpoint(Map<String, Object> map) {
        EndpointDescriptor desc = new EndpointDescriptor();

        desc.setId(getString(map, "id"));
        desc.setPath(getString(map, "path"));
        desc.setCategory(getString(map, "category"));
        desc.setSubcategory(getString(map, "subcategory"));
        desc.setDescription(getString(map, "description"));
        desc.setMethod(getString(map, "method", "GET"));

        // Keywords
        List<String> keywords = (List<String>) map.get("keywords");
        if (keywords != null) {
            desc.setKeywords(keywords);
        }

        // Parameters
        List<Map<String, Object>> paramsList = (List<Map<String, Object>>) map.get("params");
        if (paramsList != null) {
            for (Map<String, Object> paramMap : paramsList) {
                desc.getParams().add(parseParameter(paramMap));
            }
        }

        // Validation
        Map<String, Object> validation = (Map<String, Object>) map.get("validation");
        if (validation != null) {
            parseValidation(desc.getValidation(), validation);
        }

        // Paging
        Map<String, Object> paging = (Map<String, Object>) map.get("paging");
        if (paging != null) {
            parsePaging(desc.getPaging(), paging);
        }

        // Caching
        Map<String, Object> caching = (Map<String, Object>) map.get("caching");
        if (caching != null) {
            parseCaching(desc.getCaching(), caching);
        }

        // Response
        Map<String, Object> response = (Map<String, Object>) map.get("response");
        if (response != null) {
            parseResponse(desc.getResponse(), response);
        }

        // Metadata
        Map<String, Object> metadata = (Map<String, Object>) map.get("metadata");
        if (metadata != null) {
            parseMetadata(desc.getMetadata(), metadata);
        }

        return desc;
    }

    private static ParameterDescriptor parseParameter(Map<String, Object> map) {
        ParameterDescriptor param = new ParameterDescriptor();

        param.setName(getString(map, "name"));
        param.setDescription(getString(map, "description"));
        param.setRequired(getBoolean(map, "required", false));

        // Type
        String typeStr = getString(map, "type");
        if (typeStr != null) {
            param.setType(ParameterDescriptor.ParameterType.valueOf(typeStr.toUpperCase()));
        }

        // Integer constraints
        if (map.containsKey("min")) {
            param.setMin(getInteger(map, "min"));
        }
        if (map.containsKey("max")) {
            param.setMax(getInteger(map, "max"));
        }

        // Enum values
        List<String> enumValues = (List<String>) map.get("enum");
        if (enumValues != null) {
            param.setEnumValues(enumValues);
        }

        List<String> enumLabels = (List<String>) map.get("enumLabels");
        if (enumLabels != null) {
            param.setEnumLabels(enumLabels);
        }

        return param;
    }

    private static void parseValidation(EndpointDescriptor.ValidationRules validation, Map<String, Object> map) {
        List<String> required = (List<String>) map.get("requiredParams");
        if (required != null) {
            validation.setRequiredParams(required);
        }

        List<String> atLeastOne = (List<String>) map.get("requiresAtLeastOneOf");
        if (atLeastOne != null) {
            validation.setRequiresAtLeastOneOf(atLeastOne);
        }
    }

    private static void parsePaging(EndpointDescriptor.PagingConfig paging, Map<String, Object> map) {
        paging.setSupported(getBoolean(map, "supported", false));
        paging.setParamName(getString(map, "paramName", "page"));
        if (map.containsKey("defaultPageSize")) {
            paging.setDefaultPageSize(getInteger(map, "defaultPageSize"));
        }
        if (map.containsKey("maxPages")) {
            paging.setMaxPages(getInteger(map, "maxPages"));
        }
    }

    private static void parseCaching(EndpointDescriptor.CachingConfig caching, Map<String, Object> map) {
        String policy = getString(map, "policy");
        if (policy != null) {
            caching.setPolicy(EndpointDescriptor.CachingConfig.Policy.valueOf(policy.toUpperCase()));
        }

        if (map.containsKey("ttl")) {
            caching.setTtl(getInteger(map, "ttl"));
        }

        caching.setDescription(getString(map, "description"));
    }

    private static void parseResponse(EndpointDescriptor.ResponseConfig response, Map<String, Object> map) {
        response.setRootPath(getString(map, "rootPath", "response"));

        String typeStr = getString(map, "type");
        if (typeStr != null) {
            response.setType(EndpointDescriptor.ResponseConfig.ResponseType.valueOf(typeStr.toUpperCase()));
        }

        // Flatten config (simplified for now - just get prefix)
        Map<String, Object> flatten = (Map<String, Object>) map.get("flatten");
        if (flatten != null) {
            response.getFlatten().setPrefix(getString(flatten, "prefix", ""));
        }
    }

    private static void parseMetadata(EndpointDescriptor.MetadataConfig metadata, Map<String, Object> map) {
        metadata.setApiTier(getString(map, "apiTier"));
        metadata.setRateLimit(getString(map, "rateLimit"));
        if (map.containsKey("quotaWeight")) {
            metadata.setQuotaWeight(getInteger(map, "quotaWeight"));
        }
    }

    // Helper methods
    private static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private static String getString(Map<String, Object> map, String key, String defaultValue) {
        String value = getString(map, key);
        return value != null ? value : defaultValue;
    }

    private static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    private static Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
}
