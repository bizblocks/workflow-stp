package com.groupstp.workflowstp.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Json helper bean which can serialize or deserialize any objects
 *
 * @author adiatullin
 */
@Component(JsonUtil.NAME)
public class JsonUtil {
    public static final String NAME = "wfstp_JsonUtil";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Serialize provided object into json text
     *
     * @param object any serializable object
     * @param <T>    object type
     * @return serialized json text
     */
    public <T> String toJson(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    /**
     * Deserialize expected object from provided json text
     *
     * @param json  text
     * @param clazz expected object class
     * @param <T>   object type
     * @return deserialized java object
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("JSON deserialization failed", e);
        }
    }
}
