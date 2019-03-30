package com.groupstp.workflowstp.web.activity;

import com.groupstp.workflowstp.entity.Workflow;
import com.groupstp.workflowstp.entity.WorkflowInstance;
import com.groupstp.workflowstp.entity.WorkflowInstanceTask;
import com.groupstp.workflowstp.exception.WorkflowException;
import com.groupstp.workflowstp.service.WorkflowService;
import com.groupstp.workflowstp.web.util.WebUiHelper;
import com.groupstp.workflowstp.web.util.WorkflowInstanceHelper;
import com.groupstp.workflowstp.web.workflowinstance.dialog.WorkflowChooserDialog;
import com.groupstp.workflowstp.web.workflowinstance.dialog.WorkflowStepChooserDialog;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Special management screen to control all activities of workflow processes
 *
 * @author adiatullin
 */
public class ActivityScreen extends AbstractLookup {

    private static final String SCREEN_ID = "activities-screen";

    /**
     * Show main workflow activities screen to user
     *
     * @param frame calling UI frame
     */
    public static void show(Frame frame) {
        Preconditions.checkNotNullArgument(frame);

        frame.openWindow(SCREEN_ID, WindowManager.OpenType.THIS_TAB);
    }

    @Inject
    protected DataManager dataManager;
    @Inject
    protected Metadata metadata;
    @Inject
    protected WorkflowService workflowService;

    @Inject
    protected GroupTable<WorkflowInstanceTask> activitiesTable;


    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        WebUiHelper.showLinkOnTable(activitiesTable, "instance");
        initTable();
    }

    protected void initTable() {
        initViewAction();
        initOpenRelatedEntityAction();
        initMoveAction();
        initExecuteAction();
        initRestartAction();
        initResetAction();
    }

    protected void initViewAction() {
        activitiesTable.addAction(new BaseAction("view") {
            @Override
            public void actionPerform(Component component) {
                WorkflowInstance instance = activitiesTable.getSingleSelected() == null ? null : activitiesTable.getSingleSelected().getInstance();
                if (instance != null) {
                    AbstractEditor editor = openEditor(instance, WindowManager.OpenType.THIS_TAB);
                    editor.addCloseListener(actionId -> activitiesTable.getDatasource().refresh());
                }
            }

            @Override
            public boolean isPermitted() {
                if (super.isPermitted()) {
                    Set selected = activitiesTable.getSelected();
                    return !CollectionUtils.isEmpty(selected) && selected.size() == 1;
                }
                return false;
            }
        });
    }

    protected void initOpenRelatedEntityAction() {
        activitiesTable.addAction(new BaseAction("openRelatedEntity") {
            @Override
            public void actionPerform(Component component) {
                WorkflowInstance instance = activitiesTable.getSingleSelected() == null ? null : activitiesTable.getSingleSelected().getInstance();
                if (instance != null) {
                    AbstractEditor editor = openEditor(reload(instance.getEntityName(), instance.getEntityId()), WindowManager.OpenType.THIS_TAB);
                    editor.addCloseListener(actionId -> activitiesTable.getDatasource().refresh());
                }
            }

            private Entity reload(String name, String id) {
                //noinspection unchecked
                return dataManager.load(LoadContext.create(metadata.getClassNN(name).getJavaClass())
                        .setId(WorkflowInstanceHelper.parseEntityId(name, id))
                        .setView(View.MINIMAL));
            }

            @Override
            public boolean isPermitted() {
                if (super.isPermitted()) {
                    Set selected = activitiesTable.getSelected();
                    return !CollectionUtils.isEmpty(selected) && selected.size() == 1;
                }
                return false;
            }
        });
    }

    protected void initMoveAction() {
        activitiesTable.addAction(new BaseAction("move") {
            @Override
            public void actionPerform(Component component) {
                Set<WorkflowInstanceTask> items = activitiesTable.getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    Workflow wf = IterableUtils.get(items, 0).getInstance().getWorkflow();
                    WorkflowStepChooserDialog dialog = WorkflowStepChooserDialog.show(ActivityScreen.this, wf);
                    dialog.addCloseWithCommitListener(() -> {
                        try {
                            for (WorkflowInstanceTask item : items) {
                                WorkflowInstance instance = item.getInstance();
                                if (instance.getEndDate() == null ||
                                        !StringUtils.isEmpty(instance.getError())) {
                                    workflowService.moveWorkflow(instance, dialog.getStep());
                                }
                            }
                        } catch (WorkflowException e) {
                            throw new RuntimeException(getMessage("activityScreen.error.failedToMove"), e);
                        } finally {
                            activitiesTable.getDatasource().refresh();
                        }
                        showNotification(getMessage("activityScreen.caption.actionPerformed"), NotificationType.HUMANIZED);
                    });
                }
            }

            @Override
            public boolean isPermitted() {
                if (super.isPermitted()) {
                    Set<WorkflowInstanceTask> items = activitiesTable.getSelected();
                    if (!CollectionUtils.isEmpty(items)) {
                        Workflow wf = IterableUtils.get(items, 0).getInstance().getWorkflow();
                        for (WorkflowInstanceTask item : items) {
                            if (!Objects.equals(item.getInstance().getWorkflow(), wf) ||
                                    (item.getInstance().getEndDate() != null && StringUtils.isEmpty(item.getInstance().getError()))) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    protected void initExecuteAction() {
        activitiesTable.addAction(new BaseAction("execute") {
            @Override
            public void actionPerform(Component component) {
                Set<WorkflowInstanceTask> items = activitiesTable.getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    try {
                        for (WorkflowInstanceTask item : items) {
                            WorkflowInstance instance = item.getInstance();
                            if (instance.getEndDate() == null && StringUtils.isEmpty(instance.getError())) {
                                workflowService.recreateTasks(instance);
                            }
                        }
                    } catch (WorkflowException e) {
                        throw new RuntimeException(getMessage("activityScreen.error.failedToExecute"), e);
                    } finally {
                        activitiesTable.getDatasource().refresh();
                    }
                    showNotification(getMessage("activityScreen.caption.actionPerformed"), NotificationType.HUMANIZED);
                }
            }

            @Override
            public boolean isPermitted() {
                if (super.isPermitted()) {
                    Set<WorkflowInstanceTask> items = activitiesTable.getSelected();
                    if (!CollectionUtils.isEmpty(items)) {
                        for (WorkflowInstanceTask item : items) {
                            if (item.getInstance().getEndDate() != null ||
                                    !StringUtils.isEmpty(item.getInstance().getError())) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    protected void initRestartAction() {
        activitiesTable.addAction(new BaseAction("restart") {
            @Override
            public void actionPerform(Component component) {
                Set<WorkflowInstanceTask> items = activitiesTable.getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    try {
                        for (WorkflowInstanceTask item : items) {
                            if (!StringUtils.isEmpty(item.getInstance().getError())) {
                                workflowService.restartWorkflow(item.getInstance());
                            }
                        }
                    } catch (WorkflowException e) {
                        throw new RuntimeException(getMessage("activityScreen.error.failedToRestart"), e);
                    } finally {
                        activitiesTable.getDatasource().refresh();
                    }
                    showNotification(getMessage("activityScreen.caption.actionPerformed"), NotificationType.HUMANIZED);
                }
            }

            @Override
            public boolean isPermitted() {
                if (super.isPermitted()) {
                    Set<WorkflowInstanceTask> items = activitiesTable.getSelected();
                    if (!CollectionUtils.isEmpty(items)) {
                        for (WorkflowInstanceTask item : items) {
                            if (StringUtils.isEmpty(item.getInstance().getError())) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    protected void initResetAction() {
        activitiesTable.addAction(new BaseAction("reset") {
            @Override
            public void actionPerform(Component component) {
                Set<WorkflowInstanceTask> items = activitiesTable.getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    Action yes = new DialogAction(DialogAction.Type.YES, Status.PRIMARY).withHandler(event -> {
                        WorkflowInstance instance = IterableUtils.get(items, 0).getInstance();

                        WorkflowChooserDialog dialog = WorkflowChooserDialog.show(ActivityScreen.this, instance.getEntityName(), instance.getEntityId());
                        dialog.addCloseWithCommitListener(() -> {
                            try {
                                for (WorkflowInstanceTask item : items) {
                                    workflowService.resetWorkflow(item.getInstance(), dialog.getWorkflow());
                                }
                            } catch (WorkflowException e) {
                                throw new RuntimeException(getMessage("activityScreen.error.failedToReset"), e);
                            } finally {
                                activitiesTable.getDatasource().refresh();
                            }
                            showNotification(getMessage("activityScreen.caption.actionPerformed"), NotificationType.HUMANIZED);
                        });
                    });
                    Action no = new DialogAction(DialogAction.Type.NO);
                    showOptionDialog(
                            getMessage("activityScreen.warning"),
                            getMessage("activityScreen.resetDescription"),
                            MessageType.CONFIRMATION,
                            new Action[]{yes, no});
                }
            }

            @Override
            public boolean isPermitted() {
                if (super.isPermitted()) {
                    Set<WorkflowInstanceTask> items = activitiesTable.getSelected();
                    if (!CollectionUtils.isEmpty(items)) {
                        String entityName = IterableUtils.get(items, 0).getInstance().getEntityName();
                        for (WorkflowInstanceTask item : items) {
                            if (!Objects.equals(entityName, item.getInstance().getEntityName())) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }
}