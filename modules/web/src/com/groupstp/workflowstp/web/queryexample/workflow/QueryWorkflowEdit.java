package com.groupstp.workflowstp.web.queryexample.workflow;

import com.groupstp.workflowstp.dto.WorkflowExecutionContext;
import com.groupstp.workflowstp.entity.*;
import com.groupstp.workflowstp.service.WorkflowService;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QueryExample entity editor screen which using special for workflow extension
 *
 * @author adiatullin
 */
public class QueryWorkflowEdit extends AbstractEditor<QueryExample> {
    private static final Logger log = LoggerFactory.getLogger(QueryWorkflowEdit.class);

    public static final String EDITABLE = "editable";
    public static final String STAGE = "stage";
    public static final String WORKFLOW = "workflow";

    @Inject
    protected DataManager dataManager;
    @Inject
    protected Scripting scripting;
    @Inject
    protected Metadata metadata;
    @Inject
    protected WorkflowService service;
    @Inject
    protected UserSessionSource userSessionSource;

    @Inject
    protected FieldGroup fieldGroup;
    @Inject
    protected TabSheet tabsheet;
    @Inject
    protected ScrollBoxLayout scrollBox;
    @Inject
    protected DatasourceImplementation<QueryExample> queryDs;
    @Inject
    protected TextArea comment;

    @WindowParam(name = EDITABLE)
    protected Boolean editable;

    @WindowParam(name = STAGE)
    protected Stage stage;

    @WindowParam(name = WORKFLOW)
    protected Workflow workflow;

    protected WorkflowInstance workflowInstance;

    protected WorkflowInstanceTask workflowInstanceTask;


    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initLookupPickersFields();
    }

    private void initLookupPickersFields() {
        //cleanup open actions since in system we have combined screens only
        for (Component component : getComponents()) {
            if (component instanceof LookupPickerField) {
                ((LookupPickerField) component).removeAction(LookupPickerField.OpenAction.NAME);
            }
        }
    }

    @Override
    public void postInit() {
        super.postInit();

        if (!PersistenceHelper.isNew(getItem())) {
            setCaption(String.format(getMessage("queryWorkflowEdit.captionWithName"), getItem().getInstanceName()));
        } else {
            getItem().setInitiator(getInitiator());//set current user as initiator if possible
            queryDs.setModified(false);
        }

        initVisibility();
        initWorkflow();
    }

    private User getInitiator() {//get possible initiator by current user
        return userSessionSource.getUserSession().getUser();
    }

    //checkup editable fields
    protected void initVisibility() {
        if (Boolean.FALSE.equals(editable)) {//editing disabled
            for (FieldGroup.FieldConfig fieldConfig : fieldGroup.getFields()) {
                fieldConfig.setEditable(Boolean.FALSE);
            }

            comment.setEditable(false);
        }
    }

    protected void initWorkflow() {
        if (stage != null && workflow != null) {//this is screen of one of stage
            initWorkflowItems();

            if (StageType.USERS_INTERACTION.equals(stage.getType())) {//we need to extend screen by stage
                final String script = stage.getEditorScreenGroovyScript();
                if (StringUtils.isEmpty(script)) {
                    throw new DevelopmentException(String.format(getMessage("queryWorkflowEdit.editorScreenGroovyNotFound"), stage.getName()));
                }

                final Map<String, Object> binding = new HashMap<>();

                WorkflowExecutionContext ctx = service.getExecutionContext(workflowInstance);
                binding.put("entity", getItem());
                binding.put("context", ctx.getParams());
                binding.put("screen", this);
                binding.put("workflowInstance", workflowInstance);
                binding.put("workflowInstanceTask", workflowInstanceTask);
                try {
                    scripting.evaluateGroovy(script, binding);
                } catch (Exception e) {
                    log.error("Failed to evaluate editor screen groovy for workflow instance {}({}) and task {}({})",
                            workflowInstance, workflowInstance.getId(), workflowInstanceTask, workflowInstanceTask.getId());
                    throw new RuntimeException(getMessage("queryWorkflowEdit.errorOnScreenExtension"), e);
                }

                service.setExecutionContext(ctx, workflowInstance);//save parameters since they can be changed
            }
        }
    }

    //load and set workflow items
    protected void initWorkflowItems() {
        if (stage != null) {
            stage = dataManager.reload(stage, "stage-process");
            if (stage == null) {
                throw new DevelopmentException("Failed to find stage");
            }
            workflowInstance = findWorkflowInstance();
            if (workflowInstance == null) {
                throw new DevelopmentException("Failed to find workflow instance");
            }
            workflowInstanceTask = findWorkflowTask();
            if (workflowInstanceTask == null) {
                throw new DevelopmentException("Failed to find workflow instance task");
            }
        }
    }

    @Nullable
    protected WorkflowInstance findWorkflowInstance() {
        if (workflow != null) {
            String entityName = metadata.getClassNN(QueryExample.class).getName();
            String entityId = getItem().getId().toString();
            List<WorkflowInstance> list = dataManager.loadList(LoadContext.create(WorkflowInstance.class)
                    .setQuery(new LoadContext.Query("select e from wfstp$WorkflowInstance e where " +
                            "e.entityName = :entityName and e.entityId = :entityId and e.workflow.id = :workflowId and e.endDate is null " +
                            "order by e.createTs desc")
                            .setParameter("entityName", entityName)
                            .setParameter("entityId", entityId)
                            .setParameter("workflowId", workflow.getId()))
                    .setView("workflowInstance-process"));
            if (!CollectionUtils.isEmpty(list)) {
                if (list.size() > 1) {
                    log.warn(String.format("On workflow instance search for entityName (%s) and entityId (%s) " +
                            "found more than one workflow instance, last will be used", entityName, entityId));
                }
                return list.get(0);
            }
        }
        return null;
    }

    @Nullable
    protected WorkflowInstanceTask findWorkflowTask() {
        if (workflowInstance != null && stage != null) {
            List<WorkflowInstanceTask> list = dataManager.loadList(LoadContext.create(WorkflowInstanceTask.class)
                    .setQuery(new LoadContext.Query("select e from wfstp$WorkflowInstanceTask e " +
                            "join wfstp$Step s on e.step.id = s.id " +
                            "join wfstp$Stage ss on s.stage.id = ss.id " +
                            "where e.instance.id = :instanceId and ss.id = :stageId order by e.createTs desc")
                            .setParameter("instanceId", workflowInstance.getId())
                            .setParameter("stageId", stage.getId())
                            .setMaxResults(1))
                    .setView("workflowInstanceTask-process"));
            if (!CollectionUtils.isEmpty(list)) {
                return list.get(0);
            }
        }
        return null;
    }

    @Override
    public boolean preCommit() {
        if (super.preCommit()) {
            return true;
        }
        return false;
    }
}