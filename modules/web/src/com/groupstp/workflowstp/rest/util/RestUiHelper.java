package com.groupstp.workflowstp.rest.util;

import com.groupstp.workflowstp.dto.WorkflowExecutionContext;
import com.groupstp.workflowstp.entity.Stage;
import com.groupstp.workflowstp.entity.WorkflowEntity;
import com.groupstp.workflowstp.entity.WorkflowInstance;
import com.groupstp.workflowstp.entity.WorkflowInstanceTask;
import com.groupstp.workflowstp.service.WorkflowService;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.entity.UserRole;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Rest side workflow extension util class
 *
 * @author adiatullin
 */
@SuppressWarnings("unchecked")
@Component(RestUiHelper.NAME)
public class RestUiHelper {

    public static final String NAME = "RestUiHelper";

    @Inject
    protected WorkflowService workflowService;
    @Inject
    protected Messages messages;
    @Inject
    protected UserSessionSource userSessionSource;
    @Inject
    protected DataManager dataManager;


    /**
     * Get current service
     *
     * @return RestUiHelper service
     */
    public static RestUiHelper get() {
        return AppBeans.get(RestUiHelper.NAME);
    }

    //--------------------------------Performers------------------------------------------

    /**
     * Perform workflow action from external systems
     * <p>
     * Usage:
     * <p>
     * import com.groupstp.workflowstp.rest.util.RestUiHelper;
     * import com.groupstp.workflowstp.web.util.MapHelper;
     * <p>
     * Map params = MapHelper.asMap("one", "true");
     * RestUiHelper.performWorkflowAction(entities, stage, viewOnly, payload, params);
     * <p>
     */
    public static void performWorkflowAction(Collection<WorkflowEntity> entities, Stage stage,
                                             Boolean viewOnly, @Nullable String payload, Map<String, String> params) {
        performWorkflowAction(entities, stage, viewOnly, payload, params, null);
    }

    /**
     * Perform workflow action from external systems with predicate
     * <p>
     * Usage:
     * <p>
     * import com.groupstp.workflowstp.rest.util.RestUiHelper;
     * import com.groupstp.workflowstp.web.util.MapHelper;
     * <p>
     * Map params = MapHelper.asMap("one", "true");
     * RestUiHelper.performWorkflowAction(entities, stage, viewOnly, payload, params, new Predicate());
     * <p>
     */
    public static void performWorkflowAction(Collection<WorkflowEntity> entities, Stage stage,
                                             Boolean viewOnly, @Nullable String payload, Map<String, String> params,
                                             @Nullable Predicate<WorkflowEntity> predicate) {
        get().performWorkflowActionInternal(entities, stage, viewOnly, payload, params, predicate);
    }

    /**
     * Perform workflow double accept action from external systems with predicate
     * <p>
     * Usage:
     * <p>
     * import com.groupstp.workflowstp.rest.util.RestUiHelper;
     * import com.groupstp.workflowstp.web.util.MapHelper;
     * <p>
     * Map params = MapHelper.asMap("one", "true");
     * RestUiHelper.performDoubleWorkflowAction(entities, stage, viewOnly, payload, params, "special_key");
     * <p>
     */
    public static void performDoubleWorkflowAction(Collection<WorkflowEntity> entities, Stage stage,
                                                   Boolean viewOnly, @Nullable String payload, Map<String, String> params,
                                                   String key) {
        get().performDoubleWorkflowActionInternal(entities, stage, viewOnly, payload, params, key);
    }

    /**
     * Check is can be performed double action right now for current user or not
     * <p>
     * Usage:
     * <p>
     * import com.groupstp.workflowstp.rest.util.RestUiHelper;
     * <p>
     * boolean performable = RestUiHelper.isDoubleWorkflowActionPerformable(entities, stage, viewOnly, payload, "special_key");
     * <p>
     */
    public static boolean isDoubleWorkflowActionPerformable(Collection<WorkflowEntity> entities, Stage stage,
                                                            Boolean viewOnly, @Nullable String payload, String key) {
        return get().isDoubleWorkflowActionPerformableInternal(entities, stage, viewOnly, payload, key);
    }


    //---------------------------Internal logic------------------------------------------

    protected void performWorkflowActionInternal(Collection<WorkflowEntity> entities, Stage stage,
                                                 Boolean viewOnly, @Nullable String payload, Map<String, String> params,
                                                 @Nullable Predicate<WorkflowEntity> predicate) {
        try {
            if (!CollectionUtils.isEmpty(entities)) {
                for (WorkflowEntity entity : entities) {
                    if (predicate == null || predicate.test(entity)) {
                        WorkflowInstanceTask itemTask = workflowService.getWorkflowInstanceTaskNN(entity, stage, View.MINIMAL);
                        workflowService.finishTask(itemTask, params);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(messages.getMainMessage("workflow.processingError"), e);
        }
    }

    protected void performDoubleWorkflowActionInternal(Collection<WorkflowEntity> entities, Stage stage,
                                                       Boolean viewOnly, @Nullable String payload, Map<String, String> params,
                                                       String key) {
        try {
            if (!CollectionUtils.isEmpty(entities)) {
                for (WorkflowEntity entity : entities) {
                    WorkflowInstance instance = workflowService.getWorkflowInstance(entity, View.MINIMAL);
                    WorkflowInstanceTask task = workflowService.getWorkflowInstanceTaskNN(entity, stage, View.MINIMAL);
                    WorkflowExecutionContext ctx = workflowService.getExecutionContext(instance);
                    String[] performers = doubleActionPerformed(ctx, key);
                    if (performers != null) {
                        for (Map.Entry<String, String> e : params.entrySet()) {
                            ctx.putParam(e.getKey(), e.getValue());
                        }
                        workflowService.finishTask(task, ctx.getParams(), performers);
                    } else {
                        workflowService.setExecutionContext(ctx, instance);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(messages.getMainMessage("workflow.processingError"), e);
        }
    }

    protected String[] doubleActionPerformed(WorkflowExecutionContext context, String key) {
        String[] performers = null;

        User user = userSessionSource.getUserSession().getUser();
        String value = context.getParam(key);
        if (StringUtils.isEmpty(value)) {
            value = user.getLogin();
        } else {
            performers = new String[]{value, user.getLogin()};
            value = value + "," + user.getLogin();
        }
        context.putParam(key, value);

        return performers;
    }

    protected boolean isDoubleWorkflowActionPerformableInternal(Collection<WorkflowEntity> entities, Stage stage,
                                                                Boolean viewOnly, @Nullable String payload, String key) {
        assert stage != null;
        if (!CollectionUtils.isEmpty(entities)) {
            for (WorkflowEntity entity : entities) {
                WorkflowInstance itemInstance = workflowService.getWorkflowInstance(entity, View.MINIMAL);
                if (itemInstance == null) {
                    return false;
                }
                WorkflowExecutionContext ctx = workflowService.getExecutionContext(itemInstance);
                if (!isDoubleWorkflowActionPerformable(ctx, key, stage)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    protected boolean isDoubleWorkflowActionPerformable(WorkflowExecutionContext context, String key, Stage stage) {
        User user = userSessionSource.getUserSession().getUser();
        if (!isUserSatisfy(user, stage)) {
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
     * Check can user perform task on this stage
     *
     * @param user  current user
     * @param stage processing stage
     * @return can current user perform task on this stage
     */
    protected boolean isUserSatisfy(User user, Stage stage) {
        if (!PersistenceHelper.isLoadedWithView(stage, "stage-actors")) {
            stage = dataManager.reload(stage, "stage-actors");
        }
        if (!CollectionUtils.isEmpty(stage.getActors())) {
            return stage.getActors().contains(user);
        }
        if (!CollectionUtils.isEmpty(stage.getActorsRoles())) {
            if (!CollectionUtils.isEmpty(user.getUserRoles())) {
                for (UserRole userRole : user.getUserRoles()) {
                    if (stage.getActorsRoles().contains(userRole.getRole())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
