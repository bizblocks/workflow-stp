package com.groupstp.workflowstp.web.workflowinstance;

import com.groupstp.workflowstp.entity.WorkflowInstance;
import com.groupstp.workflowstp.entity.WorkflowInstanceComment;
import com.groupstp.workflowstp.web.util.WebUiHelper;
import com.groupstp.workflowstp.web.util.WebWorkflowHelper;
import com.groupstp.workflowstp.web.util.messagedialog.MessageDialog;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.app.core.file.FileDownloadHelper;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.util.Map;

/**
 * @author adiatullin
 */
public class WorkflowInstanceEdit extends AbstractEditor<WorkflowInstance> {
    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;

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

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initWorkflowLookup();
        initContextLookup();
        initCommentsTable();

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
                MessageDialog.showText(WorkflowInstanceEdit.this, getItem().getContext());
            }
        });
    }

    //setup comments table
    private void initCommentsTable() {
        FileDownloadHelper.initGeneratedColumn(commentsTable, "attachment");
        WebUiHelper.showLinkOnTable(commentsTable, "author");
    }

    @Override
    public void postInit() {
        super.postInit();

        initRelatedEntityLookup();
        initErrorLookup();
    }

    private void initRelatedEntityLookup() {
        final String entityName = getItem().getEntityName();
        final Object entityId = WebWorkflowHelper.parseEntityId(entityName, getItem().getEntityId());

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
}