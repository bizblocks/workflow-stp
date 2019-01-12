package com.groupstp.workflowstp.web.util;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Map creator helper class
 *
 * @author adiatullin
 */
public final class MapHelper {
    private MapHelper() {
    }

    /**
     * Create map object from provided key-value collection
     *
     * @param keyValues set of key value strings
     * @return constructed map
     */
    @Nullable
    public static Map<String, Object> asMap(Object... keyValues) {
        if (keyValues != null && keyValues.length > 0) {
            Map<String, Object> result = new HashMap<>();
            for (int i = 0; i < keyValues.length; i += 2) {
                Object key = keyValues[i];
                Object value = null;
                int valueIndex = i + 1;
                if (keyValues.length > valueIndex) {
                    value = keyValues[valueIndex];
                }
                result.put(key == null ? null : key.toString(), value);
            }
            return result;
        }
        return null;
    }
}
