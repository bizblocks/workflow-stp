package com.groupstp.workflowstp.entity;

import com.haulmont.cuba.core.entity.Entity;

/**
 * All entities which are using in workflow process must implement this interface
 *
 * @author adiatullin
 */
public interface WorkflowEntity<T> extends Entity<T> {
    /**
     * @return current processing step name
     */
    String getStepName();

    void setStepName(String stepName);

    /**
     * @return processing workflow entity
     */
    Workflow getWorkflow();

    void setWorkflow(Workflow workflow);

    /**
     * @return processing status
     */
    WorkflowEntityStatus getStatus();

    void setStatus(WorkflowEntityStatus status);
}
