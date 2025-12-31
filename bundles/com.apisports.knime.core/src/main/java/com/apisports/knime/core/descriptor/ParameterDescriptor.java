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
import java.util.List;

/**
 * Describes a single parameter for an API endpoint.
 */
public class ParameterDescriptor {

    public enum ParameterType {
        INTEGER, STRING, BOOLEAN, DATE, ENUM
    }

    private String name;
    private ParameterType type;
    private boolean required;
    private String description;
    private Object defaultValue;

    // Integer constraints
    private Integer min;
    private Integer max;

    // String constraints
    private String pattern;
    private Integer minLength;
    private Integer maxLength;

    // Enum constraints
    private List<String> enumValues;
    private List<String> enumLabels;

    // Date format
    private String format;

    public ParameterDescriptor() {
        this.enumValues = new ArrayList<>();
        this.enumLabels = new ArrayList<>();
    }

    // Getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ParameterType getType() {
        return type;
    }

    public void setType(ParameterType type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(List<String> enumValues) {
        this.enumValues = enumValues;
    }

    public List<String> getEnumLabels() {
        return enumLabels;
    }

    public void setEnumLabels(List<String> enumLabels) {
        this.enumLabels = enumLabels;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Get display label for an enum value.
     */
    public String getEnumLabel(String value) {
        int index = enumValues.indexOf(value);
        if (index >= 0 && index < enumLabels.size()) {
            return enumLabels.get(index);
        }
        return value;
    }
}
