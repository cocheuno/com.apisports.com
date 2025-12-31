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

package com.apisports.knime.core.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.io.IOException;

/**
 * Utility class for mapping JSON responses to POJOs.
 */
public class ResponseMapper {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Map a JSON string to the specified class type.
     * 
     * @param <T> The target type
     * @param json The JSON string
     * @param targetClass The target class
     * @return The mapped object
     * @throws IOException if mapping fails
     */
    public static <T> T map(String json, Class<T> targetClass) throws IOException {
        return OBJECT_MAPPER.readValue(json, targetClass);
    }

    /**
     * Get the shared ObjectMapper instance.
     * 
     * @return The ObjectMapper
     */
    public static ObjectMapper getMapper() {
        return OBJECT_MAPPER;
    }
}
