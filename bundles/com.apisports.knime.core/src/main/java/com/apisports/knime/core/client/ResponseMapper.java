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
