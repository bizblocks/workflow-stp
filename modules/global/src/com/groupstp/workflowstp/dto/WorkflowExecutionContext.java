package com.groupstp.workflowstp.dto;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This class using for holding workflow execution variables
 *
 * @author adiatullin
 */
public class WorkflowExecutionContext implements Serializable {
    private static final long serialVersionUID = -4720713209728129217L;

    protected final Map<String, String> params = new HashMap<>();

    /**
     * Get parameter value by key
     *
     * @param key parameter key
     * @return parameter value
     */
    @Nullable
    public String getParam(@Nullable String key) {
        return params.get(key);
    }

    /**
     * Set parameter value
     *
     * @param key   parameter key
     * @param value parameter value
     */
    public void putParam(@Nullable String key, @Nullable String value) {
        params.put(key, value);
    }

    /**
     * Get all variables
     *
     * @return map object of variables. Can be changed directly.
     */
    public Map<String, String> getParams() {
        return params;
    }
}