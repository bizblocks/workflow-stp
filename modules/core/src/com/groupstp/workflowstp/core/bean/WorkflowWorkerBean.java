package com.groupstp.workflowstp.core.bean;

import com.groupstp.workflowstp.core.config.WorkflowConfig;
import com.groupstp.workflowstp.core.util.JsonUtil;
import com.groupstp.workflowstp.entity.*;
import com.groupstp.workflowstp.event.WorkflowEvent;
import com.groupstp.workflowstp.exception.WorkflowException;
import com.groupstp.workflowstp.dto.WorkflowExecutionContext;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

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
    protected WorkflowConfig config;


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
        }

        start(instance);
    }

    @Override
    public void recreateTasks(WorkflowInstance instance) throws WorkflowException {
        start(instance);
    }

    @Override
    public WorkflowInstanceTask loadLastProcessingTask(WorkflowEntity entity, Stage stage) {
        List<WorkflowInstanceTask> list = dataManager.loadList(LoadContext.create(WorkflowInstanceTask.class)
                .setQuery(new LoadContext.Query("select e from wfstp$WorkflowInstanceTask e " +
                        "join e.instance i " +
                        "join e.step s " +
                        "where i.workflow.id = :workflowId and i.entityId = :entityId and s.stage.id = :stageId and s.workflow.id = :workflowId " +
                        "and e.endDate is null order by e.createTs desc")
                        .setParameter("workflowId", entity.getWorkflow().getId())
                        .setParameter("entityId", entity.getId().toString())
                        .setParameter("stageId", stage.getId())
                        .setMaxResults(1))
                .setView("workflowInstanceTask-browse"));
        if (!CollectionUtils.isEmpty(list)) {
            WorkflowInstanceTask task = list.get(0);
            if (!Boolean.TRUE.equals(task.getInstance().getWorkflow().getActive())) {
                throw new RuntimeException(getMessage("WorkflowWorkerBean.workflowNotActive"));
            }
            return task;
        }
        throw new RuntimeException(String.format(getMessage("WorkflowWorkerBean.taskAlreadyExecuted"), stage.getName(), entity.getInstanceName()));
    }

    @Nullable
    @Override
    public WorkflowInstanceTask loadLastTask(WorkflowEntity entity) {
        WorkflowInstance instance = loadActiveWorkflowInstance(entity);
        if (instance != null) {
            return getLastTask(instance);
        }
        return null;
    }

    @Nullable
    @Override
    public WorkflowInstance loadActiveWorkflowInstance(WorkflowEntity entity) {
        return dataManager.load(LoadContext.create(WorkflowInstance.class)
                .setQuery(new LoadContext.Query("select e from wfstp$WorkflowInstance e " +
                        "where e.workflow.id = :workflowId and e.entityId = :entityId")
                        .setParameter("workflowId", entity.getWorkflow().getId())
                        .setParameter("entityId", entity.getId().toString()))
                .setView(View.MINIMAL));
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

        WorkflowInstance originalInstance = instance;
        WorkflowEntity entity = null;

        try {
            instance = reloadNN(instance, "workflowInstance-process");

            log.debug("Iterating workflow instance {}({})", instance, instance.getId());

            if (instance.getEndDate() != null) {
                log.debug("Workflow instance {}({}) already finished", instance, instance.getId());
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
            final String script = direction.getConditionGroovyScript();
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
        final String script = definition.getConditionGroovyScript();
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

        String previousStep = entity.getStepName();
        entity.setStatus(WorkflowEntityStatus.IN_PROGRESS);
        entity.setStepName(step.getStage().getName());

        dataManager.commit(new CommitContext(task, entity));

        executeTask(task, instance, entity, previousStep);
    }

    /**
     * Run workflow task logic, if task step contains algoritm execution, execute it intermediately
     */
    protected void executeTask(WorkflowInstanceTask task, WorkflowInstance instance, WorkflowEntity entity, @Nullable String previousStep) throws WorkflowException {
        Stage stage = task.getStep().getStage();
        if (StageType.ALGORITHM_EXECUTION.equals(stage.getType())) {//can be executed automatically
            stage = reloadNN(stage, "stage-process");

            boolean executed = true;
            if (!StringUtils.isEmpty(stage.getExecutionGroovyScript())) {
                try {
                    final String script = stage.getExecutionGroovyScript();

                    final Map<String, Object> binding = new HashMap<>();
                    WorkflowExecutionContext context = getExecutionContext(instance);
                    binding.put("entity", reloadNN(entity, View.LOCAL));
                    binding.put("context", context.getParams());
                    binding.put("workflowInstance", reloadNN(instance, View.LOCAL));
                    binding.put("workflowInstanceTask", reloadNN(task, View.LOCAL));

                    //if script returned true - this mean step successfully finished and we can move to the next stage
                    executed = Boolean.TRUE.equals(scripting.evaluateGroovy(script, binding));
                    if (executed) {
                        setExecutionContext(context, instance);
                    }
                } catch (Exception e) {
                    log.error(String.format("Failed to evaluate groovy of workflow instance %s(%s) step %s (%s)",
                            instance, instance.getId(), stage.getName(), task.getId()), e);

                    markAsFailed(instance, entity, task, e);

                    throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.errorInTask"),
                            stage.getName(), e.getMessage()), e);
                }
            }

            if (executed) {
                fireEvent(entity, previousStep);

                finishTask(task, null, null);
            }
        } else if (StageType.ARCHIVE.equals(stage.getType())) {//this last archive node - mark it's as done and finish workflow
            fireEvent(entity, previousStep);

            finishTask(task, null, null);
        } else {
            fireEvent(entity, previousStep);
        }
    }


    @Override
    public void finishTask(WorkflowInstanceTask task) throws WorkflowException {
        finishTask(task, null);
    }

    @Override
    public void finishTask(WorkflowInstanceTask task, @Nullable Map<String, String> params) throws WorkflowException {
        finishTask(task, params, userSessionSource.getUserSession().getUser());
    }

    public void finishTask(WorkflowInstanceTask task, @Nullable Map<String, String> params, @Nullable User performer) throws WorkflowException {
        Preconditions.checkNotNullArgument(task, getMessage("WorkflowWorkerBean.emptyWorkflowInstanceTask"));

        WorkflowInstance instance;
        try (Transaction tr = persistence.getTransaction()) {
            EntityManager em = persistence.getEntityManager();

            task = em.reloadNN(task, View.LOCAL);
            if (task.getEndDate() != null) {
                throw new WorkflowException(String.format(getMessage("WorkflowWorkerBean.workflowInstanceTaskAlreadyFinished"), task));
            }
            task.setEndDate(timeSource.currentTimestamp());
            task.setPerformer(performer);

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
        }
    }

    /**
     * Workflow finished unsuccessful, mark it as failed and all related entities
     */
    protected void markAsFailed(WorkflowInstance instance, @Nullable WorkflowEntity entity,
                                @Nullable WorkflowInstanceTask task, @Nullable Exception e) throws WorkflowException {
        markAsFailed(instance, entity, task, e == null ? null : ExceptionUtils.getFullStackTrace(e));
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
     * Reloading entity with provided view
     */
    protected <T extends Entity> T reloadNN(T entity, String view) {
        entity = dataManager.reload(entity, view);
        if (entity == null) {
            throw new RuntimeException(String.format("Failed to reload entity by view %s", view));
        }
        return entity;
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
