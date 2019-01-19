package com.groupstp.workflowstp.web.workflowinstance;

import com.groupstp.workflowstp.entity.WorkflowInstanceComment;
import com.groupstp.workflowstp.entity.WorkflowInstanceTask;
import com.groupstp.workflowstp.exception.WorkflowException;
import com.groupstp.workflowstp.service.WorkflowService;
import com.groupstp.workflowstp.web.util.WebUiHelper;
import com.groupstp.workflowstp.web.util.WorkflowInstanceHelper;
import com.groupstp.workflowstp.web.util.messagedialog.MessageDialog;
import com.groupstp.workflowstp.web.workflowinstance.dialog.WorkflowChooserDialog;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.app.core.file.FileDownloadHelper;
import com.haulmont.cuba.gui.components.*;
import com.groupstp.workflowstp.entity.WorkflowInstance;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * @author adiatullin
 */
public class WorkflowInstanceEdit extends AbstractEditor<WorkflowInstance> {
    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private WorkflowService workflowService;

    @Inject
    private PickerField workflow;
    @Inject
    private LinkButton contextLink;
    @Inject
    private LinkButton relatedEntityLink;
    @Inject
    private LinkButton errorLink;
    @Inject
    private FieldGroup generalFieldGroup;
    @Inject
    private Table<WorkflowInstanceComment> commentsTable;
    @Inject
    private CollectionDatasource<WorkflowInstanceTask, UUID> tasksDs;
    @Inject
    private Button recreateTaskBtn;
    @Inject
    private Button resetBtn;


    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initWorkflowLookup();
        initContextLookup();
        initCommentsTable();
        initReset();

        generalFieldGroup.setEditable(false);
    }

    //setup workflow link
    private void initWorkflowLookup() {
        workflow.removeAllActions();
        workflow.addOpenAction();
        workflow.setFieldEditable(false);
    }

    //setup context parameters link
    private void initContextLookup() {
        contextLink.setAction(new BaseAction("contextLink") {
            @Override
            public void actionPerform(Component component) {
                final MessageDialog dialog = MessageDialog.showText(WorkflowInstanceEdit.this, getItem().getContext(), true);
                dialog.addCloseWithCommitListener(() -> getItem().setContext(dialog.getMessage()));
            }
        });
    }

    //setup comments table
    private void initCommentsTable() {
        FileDownloadHelper.initGeneratedColumn(commentsTable, "attachment");
        WebUiHelper.showLinkOnTable(commentsTable, "author");
    }

    private void initReset() {
        resetBtn.setAction(new AbstractAction("resetInstance") {
            @Override
            public void actionPerform(Component component) {
                Action yes = new DialogAction(DialogAction.Type.YES, Status.PRIMARY).withHandler(event -> {
                    WorkflowChooserDialog dialog = WorkflowChooserDialog.show(WorkflowInstanceEdit.this, getItem().getEntityName(), getItem().getEntityId());
                    dialog.addCloseWithCommitListener(() -> {
                        try {
                            workflowService.resetWorkflow(getItem(), dialog.getWorkflow());
                            getDsContext().refresh();
                            postInit();
                        } catch (WorkflowException e) {
                            throw new RuntimeException("Failed to reset workflow instance", e);
                        }
                    });
                });
                Action no = new DialogAction(DialogAction.Type.NO);
                showOptionDialog(
                        getMessage("workflowInstanceEdit.warning"),
                        getMessage("workflowInstanceEdit.resetDescription"),
                        MessageType.CONFIRMATION,
                        new Action[]{yes, no});
            }
        });
    }

    @Override
    public void postInit() {
        super.postInit();

        initRelatedEntityLookup();
        initErrorLookup();
        initRecreateTasks();
    }

    private void initRelatedEntityLookup() {
        final String entityName = getItem().getEntityName();
        final Object entityId = WorkflowInstanceHelper.parseEntityId(entityName, getItem().getEntityId());

        relatedEntityLink.setAction(new BaseAction("entityLink") {

            @Override
            public void actionPerform(Component component) {
                openEditor(reload(), WindowManager.OpenType.THIS_TAB);
            }

            private Entity reload() {
                //noinspection unchecked
                return dataManager.load(LoadContext.create(metadata.getClassNN(entityName).getJavaClass())
                        .setId(entityId)
                        .setView(View.MINIMAL));
            }
        });
    }

    private void initErrorLookup() {
        final String errorText = getItem().getError();
        if (!StringUtils.isEmpty(errorText)) {
            errorLink.setAction(new BaseAction("errorLink") {
                @Override
                public void actionPerform(Component component) {
                    MessageDialog.showText(WorkflowInstanceEdit.this, errorText);
                }
            });
        } else {
            errorLink.setEnabled(false);
        }
    }

    private void initRecreateTasks() {
        recreateTaskBtn.setAction(new AbstractAction("recreateTask") {
            @Override
            public void actionPerform(Component component) {
                Action yes = new DialogAction(DialogAction.Type.YES, Status.PRIMARY).withHandler(event -> {
                    try {
                        workflowService.recreateTasks(getItem());
                    } catch (WorkflowException e) {
                        throw new RuntimeException("Failed to recreate tasks", e);
                    }
                });
                Action no = new DialogAction(DialogAction.Type.NO);
                showOptionDialog(
                        getMessage("workflowInstanceEdit.warning"),
                        getMessage("workflowInstanceEdit.recreateTasksDescription"),
                        MessageType.CONFIRMATION,
                        new Action[]{yes, no});
            }
        });
        recreateTaskBtn.setEnabled(getItem().getEndDate() == null && StringUtils.isEmpty(getItem().getError()) && isAllTasksFinished());
    }

    private boolean isAllTasksFinished() {
        Collection<WorkflowInstanceTask> tasks = tasksDs.getItems();
        if (!CollectionUtils.isEmpty(tasks)) {
            for (WorkflowInstanceTask task : tasks) {
                if (task.getEndDate() == null) {
                    return false;
                }
            }
        }
        return true;
    }
}