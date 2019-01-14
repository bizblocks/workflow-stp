package com.groupstp.workflowstp.web.workflowinstancetask;

import com.groupstp.workflowstp.entity.WorkflowInstance;
import com.groupstp.workflowstp.entity.WorkflowInstanceTask;
import com.groupstp.workflowstp.web.util.WebUiHelper;
import com.groupstp.workflowstp.web.util.WorkflowInstanceHelper;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import org.apache.commons.collections4.CollectionUtils;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

/**
 * @author adiatullin
 */
public class WorkflowInstanceTaskBrowse extends AbstractLookup {
    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;

    @Inject
    private GroupTable<WorkflowInstanceTask> workflowInstanceTasksTable;

    public void init(Map<String, Object> params) {
        super.init(params);

        WebUiHelper.showLinkOnTable(workflowInstanceTasksTable, "instance");

        workflowInstanceTasksTable.addAction(new BaseAction("openRelatedEntity") {
            @Override
            public String getCaption() {
                return getMessage("workflowInstanceTaskBrowse.openRelatedEntity");
            }

            @Override
            public void actionPerform(Component component) {
                WorkflowInstanceTask task = workflowInstanceTasksTable.getSingleSelected();
                WorkflowInstance instance = task == null ? null : task.getInstance();
                if (instance != null) {
                    AbstractEditor editor = openEditor(reload(instance.getEntityName(), instance.getEntityId()), WindowManager.OpenType.THIS_TAB);
                    editor.addCloseListener(actionId -> workflowInstanceTasksTable.getDatasource().refresh());
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
                if (super.isPermitted()) {//enable only for one item
                    Set selected = workflowInstanceTasksTable.getSelected();
                    return !CollectionUtils.isEmpty(selected) && selected.size() == 1;
                }
                return false;
            }
        });
    }
}