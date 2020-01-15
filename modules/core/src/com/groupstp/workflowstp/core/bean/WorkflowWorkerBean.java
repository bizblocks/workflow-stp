package com.groupstp.workflowstp.core.bean;

import com.groupstp.workflowstp.bean.WorkflowSugarProcessor;
import com.groupstp.workflowstp.core.config.WorkflowConfig;
import com.groupstp.workflowstp.core.constant.WorkflowConstants;
import com.groupstp.workflowstp.core.util.JsonUtil;
import com.groupstp.workflowstp.data.impl.BaseWorkflowExecutionData;
import com.groupstp.workflowstp.entity.*;
import com.groupstp.workflowstp.event.WorkflowEvent;
import com.groupstp.workflowstp.exception.WorkflowException;
import com.groupstp.workflowstp.dto.WorkflowExecutionContext;
import com.groupstp.workflowstp.service.WorkflowExecutionDelegate;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.security.app.Authenticated;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base implementation of workflow functional bean
 *
 * @author adiatullin
 */
@Component(WorkflowWorker.NAME)
public class WorkflowWorkerBean extends MessageableBean implements WorkflowWorker {
    private static final Logger log = LoggerFactory.getLogger(WorkflowWorkerBean.class);

    @Inject
    protected DataManager dataManager;
    @Inject
    protected Metadata metadata;
    @Inject
    protected TimeSource timeSource;
    @Inject
    protected Scripting scripting;
    @Inject
    protected JsonUtil jsonUtil;
    @Inject
    protected Persistence persistence;
    @Inject
    protected Events events;
    @Inject
    protected UserSessionSource userSessionSource;
    @Inject
    protected WorkflowSugarProcessor sugar;

    @Inject
    protected WorkflowConfig config;

    /**
     * Current processing workflow instances by processing thread
     */
    protected final Map<UUID, Thread> processingInstances = new HashMap<>();
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    protected volatile int callCount = 0;


    @Override
    public Workflow determinateWorkflow(WorkflowEntity entity) throws WorkflowException {
        Preconditions.checkNotNullArgument(entity, getMessage("WorkflowWorkerBean.emptyEntity"));

        entity = reloadNN(entity, View.LOCAL);

        List<WorkflowDefinition> definitions = loadActiveDefinitions(entity.getMetaClass().getName());
        if (!CollectionUtils.isEmpty(definitions)) {
            for (WorkflowDefinition definition : definitions) {
                try {
                    if (isSatisfyDefinition(definition, entity)) {
                        log.debug("For entity '{}' will be used workflow '{}'", entity.getId(), definition.getWorkflow().getId());

                        return definition.getWorkflow();
                    }
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof WorkflowException) {
                        throw (WorkflowException) cause;
                    }
                    log.error(String.format("Failed to check workflow definition '%s'", definition.getId()), e);
                }
            }
        }
        throw new WorkflowException(getMessage("WorkflowWorkerBean.noActiveWorkflowDefinitions"));
    }

    protected List<WorkflowDefinition> loadActiveDefinitions(String entityName) {
        return dataManager.load(WorkflowDefinition.class)
                .query("select e from wfstp$WorkflowDefinition e where e.entityName = :entityName and e.workflow.active = true order by e.priority desc")
                .parameter("entityName", entityName)
                .view("workflowDefinition-determination")
                .list();
    }

    @Override
    public UUID startWorkflow(WorkflowEntity entity, Workflow wf) throws WorkflowException {
        Preconditions.checkNotNullArgument(entity, getMessage("WorkflowWorkerBean.emptyEntity"));
        Preconditions.checkNotNullArgument(wf, getMessage("WorkflowWorkerBean.emptyWorkflow"));

        WorkflowInstance instance;
        try {
            wf = reloadNN(wf, "workflow-process");
            if (!Boolean.TRUE.equals(wf.getActive())) {
                throw new WorkflowException(getMessage("WorkflowWorkerBean.workflowNotInActiveState"));
            }

            MetaClass metaClass = metadata.getClassNN(entity.getClass());

            Object entityId = entity.getId();
            if (entityId == null) {
                throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.incompatibleEntity"), metaClass.getName()));
            }

            if (!Objects.equals(metaClass.getName(), wf.getEntityName())) {
                throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.workflowIncompatible"),
                        wf.getEntityName(), metaClass.getName()));
            }

            instance = metadata.create(WorkflowInstance.class);
            instance.setWorkflow(wf);
            instance.setEntityName(metaClass.getName());
            instance.setEntityId(entityId.toString());
            instance.setStartDate(timeSource.currentTimestamp());

            if (entity.getWorkflow() != null) {
                throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.workflowEntityAlreadyProcessing"), entity.getId()));
            }
            entity.setStatus(WorkflowEntityStatus.IN_PROGRESS);
            entity.setWorkflow(wf);
            entity.setStepName(null);

            dataManager.commit(new CommitContext(entity, instance));

            log.info("Workflow {}({}) started for entity {}({}). Workflow instance created {}({})",
                    wf, wf.getId(), metaClass.getName(), entityId, instance, instance.getId());
        } catch (Exception e) {
            if (e instanceof WorkflowException) {
                throw e;
            }
            log.error("Workflow instance processing failed", e);
            throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.error"), e.getMessage()), e);
        }

        start(instance);

        return instance.getId();
    }

    @Override
    public void restartWorkflow(WorkflowInstance instance) throws WorkflowException {
        Preconditions.checkNotNullArgument(instance, getMessage("WorkflowWorkerBean.emptyWorkflowInstance"));

        forceAttach(instance);
        try {
            instance = reloadNN(instance, View.LOCAL);
            if (instance.getEndDate() == null) {
                log.warn("Trying to restart already running workflow process instance {}({})", instance, instance.getId());
                return;
            }

            if (StringUtils.isEmpty(instance.getError())) {
                log.warn("Trying to restart success finished workflow process instance {}({})", instance, instance.getId());
                return;
            }

            CommitContext toCommit = new CommitContext();

            instance.setError(null);
            instance.setEndDate(null);
            toCommit.addInstanceToCommit(instance);

            WorkflowEntity entity = getWorkflowEntity(instance);
            if (entity != null) {
                entity.setStatus(WorkflowEntityStatus.IN_PROGRESS);
                toCommit.addInstanceToCommit(entity);
            }

            if (Boolean.TRUE.equals(instance.getErrorInTask())) {
                WorkflowInstanceTask task = getLastTask(instance);
                if (task != null) {
                    task.setEndDate(null);
                    toCommit.addInstanceToCommit(task);
                }
            }

            dataManager.commit(toCommit);

            log.info("Workflow instance {}({}) restarted", instance, instance.getId());
        } catch (Exception e) {
            if (e instanceof WorkflowException) {
                throw e;
            }
            log.error("Workflow instance restarting failed", e);
            throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.failedToRestartWorkflowInstance"),
                    instance, instance.getId(), e.getMessage()));
        } finally {
            detach(instance);
        }

        start(instance);
    }

    @Override
    public void resetWorkflow(WorkflowInstance instance, Workflow wf) throws WorkflowException {
        Preconditions.checkNotNullArgument(instance, getMessage("WorkflowWorkerBean.emptyWorkflowInstance"));
        Preconditions.checkNotNullArgument(wf, getMessage("WorkflowWorkerBean.emptyWorkflow"));

        WorkflowEntity entity = getWorkflowEntity(instance);
        //noinspection ConstantConditions
        Preconditions.checkNotNullArgument(entity, getMessage("WorkflowWorkerBean.emptyEntity"));

        forceAttach(instance);
        try (Transaction tr = persistence.getTransaction()) {
            EntityManager em = persistence.getEntityManager();

            wf = em.reloadNN(wf, "workflow-process");
            if (!Boolean.TRUE.equals(wf.getActive())) {
                throw new WorkflowException(getMessage("WorkflowWorkerBean.workflowNotInActiveState"));
            }
            MetaClass metaClass = metadata.getClassNN(entity.getClass());
            if (!Objects.equals(metaClass.getName(), wf.getEntityName())) {
                throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.workflowIncompatible"),
                        wf.getEntityName(), metaClass.getName()));
            }

            instance = em.reloadNN(instance, "workflowInstance-process");
            instance.setWorkflow(wf);
            instance.setContext(null);

            List<WorkflowInstanceComment> comments = instance.getComments();
            if (!CollectionUtils.isEmpty(comments)) {
                for (WorkflowInstanceComment comment : comments) {
                    em.remove(comment);
                }
            }

            List<WorkflowInstanceTask> tasks = instance.getTasks();
            if (!CollectionUtils.isEmpty(tasks)) {
                for (WorkflowInstanceTask task : tasks) {
                    em.remove(task);
                }
            }

            instance.setStartDate(timeSource.currentTimestamp());
            instance.setEndDate(null);
            instance.setError(null);
            instance.setErrorInTask(null);

            entity = em.reloadNN(entity, View.LOCAL);
            entity.setStatus(WorkflowEntityStatus.IN_PROGRESS);
            entity.setWorkflow(wf);
            entity.setStepName(null);

            tr.commit();
        } catch (Exception e) {
            if (e instanceof WorkflowException) {
                throw e;
            }
            log.error("Workflow instance reset failed", e);
            throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.failedToResetWorkflowInstance"),
                    instance, instance.getId(), e.getMessage()));
        } finally {
            detach(instance);
        }

        start(instance);
    }

    @Override
    public void recreateTasks(WorkflowInstance instance) throws WorkflowException {
        Preconditions.checkNotNullArgument(instance, getMessage("WorkflowWorkerBean.emptyWorkflowInstance"));

        forceAttach(instance);
        try {
            //cleanup system parameter
            WorkflowExecutionContext ctx = getExecutionContext(instance);
            ctx.putParam(WorkflowConstants.REPEAT, null);
            ctx.putParam(WorkflowConstants.TIMEOUT, null);
            setExecutionContext(ctx, instance);
        } finally {
            detach(instance);
        }

        start(instance);
    }

    @Override
    public void moveWorkflow(WorkflowInstance instance, Step step) throws WorkflowException {
        Preconditions.checkNotNullArgument(instance, getMessage("WorkflowWorkerBean.emptyWorkflowInstance"));
        Preconditions.checkNotNullArgument(step, getMessage("WorkflowWorkerBean.emptyStep"));

        forceAttach(instance);
        try {
            try (Transaction tr = persistence.getTransaction()) {
                EntityManager em = persistence.getEntityManager();

                instance = em.reloadNN(instance);
                if (instance.getEndDate() != null && StringUtils.isEmpty(instance.getError())) {
                    log.warn("Trying to move finished workflow instance {}({})", instance, instance.getId());
                    return;
                }
                instance.setEndDate(null);
                instance.setError(null);
                instance.setErrorInTask(null);
                instance = em.merge(instance);

                Workflow wf = em.reloadNN(instance.getWorkflow());
                if (!Boolean.TRUE.equals(wf.getActive())) {
                    throw new WorkflowException(getMessage("WorkflowWorkerBean.workflowNotInActiveState"));
                }
                if (!wf.getSteps().contains(step)) {
                    throw new WorkflowException(getMessage("WorkflowWorkerBean.movementToUnknownStep"));
                }

                //cleanup system parameter
                WorkflowExecutionContext ctx = getExecutionContext(instance);
                ctx.putParam(WorkflowConstants.REPEAT, null);
                ctx.putParam(WorkflowConstants.TIMEOUT, null);
                setExecutionContext(ctx, instance);

                tr.commit();
            } catch (Exception e) {
                if (e instanceof WorkflowException) {
                    throw e;
                }
                log.error("Workflow instance movement failed", e);
                throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.failedToMoveWorkflowInstance"),
                        instance, instance.getId(), e.getMessage()));
            }

            CommitContext commitContext = new CommitContext();

            WorkflowInstanceTask lastTask = getLastTask(instance);
            if (lastTask != null) {
                //anyway need to complete the current last task to move to another
                lastTask.setEndDate(timeSource.currentTimestamp());
                commitContext.addInstanceToCommit(lastTask);
            }
            WorkflowInstanceTask task = metadata.create(WorkflowInstanceTask.class);
            task.setStartDate(timeSource.currentTimestamp());
            task.setInstance(instance);
            task.setStep(step);

            commitContext.addInstanceToCommit(task);

            dataManager.commit(commitContext);
        } finally {
            detach(instance);
        }

        start(instance);
    }

    @Override
    public boolean isProcessing(WorkflowEntity entity) {
        return getWorkflowInstance(entity) != null;
    }

    @Nullable
    @Override
    public Workflow getWorkflow(WorkflowEntity entity) {
        Preconditions.checkNotNullArgument(entity);
        return dataManager.load(Workflow.class)
                .query("select e.workflow from " + entity.getMetaClass().getName() + " e where e.id = :id")
                .parameter("id", entity.getId())
                .maxResults(1)
                .view(View.LOCAL)
                .optional()
                .orElse(null);
    }

    @Nullable
    @Override
    public WorkflowInstance getWorkflowInstance(WorkflowEntity entity) {
        return getWorkflowInstanceInternal(entity, true);
    }

    @Nullable
    @Override
    public WorkflowInstance getWorkflowInstanceIC(WorkflowEntity entity) {
        return getWorkflowInstanceInternal(entity, false);
    }

    protected WorkflowInstance getWorkflowInstanceInternal(WorkflowEntity entity, boolean active) {
        Preconditions.checkNotNullArgument(entity);

        Workflow workflow;
        if (!PersistenceHelper.isLoaded(entity, "workflow")) {
            workflow = getWorkflow(entity);
        } else {
            workflow = entity.getWorkflow();
        }

        if (workflow != null) {
            return dataManager.load(WorkflowInstance.class)
                    .query("select e from wfstp$WorkflowInstance e where " +
                            "e.entityName = :entityName and " +
                            "e.entityId = :entityId and " +
                            "e.workflow.id = :workflowId " +
                            (active ? "and e.endDate is null " : "") +
                            "order by e.createTs desc")
                    .parameter("entityName", entity.getMetaClass().getName())
                    .parameter("entityId", entity.getId().toString())
                    .parameter("workflowId", workflow.getId())
                    .maxResults(1)
                    .view("workflowInstance-process")
                    .optional()
                    .orElse(null);
        }
        return null;
    }

    @Nullable
    @Override
    public WorkflowInstanceTask getWorkflowInstanceTask(WorkflowEntity entity) {
        return getWorkflowInstanceTaskInternal(entity, true);
    }

    @Nullable
    @Override
    public WorkflowInstanceTask getWorkflowInstanceTaskIC(WorkflowEntity entity) {
        return getWorkflowInstanceTaskInternal(entity, false);
    }

    protected WorkflowInstanceTask getWorkflowInstanceTaskInternal(WorkflowEntity entity, boolean active) {
        Preconditions.checkNotNullArgument(entity);

        Workflow workflow;
        if (!PersistenceHelper.isLoaded(entity, "workflow")) {
            workflow = getWorkflow(entity);
        } else {
            workflow = entity.getWorkflow();
        }
        if (workflow != null) {
            return dataManager.load(WorkflowInstanceTask.class)
                    .query("select e from wfstp$WorkflowInstanceTask e " +
                            "join e.instance i where " +
                            "i.entityName = :entityName and " +
                            "i.entityId = :entityId and " +
                            "i.workflow.id = :workflowId " +
                            (active ? "and e.endDate is null " : "") +
                            "order by e.createTs desc")
                    .parameter("entityName", entity.getMetaClass().getName())
                    .parameter("entityId", entity.getId().toString())
                    .parameter("workflowId", workflow.getId())
                    .maxResults(1)
                    .view("workflowInstanceTask-detailed")
                    .optional()
                    .orElse(null);
        }
        return null;
    }

    @Override
    public WorkflowInstanceTask getWorkflowInstanceTaskNN(WorkflowEntity entity, Stage stage) {
        Preconditions.checkNotNullArgument(entity);
        Preconditions.checkNotNullArgument(stage);

        Workflow workflow;
        if (!PersistenceHelper.isLoaded(entity, "workflow")) {
            workflow = getWorkflow(entity);
        } else {
            workflow = entity.getWorkflow();
        }
        if (workflow == null) {
            throw new RuntimeException(getMessage("WorkflowWorkerBean.workflowNotActive"));
        }

        WorkflowInstanceTask task = dataManager.load(WorkflowInstanceTask.class)
                .query("select e from wfstp$WorkflowInstanceTask e " +
                        "join e.instance i " +
                        "join e.step s where " +
                        "i.entityName = :entityName and " +
                        "i.entityId = :entityId and " +
                        "i.workflow.id = :workflowId and " +
                        "s.stage.id = :stageId and " +
                        "s.workflow.id = :workflowId and " +
                        "e.endDate is null " +
                        " order by e.createTs desc")
                .parameter("entityName", entity.getMetaClass().getName())
                .parameter("entityId", entity.getId().toString())
                .parameter("workflowId", workflow.getId())
                .parameter("stageId", stage.getId())
                .maxResults(1)
                .view("workflowInstanceTask-detailed")
                .optional()
                .orElse(null);
        if (task != null) {
            if (!Boolean.TRUE.equals(task.getInstance().getWorkflow().getActive())) {
                throw new RuntimeException(getMessage("WorkflowWorkerBean.workflowNotActive"));
            }
            return task;
        }
        throw new RuntimeException(String.format(getMessage("WorkflowWorkerBean.taskAlreadyExecuted"), stage.getName(), entity.getInstanceName()));
    }

    @Nullable
    @Override
    public Stage getStage(WorkflowEntity entity) {
        if (!StringUtils.isEmpty(entity.getStepName())) {
            return dataManager.load(Stage.class)
                    .query("select e from wfstp$Stage e where " +
                            "e.entityName = :entityName and " +
                            "e.name = :name")
                    .parameter("entityName", entity.getMetaClass().getName())
                    .parameter("name", entity.getStepName())
                    .maxResults(1)
                    .view("stage-process")
                    .optional()
                    .orElse(null);
        }
        return null;
    }

    /**
     * Start execution of provided workflow instance
     *
     * @param instance workflow instance
     * @throws WorkflowException in case of any unexpected problems
     */
    protected void start(WorkflowInstance instance) throws WorkflowException {
        iterate(instance);//TODO maybe it is better call in scheduler to release calling thread?
    }

    /**
     * Check and run workflow by it's steps. Move instance into next steps by it's scheme
     *
     * @param instance workflow instance
     * @throws WorkflowException in case of any unexpected problems
     */
    protected void iterate(WorkflowInstance instance) throws WorkflowException {
        Preconditions.checkNotNullArgument(instance, getMessage("WorkflowWorkerBean.emptyWorkflowInstance"));

        if (!attach(instance)) {
            return;
        }

        WorkflowInstance originalInstance = instance;
        WorkflowEntity entity = null;

        try {
            instance = reloadNN(instance, "workflowInstance-process");

            log.debug("Iterating workflow instance {}({})", instance, instance.getId());

            if (instance.getError() != null) {
                log.debug("Workflow instance {}({}) marked as failed", instance, instance.getId());
                detach(instance);

                throw new WorkflowException(instance.getError());
            }
            if (instance.getEndDate() != null) {
                log.debug("Workflow instance {}({}) already finished", instance, instance.getId());
                detach(instance);
                return;
            }

            entity = getWorkflowEntity(instance);
            if (entity == null) {
                log.error("Entity for workflow instance {}({}) not found", instance, instance.getId());
                markAsFailed(instance, null, null, getMessage("WorkflowWorkerBean.entityNotFound"));
                throw new WorkflowException(getMessage("WorkflowWorkerBean.entityNotFound"));
            }

            if (!Boolean.TRUE.equals(instance.getWorkflow().getActive())) {
                log.error("Workflow instance {}({}) with inactive workflow", instance, instance.getId());
                markAsFailed(instance, entity, null, getMessage("WorkflowWorkerBean.workflowNotInActiveState"));
                throw new WorkflowException(getMessage("WorkflowWorkerBean.workflowNotInActiveState"));
            }

            WorkflowInstanceTask lastTask = getLastTask(instance);
            if (lastTask != null) {
                if (lastTask.getEndDate() != null) {//current task is done
                    Step step = lastTask.getStep();
                    if (!CollectionUtils.isEmpty(step.getDirections())) {
                        WorkflowExecutionContext context = null;
                        for (StepDirection direction : step.getDirections()) {
                            boolean satisfy;
                            try {
                                if (context == null) {
                                    context = getExecutionContext(instance);
                                }
                                satisfy = isSatisfyDirection(direction, instance, entity, context);
                            } catch (Exception e) {
                                markAsFailed(instance, entity, null, e);
                                if (e instanceof WorkflowException) {
                                    throw e;
                                }
                                throw new WorkflowException(
                                        String.format(getMessage("WorkflowWorkerBean.failedToEvaluateDirections"), e.getMessage()));
                            }
                            if (satisfy) {
                                createAndExecuteTask(direction.getTo(), instance, entity);
                                return;
                            }
                        }
                        WorkflowException e = new WorkflowException(
                                String.format(getMessage("WorkflowWorkerBean.noSuitableDirections"),
                                        instance, instance.getId(), step.getStage().getName(), lastTask.getId()));
                        markAsFailed(instance, entity, null, e);
                        throw e;
                    } else {//no directions - this is the last step of workflow
                        markAsDone(instance, entity);
                    }
                } else {//last task not done - execute it again
                    executeTask(lastTask, instance, entity, entity.getStepName());
                }
            } else {//no task find, this is first iteration call
                Step step = getStartStep(instance);
                if (step != null) {
                    createAndExecuteTask(step, instance, entity);
                } else {//no steps found just done this workflow
                    markAsDone(instance, entity);
                }
            }
        } catch (Exception e) {
            if (e instanceof WorkflowException) {
                throw e;
            }
            log.error(String.format("Failed to iterate workflow instance %s(%s)", originalInstance, originalInstance.getId()), e);
            markAsFailed(originalInstance, entity, null, e);

            throw new WorkflowException(getMessage("WorkflowWorkerBean.failedToFinishTask"));
        }
    }

    /**
     * Check what current direction are suitable to move
     */
    protected boolean isSatisfyDirection(StepDirection direction, WorkflowInstance instance,
                                         WorkflowEntity entity, WorkflowExecutionContext context) throws WorkflowException {
        if (!StringUtils.isEmpty(direction.getConditionGroovyScript())) {
            return checkDirectionByGroovy(direction, instance, entity, context);
        } else if (!StringUtils.isEmpty(direction.getConditionSqlScript())) {
            return checkDirectionBySql(direction, instance, entity);
        }
        return true;
    }

    protected boolean isSatisfyDefinition(WorkflowDefinition definition, WorkflowEntity entity) throws Exception {
        if (!StringUtils.isEmpty(definition.getConditionGroovyScript())) {
            return checkDefinitionByGroovy(definition, entity);
        } else if (!StringUtils.isEmpty(definition.getConditionSqlScript())) {
            return checkDefinitionBySql(definition, entity);
        }
        return true;
    }

    protected boolean checkDirectionByGroovy(StepDirection direction, WorkflowInstance instance,
                                             WorkflowEntity entity, WorkflowExecutionContext context) throws WorkflowException {
        try {
            final String script = prepareScript(direction.getConditionGroovyScript());
            final Map<String, Object> binding = new HashMap<>();
            binding.put("entity", entity);
            binding.put("context", context.getParams());
            binding.put("workflowInstance", instance);

            return Boolean.TRUE.equals(scripting.evaluateGroovy(script, binding));
        } catch (Exception e) {
            log.error(String.format("Failed to evaluate groovy condition direction from %s to %s of workflow instance %s (%s)",
                    direction.getFrom(), direction.getTo(), instance, instance.getId()), e);

            throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.failedToEvaluateGroovyCondition"),
                    direction.getFrom(), direction.getTo(), instance, instance.getId(), e.getMessage()), e);
        }
    }

    protected boolean checkDefinitionByGroovy(WorkflowDefinition definition, WorkflowEntity entity) {
        final String script = prepareScript(definition.getConditionGroovyScript());
        final Map<String, Object> binding = new HashMap<>();
        binding.put("entity", entity);

        return Boolean.TRUE.equals(scripting.evaluateGroovy(script, binding));
    }

    protected boolean checkDirectionBySql(StepDirection direction, WorkflowInstance instance, WorkflowEntity entity) throws WorkflowException {
        try {
            MetaClass metaClass = metadata.getClassNN(instance.getEntityName());

            QueryTransformer transformer = QueryTransformerFactory.createTransformer("select e from " + metaClass.getName() + " e");
            transformer.addWhere(direction.getConditionSqlScript());

            //noinspection unchecked
            List list = dataManager.loadList(LoadContext.create(metaClass.getJavaClass())
                    .setQuery(new LoadContext.Query(transformer.getResult() + " and e.id = :id")
                            .setParameter("id", entity.getId())
                            .setMaxResults(1))
                    .setView(View.MINIMAL));
            return !CollectionUtils.isEmpty(list);
        } catch (Exception e) {
            log.error(String.format("Failed to evaluate sql condition direction from %s to %s of workflow instance %s (%s)",
                    direction.getFrom(), direction.getTo(), instance, instance.getId()), e);

            throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.failedToEvaluateSqlCondition"),
                    direction.getFrom(), direction.getTo(), instance, instance.getId(), e.getMessage()), e);
        }
    }

    protected boolean checkDefinitionBySql(WorkflowDefinition definition, WorkflowEntity entity) {
        MetaClass metaClass = entity.getMetaClass();

        QueryTransformer transformer = QueryTransformerFactory.createTransformer("select e from " + metaClass.getName() + " e");
        transformer.addWhere(definition.getConditionSqlScript());

        //noinspection unchecked
        List list = dataManager.loadList(LoadContext.create(metaClass.getJavaClass())
                .setQuery(new LoadContext.Query(transformer.getResult() + " and e.id = :id")
                        .setParameter("id", entity.getId())
                        .setMaxResults(1))
                .setView(View.MINIMAL));
        return !CollectionUtils.isEmpty(list);
    }

    /**
     * Create new task for step and execute it
     */
    protected void createAndExecuteTask(Step step, WorkflowInstance instance, WorkflowEntity entity) throws WorkflowException {
        WorkflowInstanceTask task = metadata.create(WorkflowInstanceTask.class);
        task.setStartDate(timeSource.currentTimestamp());
        task.setInstance(instance);
        task.setStep(step);

        dataManager.commit(task);

        executeTask(task, instance, entity, entity.getStepName());
    }

    /**
     * Run workflow task logic, if task step contains algorithm execution, execute it intermediately
     */
    protected void executeTask(WorkflowInstanceTask task, WorkflowInstance instance, WorkflowEntity entity, @Nullable String previousStep) throws WorkflowException {
        boolean entityChanged = false;
        if (!WorkflowEntityStatus.IN_PROGRESS.equals(entity.getStatus())) {
            entity.setStatus(WorkflowEntityStatus.IN_PROGRESS);
            entityChanged = true;
        }
        if (!Objects.equals(task.getStep().getStage().getName(), entity.getStepName())) {
            entity.setStepName(task.getStep().getStage().getName());
            entityChanged = true;
        }
        if (entityChanged) {
            entity = dataManager.commit(entity);
        }

        if (isTimeout(task)) {
            fireEvent(entity, previousStep);

            Map<String, String> params = new HashMap<>();
            params.put(WorkflowConstants.TIMEOUT, task.getStep().getStage().getName());
            params.put(WorkflowConstants.REPEAT, null);
            finishTask(task, params, (Set<User>) null);

            return;
        }

        Stage stage = task.getStep().getStage();
        if (StageType.ALGORITHM_EXECUTION.equals(stage.getType())) {//can be executed automatically
            stage = reloadNN(stage, "stage-process");

            boolean success = true;
            if (!StringUtils.isEmpty(stage.getExecutionGroovyScript()) || !StringUtils.isEmpty(stage.getExecutionBeanName())) {
                try {
                    WorkflowExecutionContext context = getExecutionContext(instance);

                    if (isExecutable(context, task)) {
                        if (!StringUtils.isEmpty(stage.getExecutionBeanName())) {
                            WorkflowExecutionDelegate delegate = AppBeans.get(stage.getExecutionBeanName());
                            BaseWorkflowExecutionData data = new BaseWorkflowExecutionData(
                                    reloadNN(instance, View.LOCAL),
                                    reloadNN(task, View.LOCAL),
                                    reloadNN(entity, View.LOCAL),
                                    context);

                            success = delegate.execute(data);
                        } else {
                            final Map<String, Object> binding = new HashMap<>();
                            binding.put("entity", reloadNN(entity, View.LOCAL));
                            binding.put("context", context.getParams());
                            binding.put("workflowInstance", reloadNN(instance, View.LOCAL));
                            binding.put("workflowInstanceTask", reloadNN(task, View.LOCAL));

                            //if script returned true - this mean step successfully finished and we can move to the next stage
                            Object result = scripting.evaluateGroovy(prepareScript(stage.getExecutionGroovyScript()), binding);
                            if (result instanceof Boolean) {
                                success = Boolean.TRUE.equals(result);
                            }
                        }
                        if (success) {
                            context.putParam(WorkflowConstants.REPEAT, null);
                        } else {
                            //otherwise write the time of the execution
                            context.putParam(WorkflowConstants.REPEAT, Long.toString(timeSource.currentTimeMillis()));
                        }
                        //store context parameters
                        setExecutionContext(context, instance);
                    } else {
                        success = false;
                    }
                } catch (Exception e) {
                    log.error(String.format("Failed to evaluate groovy of workflow instance %s(%s) step %s (%s)",
                            instance, instance.getId(), stage.getName(), task.getId()), e);

                    markAsFailed(instance, entity, task, e);

                    if (e instanceof WorkflowException) {
                        throw (WorkflowException) e;
                    }

                    throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.errorInTask"),
                            stage.getName(), e.getMessage()), e);
                }
            }

            fireEvent(entity, previousStep);

            if (success) {
                finishTask(task, null, (Set<User>) null);
            } else {
                //re-execution will be performed in next workflow heartbeat to support timeout and repeat feature
                detach(instance);
            }
        } else if (StageType.ARCHIVE.equals(stage.getType())) {//this last archive node - mark it's as done and finish workflow
            fireEvent(entity, previousStep);

            finishTask(task, null, (Set<User>) null);
        } else if (StageType.USERS_INTERACTION.equals(stage.getType())) {
            fireEvent(entity, previousStep);
            //re-execution will be performed in next workflow heartbeat to support timeout feature
            detach(instance);
        } else {
            fireEvent(entity, previousStep);
        }
    }

    @Override
    public void finishTask(WorkflowInstanceTask task, String... performersLogin) throws WorkflowException {
        finishTask(task, null, performersLogin);
    }

    @Override
    public void finishTask(WorkflowInstanceTask task, @Nullable Map<String, String> params, String... performersLogin) throws WorkflowException {
        Set<User> performers;
        if (performersLogin != null && performersLogin.length > 0) {
            performers = new HashSet<>(performersLogin.length);
            for (String login : performersLogin) {
                if (!StringUtils.isBlank(login)) {
                    User user = dataManager.load(User.class)
                            .query("select e from sec$User e where e.loginLowerCase = :login")
                            .parameter("login", login.toLowerCase())
                            .view(View.MINIMAL)
                            .optional()
                            .orElse(null);
                    if (user == null) {
                        log.warn("User with login {} not found", login);
                    } else {
                        performers.add(user);
                    }
                }
            }
        } else {
            performers = Collections.singleton(userSessionSource.getUserSession().getUser());
        }
        finishTask(task, params, performers);
    }

    public void finishTask(WorkflowInstanceTask task, @Nullable Map<String, String> params, Set<User> performers) throws WorkflowException {
        Preconditions.checkNotNullArgument(task, getMessage("WorkflowWorkerBean.emptyWorkflowInstanceTask"));

        WorkflowInstance instance;
        try (Transaction tr = persistence.getTransaction()) {
            EntityManager em = persistence.getEntityManager();

            task = em.reloadNN(task, View.LOCAL);
            if (task.getEndDate() != null) {
                throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.workflowInstanceTaskAlreadyFinished"), task));
            }
            task.setEndDate(timeSource.currentTimestamp());
            task.setPerformers(performers);

            instance = task.getInstance();

            //cleanup variable directions
            String directionVariables = task.getStep().getStage().getDirectionVariables();
            if (!StringUtils.isEmpty(directionVariables)) {
                String[] variables = directionVariables.split(",");
                if (variables.length > 0) {
                    Map<String, String> parameters = new HashMap<>();
                    for (String variable : variables) {
                        parameters.put(variable, null);
                    }
                    if (params != null && params.size() > 0) {
                        parameters.putAll(params);
                    }
                    params = parameters;
                }
            }

            if (params != null && params.size() > 0) {
                WorkflowExecutionContext ctx = getExecutionContext(instance);
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    ctx.putParam(entry.getKey(), entry.getValue());
                }
                setExecutionContext(ctx, instance);
            }

            tr.commit();
        }

        iterate(instance);//move to the next step
    }

    /**
     * Workflow finished successful, mark it's as done and related entity
     */
    protected void markAsDone(WorkflowInstance instance, @Nullable WorkflowEntity entity) throws WorkflowException {
        try (Transaction tr = persistence.getTransaction()) {
            EntityManager em = persistence.getEntityManager();

            instance = em.reloadNN(instance, View.LOCAL);
            instance.setEndDate(timeSource.currentTimestamp());

            if (entity != null) {
                entity = em.reloadNN(entity, View.LOCAL);
                //entity.setStepName(null); keep last step name
                entity.setStatus(WorkflowEntityStatus.DONE);
            }

            tr.commit();
        } catch (Exception e) {
            log.error(String.format("Failed to mark as done workflow instance %s (%s)", instance, instance.getId()), e);
            throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.failedToDoneWorkflow"), instance, instance.getId()), e);
        } finally {
            detach(instance);
        }
    }

    /**
     * Workflow finished unsuccessful, mark it as failed and all related entities
     */
    protected void markAsFailed(WorkflowInstance instance, @Nullable WorkflowEntity entity,
                                @Nullable WorkflowInstanceTask task, @Nullable Exception e) throws WorkflowException {
        markAsFailed(instance, entity, task, e == null ? null : ExceptionUtils.getStackTrace(e));
    }

    protected void markAsFailed(WorkflowInstance instance, @Nullable WorkflowEntity entity,
                                @Nullable WorkflowInstanceTask task, @Nullable String error) throws WorkflowException {
        try (Transaction tr = persistence.getTransaction()) {
            EntityManager em = persistence.getEntityManager();

            instance = em.reloadNN(instance, View.LOCAL);
            instance.setEndDate(timeSource.currentTimestamp());
            instance.setError(StringUtils.isEmpty(error) ? getMessage("WorkflowWorkerBean.internalServerError") : error);
            instance.setErrorInTask(task != null);

            if (entity != null) {
                entity = em.reloadNN(entity, View.LOCAL);
                entity.setStatus(WorkflowEntityStatus.FAILED);
            }

            if (task != null) {
                task = em.reloadNN(task, View.LOCAL);
                task.setEndDate(timeSource.currentTimestamp());
            }

            tr.commit();
        } catch (Exception e) {
            log.error(String.format("Failed to mark as failed workflow instance %s (%s). Original error %s",
                    instance, instance.getId(), error), e);
            throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.failedToWriteException"), instance, instance.getId()), e);
        } finally {
            detach(instance);
        }
    }

    @Override
    public WorkflowExecutionContext getExecutionContext(WorkflowInstance instance) {
        Preconditions.checkNotNullArgument(instance, getMessage("WorkflowWorkerBean.emptyWorkflowInstance"));

        try (Transaction tr = persistence.getTransaction()) {
            EntityManager em = persistence.getEntityManager();
            instance = em.reloadNN(instance, View.LOCAL);

            WorkflowExecutionContext ctx;
            if (!StringUtils.isEmpty(instance.getContext())) {
                ctx = jsonUtil.fromJson(instance.getContext(), WorkflowExecutionContext.class);
            } else {
                ctx = new WorkflowExecutionContext();
            }

            tr.commit();

            return ctx;
        }
    }

    @Override
    public void setExecutionContext(WorkflowExecutionContext context, WorkflowInstance instance) {
        Preconditions.checkNotNullArgument(instance, getMessage("WorkflowWorkerBean.emptyWorkflowInstance"));

        try (Transaction tr = persistence.getTransaction()) {
            EntityManager em = persistence.getEntityManager();
            instance = em.reloadNN(instance, View.LOCAL);

            String text = context == null ? null : jsonUtil.toJson(context);
            if (!Objects.equals(instance.getContext(), text)) {
                instance.setContext(text);
            }

            tr.commit();
        }
    }

    @Nullable
    @Override
    public String getParameter(WorkflowInstance instance, @Nullable String key) {
        Preconditions.checkNotNullArgument(instance, getMessage("WorkflowWorkerBean.emptyWorkflowInstance"));
        return getExecutionContext(instance).getParam(key);
    }

    @Override
    public void setParameter(WorkflowInstance instance, @Nullable String key, @Nullable String value) {
        Preconditions.checkNotNullArgument(instance, getMessage("WorkflowWorkerBean.emptyWorkflowInstance"));
        try (Transaction tr = persistence.getTransaction()) {
            WorkflowExecutionContext ctx = getExecutionContext(instance);
            ctx.putParam(key, value);
            setExecutionContext(ctx, instance);

            tr.commit();
        }
    }

    @Authenticated
    @Override
    public void performWorkflowHeartbeat() {
        if (Boolean.TRUE.equals(config.getHeartbeatEnable())) {

            Integer callsToSkip = config.getDelayCallCount();
            if (callsToSkip != null) {
                if (callCount < callsToSkip) {
                    callCount++;
                    return;
                }
            }

            Set<UUID> processing;

            ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
            readLock.lock();
            try {
                processing = processingInstances.keySet();
            } finally {
                readLock.unlock();
            }

            List<WorkflowInstance> notFinished = getNotFinishedWorkflowInstances(processing);
            for (WorkflowInstance instance : notFinished) {
                try {
                    iterate(instance);
                } catch (WorkflowException e) {
                    log.warn("Failed to restart workflow instance from heartbeat");
                }
            }
        }
    }

    //utils

    /**
     * Retrieve from database last processing task by workflow instance
     */
    @Nullable
    protected WorkflowInstanceTask getLastTask(WorkflowInstance instance) {
        List<WorkflowInstanceTask> list = dataManager.loadList(LoadContext.create(WorkflowInstanceTask.class)
                .setQuery(new LoadContext.Query("select e from wfstp$WorkflowInstanceTask e where e.instance.id = :instanceId " +
                        "order by e.createTs desc")
                        .setParameter("instanceId", instance.getId())
                        .setMaxResults(1))
                .setView("workflowInstanceTask-process"));
        if (!CollectionUtils.isEmpty(list)) {
            return list.get(0);
        }
        return null;
    }

    protected Step getStartStep(WorkflowInstance instance) {
        List<Step> list = dataManager.loadList(LoadContext.create(Step.class)
                .setQuery(new LoadContext.Query("select s from wfstp$WorkflowInstance i " +
                        "join i.workflow w " +
                        "join w.steps s " +
                        "where i.id = :instanceId and s.start = true")
                        .setParameter("instanceId", instance.getId())
                        .setMaxResults(1))
                .setView("step-process"));
        if (!CollectionUtils.isEmpty(list)) {
            return list.get(0);
        }
        return getFirstStep(instance);
    }

    /**
     * Retrieve from database first step by workflow instance
     */
    @Nullable
    protected Step getFirstStep(WorkflowInstance instance) {
        List<Step> list = dataManager.loadList(LoadContext.create(Step.class)
                .setQuery(new LoadContext.Query("select s from wfstp$WorkflowInstance i " +
                        "join i.workflow w " +
                        "join w.steps s " +
                        "where i.id = :instanceId order by s.order asc")
                        .setParameter("instanceId", instance.getId())
                        .setMaxResults(1))
                .setView("step-process"));
        if (!CollectionUtils.isEmpty(list)) {
            return list.get(0);
        }
        return null;
    }

    /**
     * Retrieve from database workflow related entity
     */
    @Nullable
    protected WorkflowEntity getWorkflowEntity(WorkflowInstance instance) {
        if (instance != null) {
            MetaClass metaClass = metadata.getClassNN(instance.getEntityName());
            Object id = parseEntityId(metaClass, instance.getEntityId());
            //noinspection unchecked
            List list = dataManager.loadList(LoadContext.create(metaClass.getJavaClass()).setId(id).setView(View.LOCAL));
            if (!CollectionUtils.isEmpty(list)) {
                return (WorkflowEntity) list.get(0);
            }
        }
        return null;
    }

    /**
     * Retrieve all not finished workflow instances
     */
    protected List<WorkflowInstance> getNotFinishedWorkflowInstances(Set<UUID> processingIds) {
        return dataManager.load(WorkflowInstance.class)
                .query("select e from wfstp$WorkflowInstance e where e.endDate is null and e.error is null and e.id not in :ids")
                .parameter("ids", processingIds)
                .view(View.MINIMAL)
                .list();
    }

    /**
     * Check is provided task are timeout or not
     *
     * @param task current task
     * @return is task timeout
     */
    protected boolean isTimeout(WorkflowInstanceTask task) {
        Integer timeoutSec = task.getStep().getTimeoutSec();
        if (timeoutSec != null && timeoutSec > 0) {
            long start = task.getStartDate().getTime();
            long now = timeSource.currentTimeMillis();
            long diff = now - start;
            if (diff >= (timeoutSec * 1000)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check is can provided task be executed or not
     *
     * @param ctx  workflow instance execution context
     * @param task performing task
     * @return can task be executed right now
     */
    protected boolean isExecutable(WorkflowExecutionContext ctx, WorkflowInstanceTask task) {
        Integer repeatSec = task.getStep().getRepeatSec();
        if (repeatSec != null && repeatSec > 0) {
            String lastExecuteText = ctx.getParam(WorkflowConstants.REPEAT);
            if (!StringUtils.isEmpty(lastExecuteText)) {
                long lastExecute = Long.valueOf(lastExecuteText);
                long now = timeSource.currentTimeMillis();
                long diff = now - lastExecute;
                if (diff < (repeatSec * 1000)) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean attach(WorkflowInstance instance) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            Thread processingThread = processingInstances.get(instance.getId());
            if (Objects.equals(processingThread, Thread.currentThread())) {
                return true;
            }

            if (processingThread == null || !processingThread.isAlive()) {
                processingInstances.put(instance.getId(), Thread.currentThread());
                return true;
            }

            return false;
        } finally {
            writeLock.unlock();
        }
    }

    protected void forceAttach(WorkflowInstance instance) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            processingInstances.put(instance.getId(), Thread.currentThread());
            //TODO in perfect world need to intermediately stop execution of another thread
        } finally {
            writeLock.unlock();
        }
    }

    protected void detach(WorkflowInstance instance) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            processingInstances.remove(instance.getId());
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Reloading entity with provided view
     */
    protected <T extends Entity> T reloadNN(T entity, String view) {
        entity = dataManager.reload(entity, view);
        if (entity == null) {
            throw new RuntimeException(String.format("Failed to reload entity by view %s", view));
        }
        return entity;
    }

    protected String prepareScript(String script) {
        return sugar.prepareScript(script);
    }

    /**
     * Parse entity ID to correct java object
     */
    @Nullable
    protected Object parseEntityId(MetaClass metaClass, String entityId) {
        if (!StringUtils.isEmpty(entityId)) {
            MetaProperty idProperty = metaClass.getPropertyNN("id");
            Class idClass = idProperty.getJavaType();
            if (UUID.class.isAssignableFrom(idClass)) {
                return UuidProvider.fromString(entityId);
            } else if (Integer.class.isAssignableFrom(idClass)) {
                return Integer.valueOf(entityId);
            } else if (Long.class.isAssignableFrom(idClass)) {
                return Long.valueOf(entityId);
            } else if (String.class.isAssignableFrom(idClass)) {
                return entityId;
            } else {
                throw new UnsupportedOperationException(String.format("Unknown entity '%s' id type '%s'", metaClass.getName(), entityId));
            }
        }
        return null;
    }

    protected void fireEvent(WorkflowEntity entity, @Nullable String previousStep) {
        String id = entity.getId().toString();
        Class idClass = entity.getId().getClass();

        if (!Objects.equals(entity.getStepName(), previousStep)) {
            WorkflowEvent event = new WorkflowEvent(entity.getClass(), id, idClass, entity.getStepName(), previousStep);
            events.publish(event);
        }
    }
}
