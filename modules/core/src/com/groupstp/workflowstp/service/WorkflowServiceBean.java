package com.groupstp.workflowstp.service;

import com.groupstp.workflowstp.core.bean.WorkflowWorker;
import com.groupstp.workflowstp.dto.WorkflowExecutionContext;
import com.groupstp.workflowstp.entity.Workflow;
import com.groupstp.workflowstp.entity.WorkflowEntity;
import com.groupstp.workflowstp.entity.WorkflowInstance;
import com.groupstp.workflowstp.entity.WorkflowInstanceTask;
import com.groupstp.workflowstp.exception.WorkflowException;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

/**
 * Base implementation of workflow service
 *
 * @author adiatullin
 */
@Service(WorkflowService.NAME)
public class WorkflowServiceBean implements WorkflowService {

    @Inject
    private WorkflowWorker worker;

    @Override
    public UUID startWorkflow(WorkflowEntity entity, Workflow wf) throws WorkflowException {
        return worker.startWorkflow(entity, wf);
    }

    @Override
    public void restartWorkflow(WorkflowInstance instance) throws WorkflowException {
        worker.restartWorkflow(instance);
    }

    @Override
    public void finishTask(WorkflowInstanceTask task) throws WorkflowException {
        worker.finishTask(task);
    }

    @Override
    public void finishTask(WorkflowInstanceTask task, Map<String, String> params) throws WorkflowException {
        worker.finishTask(task, params);
    }

    @Override
    public WorkflowExecutionContext getExecutionContext(WorkflowInstance instance) {
        return worker.getExecutionContext(instance);
    }

    @Override
    public void setExecutionContext(WorkflowExecutionContext context, WorkflowInstance instance) {
        worker.setExecutionContext(context, instance);
    }

    @Nullable
    @Override
    public String getParameter(WorkflowInstance instance, @Nullable String key) {
        return worker.getParameter(instance, key);
    }

    @Override
    public void setParameter(WorkflowInstance instance, @Nullable String key, @Nullable String value) {
        worker.setParameter(instance, key, value);
    }
}