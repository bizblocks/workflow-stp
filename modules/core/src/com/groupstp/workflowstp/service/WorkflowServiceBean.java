package com.groupstp.workflowstp.service;

import com.groupstp.workflowstp.core.bean.WorkflowWorker;
import com.groupstp.workflowstp.entity.*;
import com.groupstp.workflowstp.exception.WorkflowException;
import com.groupstp.workflowstp.dto.WorkflowExecutionContext;
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
    public Workflow determinateWorkflow(WorkflowEntity entity) throws WorkflowException {
        return worker.determinateWorkflow(entity);
    }

    @Override
    public UUID startWorkflow(WorkflowEntity entity, Workflow wf) throws WorkflowException {
        return worker.startWorkflow(entity, wf);
    }

    @Override
    public void restartWorkflow(WorkflowInstance instance) throws WorkflowException {
        worker.restartWorkflow(instance);
    }

    @Override
    public void resetWorkflow(WorkflowInstance instance, Workflow wf) throws WorkflowException {
        worker.resetWorkflow(instance, wf);
    }

    @Override
    public void recreateTasks(WorkflowInstance instance) throws WorkflowException {
        worker.recreateTasks(instance);
    }

    @Override
    public void moveWorkflow(WorkflowInstance instance, Step step) throws WorkflowException {
        worker.moveWorkflow(instance, step);
    }

    @Override
    public boolean isProcessing(WorkflowEntity entity) {
        return worker.isProcessing(entity);
    }

    @Nullable
    @Override
    public Workflow getWorkflow(WorkflowEntity entity) {
        return worker.getWorkflow(entity);
    }

    @Nullable
    @Override
    public WorkflowInstance getWorkflowInstance(WorkflowEntity entity) {
        return worker.getWorkflowInstance(entity);
    }

    @Nullable
    @Override
    public WorkflowInstance getWorkflowInstanceIC(WorkflowEntity entity) {
        return worker.getWorkflowInstanceIC(entity);
    }

    @Nullable
    @Override
    public WorkflowInstanceTask getWorkflowInstanceTask(WorkflowEntity entity) {
        return worker.getWorkflowInstanceTask(entity);
    }

    @Nullable
    @Override
    public WorkflowInstanceTask getWorkflowInstanceTaskIC(WorkflowEntity entity) {
        return worker.getWorkflowInstanceTaskIC(entity);
    }

    @Override
    public WorkflowInstanceTask getWorkflowInstanceTaskNN(WorkflowEntity entity, Stage stage) {
        return worker.getWorkflowInstanceTaskNN(entity, stage);
    }

    @Nullable
    @Override
    public Stage getStage(WorkflowEntity entity) {
        return worker.getStage(entity);
    }

    @Override
    public void finishTask(WorkflowInstanceTask task, String... performersLogin) throws WorkflowException {
        worker.finishTask(task, performersLogin);
    }

    @Override
    public void finishTask(WorkflowInstanceTask task, Map<String, String> params, String... performersLogin) throws WorkflowException {
        worker.finishTask(task, params, performersLogin);
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