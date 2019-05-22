package com.groupstp.workflowstp.core.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;
import com.haulmont.cuba.core.config.defaults.DefaultInteger;

/**
 * Workflow functional configuration
 *
 * @author adiatullin
 */
@Source(type = SourceType.DATABASE)
public interface WorkflowConfig extends Config {

    /**
     * @return is heartbeat functional enabled (support timeouts, repeats and etc.)
     */
    @Property("workflow.heartbeatEnable")
    @DefaultBoolean(true)
    Boolean getHeartbeatEnable();

    void setHeartbeatEnable(Boolean value);

    /**
     * How many scheduler ticks to skip after server startup.
     * Actual workflow refresh will start with the next call.
     * <br> This reduces the server load on startup.
     */
    @Property("workflow.delayCallCount")
    @DefaultInteger(5)
    int getDelayCallCount();

    void setDelayCallCount(int value);

}
