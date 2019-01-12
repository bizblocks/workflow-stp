package com.groupstp.workflowstp.web.workflowinstance;

import com.groupstp.workflowstp.entity.Workflow;
import com.groupstp.workflowstp.entity.WorkflowInstance;
import com.groupstp.workflowstp.service.WorkflowService;
import com.groupstp.workflowstp.web.util.WebUiHelper;
import com.groupstp.workflowstp.web.util.WorkflowInstanceHelper;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.actions.RemoveAction;
import com.haulmont.cuba.security.entity.EntityOp;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

/**
 * @author adiatullin
 */
public class WorkflowInstanceBrowse extends AbstractLookup {
    @Inject
    private Metadata metadata;
    @Inject
    private DataManager dataManager;
    @Inject
    private Security security;
    @Inject
    private WorkflowService service;

    @Inject
    private Table<WorkflowInstance> workflowInstancesTable;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        WebUiHelper.showLinkOnTable(workflowInstancesTable, "workflow", entity -> ((Workflow) entity).getName());

        workflowInstancesTable.addAction(new BaseAction("openRelatedEntity") {
            @Override
            public String getCaption() {
                return getMessage("workflowInstanceBrowse.openRelatedEntity");
            }

            @Override
            public void actionPerform(Component component) {
                WorkflowInstance instance = workflowInstancesTable.getSingleSelected();
                if (instance != null) {
                    AbstractEditor editor = openEditor(reload(instance.getEntityName(), instance.getEntityId()), WindowManager.OpenType.THIS_TAB);
                    editor.addCloseListener(actionId -> workflowInstancesTable.getDatasource().refresh());
                }
            }

            //reload only if user clicked the button
            private Entity reload(String name, String id) {
                //noinspection unchecked
                return dataManager.load(LoadContext.create(metadata.getClassNN(name).getJavaClass())
                        .setId(WorkflowInstanceHelper.parseEntityId(name, id))
                        .setView(View.MINIMAL));
            }

            @Override
            public boolean isPermitted() {
                if (super.isPermitted()) {
                    Set selected = workflowInstancesTable.getSelected();
                    return !CollectionUtils.isEmpty(selected) && selected.size() == 1;
                }
                return false;
            }
        });

        workflowInstancesTable.addAction(new RemoveAction(workflowInstancesTable) {
            @Override
            public boolean isPermitted() {
                if (super.isPermitted() && security.isEntityOpPermitted(WorkflowInstance.class, EntityOp.DELETE)) {
                    Set<WorkflowInstance> selected = workflowInstancesTable.getSelected();
                    if (!CollectionUtils.isEmpty(selected) && selected.size() == 1) {
                        return IterableUtils.get(selected, 0).getEndDate() != null;//remove only finished instances
                    }
                }
                return false;
            }
        });

        workflowInstancesTable.addAction(new BaseAction("restart") {
            @Override
            public void actionPerform(Component component) {
                WorkflowInstance instance = workflowInstancesTable.getSingleSelected();
                if (instance != null) {
                    Action doAction = new DialogAction(DialogAction.Type.YES, Status.PRIMARY).withHandler(event -> {
                        try {
                            service.restartWorkflow(instance);
                        } catch (Exception e) {
                            throw new RuntimeException(getMessage("workflowInstanceBrowse.restartFailed"), e);
                        } finally {
                            workflowInstancesTable.getDatasource().refresh();
                        }
                    });
                    showOptionDialog(
                            messages.getMainMessage("dialogs.Confirmation"),
                            getMessage("workflowInstanceBrowse.restartConfirmation"),
                            MessageType.CONFIRMATION,
                            new Action[]{
                                    doAction,
                                    new DialogAction(DialogAction.Type.NO)
                            }
                    );
                }
            }

            @Override
            public boolean isPermitted() {
                if (super.isPermitted() && security.isEntityOpPermitted(WorkflowInstance.class, EntityOp.UPDATE)) {
                    Set<WorkflowInstance> selected = workflowInstancesTable.getSelected();
                    if (!CollectionUtils.isEmpty(selected) && selected.size() == 1) {
                        return !StringUtils.isEmpty(IterableUtils.get(selected, 0).getError());//restart possible only for fail items
                    }
                }
                return false;
            }
        });
    }
}