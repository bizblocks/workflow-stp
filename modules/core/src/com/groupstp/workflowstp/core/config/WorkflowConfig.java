package com.groupstp.workflowstp.core.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;

/**
 * Workflow functional configuration
 *
 * @author adiatullin
 */
@Source(type = SourceType.DATABASE)
public interface WorkflowConfig extends Config {
}
