package com.groupstp.workflowstp.data.impl;

import com.groupstp.workflowstp.data.WorkflowExecutionData;
import com.groupstp.workflowstp.dto.WorkflowExecutionContext;
import com.groupstp.workflowstp.entity.WorkflowInstance;
import com.groupstp.workflowstp.entity.WorkflowInstanceTask;
import com.haulmont.cuba.core.entity.Entity;

/**
 * Basic implementation of workflow execution data
 *
 * @author adiatullin
 * @see com.groupstp.workflowstp.service.WorkflowExecutionDelegate
 */
public final class BaseWorkflowExecutionData implements WorkflowExecutionData {

    private final WorkflowInstance instance;
    private final WorkflowInstanceTask task;
    private final Entity processingEntity;
    private final WorkflowExecutionContext executionContext;

    public BaseWorkflowExecutionData(WorkflowInstance instance, WorkflowInstanceTask task,
                                     Entity processingEntity, WorkflowExecutionContext executionContext) {
        this.instance = instance;
        this.task = task;
        this.processingEntity = processingEntity;
        this.executionContext = executionContext;
    }

    @Override
    public WorkflowInstance getInstance() {
        return instance;
    }

    @Override
    public WorkflowInstanceTask getTask() {
        return task;
    }

    @Override
    public <T extends Entity> T getProcessingEntity() {
        //noinspection unchecked
        return (T) processingEntity;
    }

    @Override
    public WorkflowExecutionContext getExecutionContext() {
        return executionContext;
    }
}
