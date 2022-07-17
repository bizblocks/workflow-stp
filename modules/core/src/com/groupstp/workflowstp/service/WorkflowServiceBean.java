package com.groupstp.workflowstp.service;

import com.groupstp.workflowstp.core.bean.WorkflowWorker;
import com.groupstp.workflowstp.entity.*;
import com.groupstp.workflowstp.exception.WorkflowException;
import com.groupstp.workflowstp.dto.WorkflowExecutionContext;
import com.haulmont.cuba.core.global.AppBeans;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;
import java.util.TreeMap;
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
    public Workflow determinateWorkflow(WorkflowEntity entity, @Nullable String view) throws WorkflowException {
        return worker.determinateWorkflow(entity, view);
    }

    @Override
    public Map<String, String> getWorkflowExecutionDelegates() {
        Map<String, String> result = new TreeMap<>();

        Map<String, WorkflowExecutionDelegate> items = AppBeans.getAll(WorkflowExecutionDelegate.class);
        if (items != null && items.size() > 0) {
            for (Map.Entry<String, WorkflowExecutionDelegate> item : items.entrySet()) {
                result.put(item.getValue().getName(), item.getKey());
            }
        }
        return result;
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
    public Workflow getWorkflow(WorkflowEntity entity, @Nullable String view) {
        return worker.getWorkflow(entity, view);
    }

    @Nullable
    @Override
    public WorkflowInstance getWorkflowInstance(WorkflowEntity entity) {
        return worker.getWorkflowInstance(entity);
    }

    @Nullable
    @Override
    public WorkflowInstance getWorkflowInstance(WorkflowEntity entity, @Nullable String view) {
        return worker.getWorkflowInstance(entity, view);
    }

    @Nullable
    @Override
    public WorkflowInstance getWorkflowInstanceIC(WorkflowEntity entity) {
        return worker.getWorkflowInstanceIC(entity);
    }

    @Nullable
    @Override
    public WorkflowInstance getWorkflowInstanceIC(WorkflowEntity entity, @Nullable String view) {
        return worker.getWorkflowInstanceIC(entity, view);
    }

    @Nullable
    @Override
    public WorkflowInstanceTask getWorkflowInstanceTask(WorkflowEntity entity) {
        return worker.getWorkflowInstanceTask(entity);
    }

    @Nullable
    @Override
    public WorkflowInstanceTask getWorkflowInstanceTask(WorkflowEntity entity, @Nullable String view) {
        return worker.getWorkflowInstanceTask(entity, view);
    }

    @Nullable
    @Override
    public WorkflowInstanceTask getWorkflowInstanceTaskIC(WorkflowEntity entity) {
        return worker.getWorkflowInstanceTaskIC(entity);
    }

    @Nullable
    @Override
    public WorkflowInstanceTask getWorkflowInstanceTaskIC(WorkflowEntity entity, @Nullable String view) {
        return worker.getWorkflowInstanceTaskIC(entity, view);
    }

    @Override
    public WorkflowInstanceTask getWorkflowInstanceTaskNN(WorkflowEntity entity, Stage stage) {
        return worker.getWorkflowInstanceTaskNN(entity, stage);
    }

    @Override
    public WorkflowInstanceTask getWorkflowInstanceTaskNN(WorkflowEntity entity, Stage stage, @Nullable String view) {
        return worker.getWorkflowInstanceTaskNN(entity, stage, view);
    }

    @Nullable
    @Override
    public Stage getStage(WorkflowEntity entity) {
        return worker.getStage(entity);
    }

    @Nullable
    @Override
    public Stage getStage(WorkflowEntity entity, @Nullable String view) {
        return worker.getStage(entity, view);
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