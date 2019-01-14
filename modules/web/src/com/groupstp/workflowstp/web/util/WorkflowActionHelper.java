package com.groupstp.workflowstp.web.util;

import com.groupstp.workflowstp.dto.WorkflowExecutionContext;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.entity.UserRole;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Workflow UI actions helper class
 *
 * @author adiatullin
 */
@Component(WorkflowActionHelper.NAME)
public class WorkflowActionHelper {

    public static final String NAME = "WorkflowActionHelper";

    @Inject
    protected UserSessionSource userSessionSource;
    @Inject
    protected DataManager dataManager;

    /**
     * Check can cumulative action can be performed by current user.
     *
     * @param context workflow execution context
     * @param key     execution part key
     * @param role    expected user role
     * @return can current user perform cumulative action
     */
    public static boolean isCumulativeActionPerformableByRole(WorkflowExecutionContext context, String key, String role) {
        return ((WorkflowActionHelper) AppBeans.get(WorkflowActionHelper.NAME)).isCumulativeActionPerformableByRoleInternal(context, key, role);
    }

    protected boolean isCumulativeActionPerformableByRoleInternal(WorkflowExecutionContext context, String key, String role) {
        User user = userSessionSource.getUserSession().getUser();
        if (!isUserSatisfyByRole(user, role)) {
            return false;
        }
        String value = context.getParam(key);
        if (!StringUtils.isEmpty(value)) {
            String[] acceptedUsers = value.split(",");
            if (acceptedUsers.length > 0) {
                for (String acceptedUser : acceptedUsers) {
                    if (Objects.equals(acceptedUser, user.getLogin())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * User performed action is cumulative action performed at the end.
     *
     * @param context workflow execution context
     * @param key     execution part key
     * @param role    expected user role
     * @return can current user perform cumulative action
     */
    public static boolean cumulativeActionPerformed(WorkflowExecutionContext context, String key, String role) {
        return ((WorkflowActionHelper) AppBeans.get(WorkflowActionHelper.NAME)).cumulativeActionPerformedInternal(context, key, role);
    }

    protected boolean cumulativeActionPerformedInternal(WorkflowExecutionContext context, String key, String role) {
        User user = userSessionSource.getUserSession().getUser();
        String value = context.getParam(key);
        if (StringUtils.isEmpty(value)) {
            value = user.getLogin();
        } else {
            value = value + "," + user.getLogin();
        }
        context.putParam(key, value);

        List<String> users = getRoleUsers(role);
        if (!CollectionUtils.isEmpty(users)) {
            users.removeAll(Arrays.asList(value.split(",")));
            return users.isEmpty();
        }
        return true;
    }

    protected boolean isUserSatisfyByRole(User user, String role) {
        if (!CollectionUtils.isEmpty(user.getUserRoles())) {
            for (UserRole userRole : user.getUserRoles()) {
                if (Objects.equals(role, userRole.getRole().getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected List<String> getRoleUsers(String role) {
        List<User> users = dataManager.loadList(LoadContext.create(User.class)
                .setQuery(new LoadContext.Query("select e from sec$User e join e.userRoles ur join ur.role r where r.name = :roleName").setParameter("roleName", role))
                .setView(View.MINIMAL));
        if (!CollectionUtils.isEmpty(users)) {
            return users.stream()
                    .map(User::getLogin)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
