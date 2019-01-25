package com.groupstp.workflowstp.core.bean;

import com.groupstp.workflowstp.entity.*;
import com.groupstp.workflowstp.exception.WorkflowException;
import com.groupstp.workflowstp.dto.WorkflowExecutionContext;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * Workflow functional internal component bean
 *
 * @author adiatullin
 */
public interface WorkflowWorker {
    String NAME = "wfstp_WorkflowWorker";

    /**
     * Determinate and get active workflow object which should be used for specified entity
     *
     * @param entity workflow entity
     * @return active workflow object which should be used for specified entity
     * @throws WorkflowException in case of any unexpected problems or if active workflow not found
     */
    Workflow determinateWorkflow(WorkflowEntity entity) throws WorkflowException;

    /**
     * Create and start workflow execution by provided entity and workflow object.
     * Workflow must be in active status and it's entityName must be equivalent entity class.
     *
     * @param entity entity object
     * @param wf     workflow object
     * @return ID of created workflow instance
     * @throws WorkflowException in case of any unexpected problems
     */
    UUID startWorkflow(WorkflowEntity entity, Workflow wf) throws WorkflowException;

    /**
     * Restart failed workflow instance
     *
     * @param instance workflow instance to restart
     * @throws WorkflowException in case of any unexpected problems
     */
    void restartWorkflow(WorkflowInstance instance) throws WorkflowException;

    /**
     * Fully reset of workflow instance
     *
     * @param instance reset workflow instance
     * @param wf       expecting workflow
     * @throws WorkflowException in case of any unexpected problems
     */
    void resetWorkflow(WorkflowInstance instance, Workflow wf) throws WorkflowException;

    /**
     * Recreate tasks if workflow process stopped
     *
     * @param instance worflow instance task
     * @throws WorkflowException in case of any unexpected problems
     */
    void recreateTasks(WorkflowInstance instance) throws WorkflowException;

    /**
     * Check is current workflow entity processing (processed) or not
     *
     * @param entity workflow entity
     * @return is current workflow entity processing (processed) or not
     */
    boolean isProcessing(WorkflowEntity entity);

    /**
     * Retrieve workflow reference of workflow entity
     *
     * @param entity workflow entity
     * @return current related with entity workflow
     */
    @Nullable
    Workflow getWorkflow(WorkflowEntity entity);

    /**
     * Load last active workflow instace by workflow entity
     *
     * @param entity workflow entity
     * @return active current entity workflow instance
     */
    @Nullable
    WorkflowInstance getWorkflowInstance(WorkflowEntity entity);

    /**
     * Load entity last processing workflow instance task by provided stage
     *
     * @param entity workflow entity
     * @param stage  current stage
     * @return processing workflow instance task
     */
    WorkflowInstanceTask getWorkflowInstanceTask(WorkflowEntity entity, Stage stage);

    /**
     * Load entity last processing (or processed) workflow instance task by current entity stage
     *
     * @param entity workflow entity
     * @return last processing(or processed) workflow instance task
     */
    @Nullable
    WorkflowInstanceTask getWorkflowInstanceTask(WorkflowEntity entity);

    /**
     * Load entity current processing stage
     *
     * @param entity workflow entity
     * @return entity current processing stage or null
     */
    @Nullable
    Stage getStage(WorkflowEntity entity);

    /**
     * Complete (if possible) provided workflow task and move workflow instance to the next step
     *
     * @param task workflow instance task
     * @throws WorkflowException in case of any unexpected problems
     */
    void finishTask(WorkflowInstanceTask task) throws WorkflowException;

    /**
     * Complete (if possible) provided workflow task with additional parameters which will be saved into workflow execution
     * and move workflow instance to the next step
     *
     * @param task   workflow instance task
     * @param params additional workflow execution parameters which must be saved
     * @throws WorkflowException in case of any unexpected problems
     */
    void finishTask(WorkflowInstanceTask task, @Nullable Map<String, String> params) throws WorkflowException;

    /**
     * Get workflow execution context
     *
     * @param instance workflow instance
     * @return execution context with parameters
     */
    WorkflowExecutionContext getExecutionContext(WorkflowInstance instance);

    /**
     * Save execution context into workflow instance
     *
     * @param context  execution context with parameters
     * @param instance workflow instance
     */
    void setExecutionContext(WorkflowExecutionContext context, WorkflowInstance instance);

    /**
     * Get execution parameter value by provided workflow instance and key
     *
     * @param instance workflow instance
     * @param key      parameter key
     * @return variable value
     */
    @Nullable
    String getParameter(WorkflowInstance instance, @Nullable String key);

    /**
     * Save execution parameter value into context workflow instance
     *
     * @param instance workflow instance
     * @param key      variable key
     * @param value    variable value
     */
    void setParameter(WorkflowInstance instance, @Nullable String key, @Nullable String value);
}
