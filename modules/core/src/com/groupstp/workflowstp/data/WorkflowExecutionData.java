package com.groupstp.workflowstp.data;

import com.groupstp.workflowstp.dto.WorkflowExecutionContext;
import com.groupstp.workflowstp.entity.WorkflowInstance;
import com.groupstp.workflowstp.entity.WorkflowInstanceTask;
import com.haulmont.cuba.core.entity.Entity;

/**
 * Workflow execution all data
 *
 * @author adiatullin
 */
public interface WorkflowExecutionData {

    /**
     * @return current processing instance
     */
    WorkflowInstance getInstance();

    /**
     * @return current processing task
     */
    WorkflowInstanceTask getTask();

    /**
     * Current processing workflow entity
     *
     * @param <T> type of entity
     * @return entity
     */
    <T extends Entity> T getProcessingEntity();

    /**
     * @return execution context where store all parameters
     */
    WorkflowExecutionContext getExecutionContext();
}
