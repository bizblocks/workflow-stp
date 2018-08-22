package com.groupstp.workflowstp.web.queryexample.workflow.frame;

import com.groupstp.workflowstp.entity.QueryExample;
import com.groupstp.workflowstp.entity.Stage;
import com.groupstp.workflowstp.entity.StageType;
import com.groupstp.workflowstp.entity.Workflow;
import com.groupstp.workflowstp.exception.WorkflowException;
import com.groupstp.workflowstp.service.WorkflowService;
import com.groupstp.workflowstp.web.queryexample.workflow.QueryWorkflowEdit;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.Scripting;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.*;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * This frame using for create view for each stage of queryexample workflow
 *
 * @author adiatullin
 * @see com.groupstp.workflowstp.web.queryexample.workflow.QueryWorkflowBrowse
 */
public class QueryWorkflowBrowseTableFrame extends AbstractFrame {
    private static final Logger log = LoggerFactory.getLogger(QueryWorkflowBrowseTableFrame.class);

    public static final String STAGE = "stage";
    public static final String WORKFLOW = "workflow";

    @Inject
    protected WorkflowService service;
    @Inject
    protected ComponentsFactory componentsFactory;
    @Inject
    protected UserSessionSource userSessionSource;
    @Inject
    protected Scripting scripting;

    @Inject
    protected CollectionDatasource<QueryExample, UUID> queriesDs;
    @Inject
    protected Table<QueryExample> queriesTable;
    @Inject
    protected ButtonsPanel buttonsPanel;

    @WindowParam(name = STAGE)
    protected Stage stage;

    @WindowParam(name = WORKFLOW)
    protected Workflow workflow;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initSqlQuery();
        initStageTableBehaviour();
        initWorkflowExtension();
    }

    //setup table datasource sql queryexample
    private void initSqlQuery() {
        String sqlQuery = "select e from wfstp$QueryExample e ";
        if (stage == null) {//this is my queries tab
            sqlQuery = sqlQuery + "where e.initiator.id = '" + getUser().getId() + "'";
        } else {
            sqlQuery = sqlQuery + "where e.stepName = '" + stage.getName() + "'";
        }
        queriesDs.setQuery(sqlQuery);
        queriesDs.refresh();
    }

    //setup actions which depends on workflow stage
    private void initStageTableBehaviour() {
        if (stage == null) {
            initMyQueriesView();
        } else {
            initStageQueriesView();
        }

        EditAction editAction = (EditAction) queriesTable.getAction(EditAction.ACTION_ID);
        if (editAction != null) {//after editing refresh table
            editAction.setAfterCommitHandler(entity -> queriesDs.refresh());
        }
    }

    //Table view for 'My queries' tab
    private void initMyQueriesView() {
        //add additional columns
        MetaPropertyPath path = queriesDs.getMetaClass().getPropertyPath("status");
        Table.Column column = new Table.Column(path, "status");
        //noinspection ConstantConditions
        column.setType(path.getRangeJavaClass());
        column.setCaption(messages.getMessage(QueryExample.class, "QueryExample.status"));
        queriesTable.addColumn(column);

        path = queriesDs.getMetaClass().getPropertyPath("stepName");
        column = new Table.Column(path, "stepName");
        //noinspection ConstantConditions
        column.setType(path.getRangeJavaClass());
        column.setCaption(messages.getMessage(QueryExample.class, "QueryExample.stepName"));
        queriesTable.addColumn(column);

        //add actions and buttons
        CreateAction createAction = new CreateAction(queriesTable) {
            @Override
            public String getWindowId() {
                return "queryexample-workflow-edit";
            }
        };
        Button createButton = componentsFactory.createComponent(Button.class);
        createButton.setAction(createAction);

        EditAction editAction = new EditAction(queriesTable) {
            @Override
            public String getWindowId() {
                return "queryexample-workflow-edit";
            }

            @Override
            public Map<String, Object> getWindowParams() {
                Map<String, Object> params = new HashMap<>();
                Map<String, Object> superParams = super.getWindowParams();
                if (superParams != null && superParams.size() > 0) {
                    params.putAll(superParams);
                }
                params.put(QueryWorkflowEdit.EDITABLE, canEdit(queriesTable.getSingleSelected()));
                return params;
            }

            @Override
            public boolean isPermitted() {
                if (super.isPermitted()) {
                    Set<QueryExample> queries = queriesTable.getSelected();
                    return !CollectionUtils.isEmpty(queries) && queries.size() == 1;
                }
                return false;
            }
        };
        Button editButton = componentsFactory.createComponent(Button.class);
        editButton.setAction(editAction);

        RemoveAction removeAction = new RemoveAction(queriesTable) {
            @Override
            public boolean isPermitted() {
                if (super.isPermitted()) {
                    Set<QueryExample> queries = queriesTable.getSelected();
                    if (!CollectionUtils.isEmpty(queries)) {
                        for (QueryExample query : queries) {
                            if (!canDelete(query)) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        };
        Button removeButton = componentsFactory.createComponent(Button.class);
        removeButton.setAction(removeAction);

        BaseAction runAction = null;
        Button runButton = null;
        if (workflow != null) {
            runAction = new BaseAction("run") {
                @Override
                public String getCaption() {
                    return getMessage("queryWorkflowBrowseTableFrame.startWorkflow");
                }

                @Override
                public String getIcon() {
                    return CubaIcon.OK.source();
                }

                @Override
                public void actionPerform(Component component) {
                    final QueryExample query = queriesTable.getSingleSelected();
                    if (query != null) {
                        Action doAction = new DialogAction(DialogAction.Type.YES, Status.PRIMARY).withHandler(event -> {
                            try {
                                service.startWorkflow(query, workflow);
                                showNotification(getMessage("queryWorkflowBrowseTableFrame.workflowStarted"), NotificationType.HUMANIZED);
                            } catch (WorkflowException e) {
                                log.error(String.format("Failed to launch workflow %s for queryexample %s", workflow, query), e);

                                showNotification(String.format(getMessage("queryWorkflowBrowseTableFrame.workflowFailed"),
                                        e.getMessage() == null ? getMessage("queryWorkflowBrowseTableFrame.notAvailable") : e.getMessage()),
                                        NotificationType.ERROR);
                            } finally {
                                queriesDs.refresh();
                            }
                        });
                        showOptionDialog(
                                messages.getMainMessage("dialogs.Confirmation"),
                                getMessage("queryWorkflowBrowseTableFrame.startWorkflowConfirmation"),
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
                    if (super.isPermitted()) {
                        Set<QueryExample> queries = queriesTable.getSelected();
                        if (!CollectionUtils.isEmpty(queries) && queries.size() == 1) {
                            return canRun(IterableUtils.get(queries, 0));
                        }
                    }
                    return false;
                }
            };
            runButton = componentsFactory.createComponent(Button.class);
            runButton.setAction(runAction);
        }

        RefreshAction refreshAction = new RefreshAction(queriesTable);
        Button refreshButton = componentsFactory.createComponent(Button.class);
        refreshButton.setAction(refreshAction);


        queriesTable.addAction(createAction);
        queriesTable.addAction(editAction);
        queriesTable.addAction(removeAction);
        if (runAction != null) {
            queriesTable.addAction(runAction);
        }
        queriesTable.addAction(refreshAction);
        buttonsPanel.add(createButton);
        buttonsPanel.add(editButton);
        buttonsPanel.add(removeButton);
        if (runButton != null) {
            buttonsPanel.add(runButton);
        }
        buttonsPanel.add(refreshButton);
    }

    //Base table view for non 'My Queries' tab
    private void initStageQueriesView() {
        //setup actions and buttons
        EditAction editAction = new EditAction(queriesTable) {
            @Override
            public String getWindowId() {
                return "queryexample-workflow-edit";
            }

            @Override
            public Map<String, Object> getWindowParams() {
                Map<String, Object> params = new HashMap<>();
                Map<String, Object> superParams = super.getWindowParams();
                if (superParams != null && superParams.size() > 0) {
                    params.putAll(superParams);
                }
                params.put(QueryWorkflowEdit.EDITABLE, false);
                params.put(QueryWorkflowEdit.STAGE, stage);
                params.put(QueryWorkflowEdit.WORKFLOW, workflow);
                return params;
            }

            @Override
            public boolean isPermitted() {
                if (super.isPermitted()) {
                    Set<QueryExample> queries = queriesTable.getSelected();
                    return !CollectionUtils.isEmpty(queries) && queries.size() == 1;
                }
                return false;
            }
        };
        Button viewButton = componentsFactory.createComponent(Button.class);
        viewButton.setAction(editAction);

        RefreshAction refreshAction = new RefreshAction(queriesTable);
        Button refreshButton = componentsFactory.createComponent(Button.class);
        refreshButton.setAction(refreshAction);


        queriesTable.addAction(editAction);
        queriesTable.addAction(refreshAction);
        buttonsPanel.add(viewButton);
        buttonsPanel.add(refreshButton);
    }

    private void initWorkflowExtension() {
        if (stage != null && workflow != null) {//this is not default tab, we must extend its view by stage behaviour
            if (StageType.USERS_INTERACTION.equals(stage.getType())) {
                final String script = stage.getBrowseScreenGroovyScript();
                if (!StringUtils.isEmpty(script)) {
                    final Map<String, Object> binding = new HashMap<>();
                    binding.put("stage", stage);
                    binding.put("workflow", workflow);
                    binding.put("screen", this);
                    try {
                        scripting.evaluateGroovy(script, binding);
                    } catch (Exception e) {
                        log.error("Failed to evaluate browse screen groovy for workflow {}({}) and stage {}({})",
                                workflow, workflow.getId(), stage, stage.getId());
                        throw new RuntimeException(getMessage("queryWorkflowBrowseTableFrame.errorOnScreenExtension"), e);
                    }
                }
            }
        }
    }

    protected User getUser() {
        User user = userSessionSource.getUserSession().getCurrentOrSubstitutedUser();
        if (user == null) {
            throw new DevelopmentException(getMessage("queryWorkflowBrowseTableFrame.userNotFound"));
        }
        return user;
    }

    //check what we can provide to user to delete queryexample
    protected boolean canDelete(QueryExample query) {
        return query != null && query.getStatus() == null;
    }

    //check what we can provide to user to edit queryexample
    protected boolean canEdit(QueryExample query) {
        return query != null && query.getStatus() == null;
    }

    //check what we can provide to user to run queryexample
    protected boolean canRun(QueryExample query) {
        return query != null && query.getStatus() == null;
    }
}
