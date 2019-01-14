package com.groupstp.workflowstp.web.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;

/**
 * This interface using to hold settings of web workflow behaviour
 *
 * @author adiatullin
 */
@Source(type = SourceType.DATABASE)
public interface WorkflowWebConfig extends Config {

    /**
     * @return when extending tables should selection column be visible
     */
    @Property("workflow.showSelection")
    @DefaultBoolean(true)
    Boolean getShowSelection();
    void setShowSelection(Boolean value);

    /**
     * @return should system log a workflow screen extension scripts
     */
    @Property("workflow.printScreenExtensionScripts")
    @DefaultBoolean(false)
    Boolean getPrintScreenExtensionsScript();
    void setPrintScreenExtensionsScript(Boolean value);
}
