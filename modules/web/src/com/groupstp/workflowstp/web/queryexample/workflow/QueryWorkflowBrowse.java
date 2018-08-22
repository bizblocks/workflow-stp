package com.groupstp.workflowstp.web.queryexample.workflow;

import com.groupstp.workflowstp.entity.*;
import com.groupstp.workflowstp.web.queryexample.workflow.frame.QueryWorkflowBrowseTableFrame;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.components.AbstractLookup;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.security.entity.RoleType;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.entity.UserRole;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * QueryExample entity browser screen which using special for workflow extension
 *
 * @author adiatullin
 */
public class QueryWorkflowBrowse extends AbstractLookup {
    private static final Logger log = LoggerFactory.getLogger(QueryWorkflowBrowse.class);

    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private UserSessionSource userSessionSource;

    @Inject
    private TabSheet tabSheet;

    private Workflow activeWorkflow;
    private User user;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        activeWorkflow = getActiveWorkflow();
        user = getUser();

        initTabSheets();
    }

    //initialize tabs view which dependents on active queryexample workflow
    private void initTabSheets() {
        //default tab
        if (isShowMyQueriesTab(user)) {
            TabSheet.Tab tab = tabSheet.addTab("myqueries", createTab(null));
            tab.setCaption(getMessage("queryWorkflowBrowse.myQueries"));
        }

        if (activeWorkflow != null && !CollectionUtils.isEmpty(activeWorkflow.getSteps())) {
            for (Step step : activeWorkflow.getSteps()) {
                if (isSatisfyByUser(step.getStage())) {
                    String stageName = step.getStage().getName();
                    String tabKey = stageName.replaceAll("\\s", StringUtils.EMPTY).toLowerCase();

                    TabSheet.Tab tab = tabSheet.addTab(tabKey, createTab(step.getStage()));
                    tab.setCaption(stageName);
                }
            }
        }
    }

    //check what is current user is actor of this stage
    private boolean isSatisfyByUser(Stage stage) {
        if (stage != null) {
            if (StageType.USERS_INTERACTION.equals(stage.getType())) {
                if (!CollectionUtils.isEmpty(stage.getActorsRoles())) {
                    if (!CollectionUtils.isEmpty(user.getUserRoles())) {
                        for (UserRole ur : user.getUserRoles()) {
                            if (stage.getActorsRoles().contains(ur.getRole())) {
                                return true;
                            }
                        }
                    }
                } else if (!CollectionUtils.isEmpty(stage.getActors())) {
                    return stage.getActors().contains(user);
                }
            }
        }
        return false;
    }

    private Component createTab(@Nullable Stage stage) {
        return openFrame(null, "queryexample-workflow-table",
                ParamsMap.of(QueryWorkflowBrowseTableFrame.STAGE, stage,
                        QueryWorkflowBrowseTableFrame.WORKFLOW, activeWorkflow));
    }

    @Override
    public void ready() {
        super.ready();

        initTabSelection();
    }

    private void initTabSelection() {
        Element element = getSettings().get(tabSheet.getId());
        String tabName = element.attributeValue("q_tab");
        if (!StringUtils.isEmpty(tabName)) {
            TabSheet.Tab tab = tabSheet.getTab(tabName);
            if (tab != null) {
                tabSheet.setSelectedTab(tab);
            }
        }

        tabSheet.addSelectedTabChangeListener(event -> {
            String currentTabName = event.getSelectedTab() == null ? null : event.getSelectedTab().getName();
            element.addAttribute("q_tab", currentTabName);
        });
    }

    //retrieve one active workflow for queryexample entity
    @Nullable
    private Workflow getActiveWorkflow() {
        String entityName = metadata.getClassNN(QueryExample.class).getName();
        List<Workflow> list = dataManager.loadList(LoadContext.create(Workflow.class)
                .setQuery(new LoadContext.Query("select e from wfstp$Workflow e where " +
                        "e.active = true and e.entityName = :entityName order by e.createTs asc")
                        .setParameter("entityName", entityName))
                .setView("query-workflow-browse"));
        if (!CollectionUtils.isEmpty(list)) {
            if (list.size() > 1) {
                log.warn(String.format("In system existing two active workflow for entity '%s'. The first will be used", entityName));
            }
            return list.get(0);
        }
        return null;
    }

    //get current user
    private User getUser() {
        User user = userSessionSource.getUserSession().getCurrentOrSubstitutedUser();
        if (user == null) {
            throw new DevelopmentException(getMessage("queryWorkflowBrowse.userNotFound"));
        }
        user = dataManager.reload(user, "user-with-role");
        if (user == null) {
            throw new DevelopmentException(getMessage("queryWorkflowBrowse.reloadedUserNotFound"));
        }
        return user;
    }

    //check is should we show default tab for provided user or not
    private boolean isShowMyQueriesTab(User user) {
        if (!CollectionUtils.isEmpty(user.getUserRoles())) {
            for (UserRole ur : user.getUserRoles()) {
                if (Boolean.TRUE.equals(ur.getRole().getDefaultRole()) || RoleType.SUPER.equals(ur.getRole().getType())) {
                    return true;
                }
            }
        }
        return false;
    }
}