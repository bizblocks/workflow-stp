package com.groupstp.workflowstp.rest.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;

/**
 * This interface using to hold settings of REST workflow behaviour
 *
 * @author adiatullin
 */
@Source(type = SourceType.DATABASE)
public interface WorkflowRestConfig extends Config {

    /**
     * @return is rest communication enabled or not
     */
    @Property("workflow.rest.enabled")
    @DefaultBoolean(true)
    Boolean getRestEnabled();

    void setRestEnabled(Boolean value);
}
