package com.groupstp.workflowstp.web.workflowinstance.dialog;

import com.groupstp.workflowstp.entity.Workflow;
import com.groupstp.workflowstp.entity.WorkflowEntity;
import com.groupstp.workflowstp.exception.WorkflowException;
import com.groupstp.workflowstp.service.WorkflowService;
import com.groupstp.workflowstp.web.util.WorkflowInstanceHelper;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * This dialog show chooser of active workflow
 *
 * @author adiatullin
 */
public class WorkflowChooserDialog extends AbstractWindow {
    private static final Logger log = LoggerFactory.getLogger(WorkflowChooserDialog.class);

    public static final String SCREEN_ID = "workflow-chooser-dialog";

    private static final String ENTITY_NAME = "entity_name";
    private static final String ENTITY_ID = "entity_id";


    /**
     * Show workflow chooser dialog
     *
     * @param frame      calling UI frame
     * @param entityName expect entity name
     * @param entityId   expect entity id
     * @return opened WorkflowChooserDialog instance
     */
    public static WorkflowChooserDialog show(Frame frame, String entityName, String entityId) {
        Preconditions.checkNotEmptyString(entityName);
        Preconditions.checkNotEmptyString(entityId);

        return (WorkflowChooserDialog) frame.openWindow(SCREEN_ID, WindowManager.OpenType.DIALOG,
                ParamsMap.of(ENTITY_NAME, entityId, ENTITY_ID, entityId));
    }

    @Inject
    private WorkflowService workflowService;
    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;

    @Inject
    private LookupField workflowField;

    private WorkflowEntity entity;


    public Workflow getWorkflow() {
        return workflowField.getValue();
    }

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initEntity(params);
        initWorkflowField();
    }

    private void initEntity(Map<String, Object> params) {
        MetaClass metaClass = metadata.getClassNN((String) params.get(ENTITY_NAME));
        Object id = WorkflowInstanceHelper.parseEntityId((String) params.get(ENTITY_NAME), (String) params.get(ENTITY_ID));
        //noinspection unchecked
        WorkflowEntity entity = (WorkflowEntity) dataManager.load(metaClass.getJavaClass())
                .id(id)
                .view(View.LOCAL)
                .optional()
                .orElse(null);
        if (entity == null) {
            throw new RuntimeException(getMessage("workflowChooserDialog.entityNotFound"));
        }
        this.entity = entity;
    }

    private void initWorkflowField() {
        List<Workflow> options = dataManager.load(Workflow.class)
                .query("select e from wfstp$Workflow e where e.active = true and e.entityName = :entityName order by e.order")
                .parameter("entityName", entity.getMetaClass().getName())
                .view(View.MINIMAL)
                .list();
        if (!CollectionUtils.isEmpty(options)) {
            workflowField.setOptionsList(options);
        }
        workflowField.setNullOptionVisible(false);
        workflowField.setRequired(true);
    }

    public void onDeterminate() {
        try {
            workflowField.setValue(workflowService.determinateWorkflow(entity));
        } catch (WorkflowException e) {
            log.error("Failed to determinate workflow", e);
            throw new RuntimeException(getMessage("workflowChooserDialog.internalError"));
        }
    }

    public void onOk() {
        if (workflowField.getValue() != null) {
            close(COMMIT_ACTION_ID, true);
        } else {
            showNotification(messages.getMainMessage("validationFail.caption"), getMessage("workflowChooserDialog.pleaseSelectWorkflow"), NotificationType.TRAY);
        }
    }

    public void onCancel() {
        close(CLOSE_ACTION_ID, true);
    }
}
