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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes a single API endpoint with all its configuration.
 */
public class EndpointDescriptor {

    private String id;
    private String path;
    private String category;
    private String subcategory;
    private String description;
    private List<String> keywords;
    private String method = "GET";

    private List<ParameterDescriptor> params;
    private ValidationRules validation;
    private PagingConfig paging;
    private CachingConfig caching;
    private ResponseConfig response;
    private MetadataConfig metadata;
    private List<ExampleConfig> examples;

    public EndpointDescriptor() {
        this.keywords = new ArrayList<>();
        this.params = new ArrayList<>();
        this.examples = new ArrayList<>();
        this.validation = new ValidationRules();
        this.paging = new PagingConfig();
        this.caching = new CachingConfig();
        this.response = new ResponseConfig();
        this.metadata = new MetadataConfig();
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<ParameterDescriptor> getParams() {
        return params;
    }

    public void setParams(List<ParameterDescriptor> params) {
        this.params = params;
    }

    public ValidationRules getValidation() {
        return validation;
    }

    public void setValidation(ValidationRules validation) {
        this.validation = validation;
    }

    public PagingConfig getPaging() {
        return paging;
    }

    public void setPaging(PagingConfig paging) {
        this.paging = paging;
    }

    public CachingConfig getCaching() {
        return caching;
    }

    public void setCaching(CachingConfig caching) {
        this.caching = caching;
    }

    public ResponseConfig getResponse() {
        return response;
    }

    public void setResponse(ResponseConfig response) {
        this.response = response;
    }

    public MetadataConfig getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataConfig metadata) {
        this.metadata = metadata;
    }

    public List<ExampleConfig> getExamples() {
        return examples;
    }

    public void setExamples(List<ExampleConfig> examples) {
        this.examples = examples;
    }

    /**
     * Get full display name with category.
     */
    public String getDisplayName() {
        if (subcategory != null && !subcategory.isEmpty()) {
            return category + " > " + subcategory + " > " + id;
        }
        return category + " > " + id;
    }

    /**
     * Get parameter by name.
     */
    public ParameterDescriptor getParameter(String name) {
        for (ParameterDescriptor param : params) {
            if (param.getName().equals(name)) {
                return param;
            }
        }
        return null;
    }

    /**
     * Configuration classes
     */

    public static class ValidationRules {
        private List<String> requiredParams = new ArrayList<>();
        private List<String> requiresAtLeastOneOf = new ArrayList<>();
        private List<List<String>> requiresOneOfGroups = new ArrayList<>();
        private List<List<String>> mutuallyExclusive = new ArrayList<>();

        public List<String> getRequiredParams() { return requiredParams; }
        public void setRequiredParams(List<String> requiredParams) { this.requiredParams = requiredParams; }
        public List<String> getRequiresAtLeastOneOf() { return requiresAtLeastOneOf; }
        public void setRequiresAtLeastOneOf(List<String> requiresAtLeastOneOf) { this.requiresAtLeastOneOf = requiresAtLeastOneOf; }
        public List<List<String>> getRequiresOneOfGroups() { return requiresOneOfGroups; }
        public void setRequiresOneOfGroups(List<List<String>> requiresOneOfGroups) { this.requiresOneOfGroups = requiresOneOfGroups; }
        public List<List<String>> getMutuallyExclusive() { return mutuallyExclusive; }
        public void setMutuallyExclusive(List<List<String>> mutuallyExclusive) { this.mutuallyExclusive = mutuallyExclusive; }
    }

    public static class PagingConfig {
        private boolean supported = false;
        private String paramName = "page";
        private Integer defaultPageSize;
        private Integer maxPages = 25;

        public boolean isSupported() { return supported; }
        public void setSupported(boolean supported) { this.supported = supported; }
        public String getParamName() { return paramName; }
        public void setParamName(String paramName) { this.paramName = paramName; }
        public Integer getDefaultPageSize() { return defaultPageSize; }
        public void setDefaultPageSize(Integer defaultPageSize) { this.defaultPageSize = defaultPageSize; }
        public Integer getMaxPages() { return maxPages; }
        public void setMaxPages(Integer maxPages) { this.maxPages = maxPages; }
    }

    public static class CachingConfig {
        public enum Policy { STATIC, REFERENCE, HOURLY, LIVE, NONE }

        private Policy policy = Policy.NONE;
        private Integer ttl;
        private String description;

        public Policy getPolicy() { return policy; }
        public void setPolicy(Policy policy) { this.policy = policy; }
        public Integer getTtl() { return ttl; }
        public void setTtl(Integer ttl) { this.ttl = ttl; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        /**
         * Get effective TTL based on policy or explicit value.
         */
        public int getEffectiveTtl() {
            if (ttl != null) return ttl;
            switch (policy) {
                case STATIC: return 2592000;  // 30 days
                case REFERENCE: return 86400;  // 1 day
                case HOURLY: return 3600;  // 1 hour
                case LIVE: return 300;  // 5 min
                case NONE:
                default: return 0;
            }
        }
    }

    public static class ResponseConfig {
        public enum ResponseType { ARRAY, OBJECT }

        private String rootPath = "response";
        private ResponseType type = ResponseType.ARRAY;
        private FlattenConfig flatten;

        public ResponseConfig() {
            this.flatten = new FlattenConfig();
        }

        public String getRootPath() { return rootPath; }
        public void setRootPath(String rootPath) { this.rootPath = rootPath; }
        public ResponseType getType() { return type; }
        public void setType(ResponseType type) { this.type = type; }
        public FlattenConfig getFlatten() { return flatten; }
        public void setFlatten(FlattenConfig flatten) { this.flatten = flatten; }
    }

    public static class FlattenConfig {
        private String prefix = "";
        private List<NestedObjectRule> nestedObjects = new ArrayList<>();
        private List<NestedArrayRule> nestedArrays = new ArrayList<>();
        private List<RenameRule> renameColumns = new ArrayList<>();
        private List<String> excludeColumns = new ArrayList<>();

        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
        public List<NestedObjectRule> getNestedObjects() { return nestedObjects; }
        public void setNestedObjects(List<NestedObjectRule> nestedObjects) { this.nestedObjects = nestedObjects; }
        public List<NestedArrayRule> getNestedArrays() { return nestedArrays; }
        public void setNestedArrays(List<NestedArrayRule> nestedArrays) { this.nestedArrays = nestedArrays; }
        public List<RenameRule> getRenameColumns() { return renameColumns; }
        public void setRenameColumns(List<RenameRule> renameColumns) { this.renameColumns = renameColumns; }
        public List<String> getExcludeColumns() { return excludeColumns; }
        public void setExcludeColumns(List<String> excludeColumns) { this.excludeColumns = excludeColumns; }
    }

    public static class NestedObjectRule {
        public enum Strategy { FLATTEN, JSON }

        private String path;
        private Strategy strategy = Strategy.FLATTEN;
        private String prefix = "";

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public Strategy getStrategy() { return strategy; }
        public void setStrategy(Strategy strategy) { this.strategy = strategy; }
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
    }

    public static class NestedArrayRule {
        public enum Strategy { STRINGIFY, EXPLODE, IGNORE }

        private String path;
        private Strategy strategy = Strategy.STRINGIFY;
        private String prefix = "";

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public Strategy getStrategy() { return strategy; }
        public void setStrategy(Strategy strategy) { this.strategy = strategy; }
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
    }

    public static class RenameRule {
        private String from;
        private String to;

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
    }

    public static class MetadataConfig {
        private String apiTier;
        private String rateLimit;
        private Integer quotaWeight = 1;

        public String getApiTier() { return apiTier; }
        public void setApiTier(String apiTier) { this.apiTier = apiTier; }
        public String getRateLimit() { return rateLimit; }
        public void setRateLimit(String rateLimit) { this.rateLimit = rateLimit; }
        public Integer getQuotaWeight() { return quotaWeight; }
        public void setQuotaWeight(Integer quotaWeight) { this.quotaWeight = quotaWeight; }
    }

    public static class ExampleConfig {
        private String title;
        private Map<String, Object> params;
        private String description;

        public ExampleConfig() {
            this.params = new HashMap<>();
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Map<String, Object> getParams() { return params; }
        public void setParams(Map<String, Object> params) { this.params = params; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
