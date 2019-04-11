package com.groupstp.workflowstp.core.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;

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
}
