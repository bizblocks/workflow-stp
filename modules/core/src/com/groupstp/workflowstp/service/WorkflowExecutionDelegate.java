package com.groupstp.workflowstp.service;

import com.groupstp.workflowstp.data.WorkflowExecutionData;

/**
 * All beans which should execute in workflow process should implement current interface
 *
 * @author adiatullin
 */
public interface WorkflowExecutionDelegate {

    /**
     * Current workflow execution delegate name to show it to user
     *
     * @return name of execution delegate
     */
    String getName();

    /**
     * Do execution by provided workflow execution data.
     *
     * @param data all execution data
     * @return flag what mean this logic finished and process can move to the next step or not
     */
    boolean execute(WorkflowExecutionData data);
}
