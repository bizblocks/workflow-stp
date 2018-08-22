package com.groupstp.workflowstp.web.workflowinstancecomment;

import com.groupstp.workflowstp.entity.WorkflowInstanceComment;
import com.groupstp.workflowstp.entity.WorkflowInstanceTask;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.FieldGroup;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.util.Map;

/**
 * This dialog using for request comment from user which is actor of workflow
 *
 * @author adiatullin
 */
public class WorkflowInstanceCommentDialog extends AbstractWindow {
    public static final String SCREEN_ID = "workflow-instance-comment-dialog";
    public static final String WORKFLOW_TASK = "workflowTask";
    public static final String REQUIRED_COMMENT = "requiredComment";

    /**
     * Ask comment from user
     *
     * @param frame    active screen
     * @param task     current workflow task
     * @param required is comment required
     * @return dialog window object
     */
    public static Window askComment(Window frame, WorkflowInstanceTask task, Boolean required) {
        Preconditions.checkNotNullArgument(frame, "Frame is empty");
        Preconditions.checkNotNullArgument(task, "Task is empty");
        required = Boolean.TRUE.equals(required);

        return frame.openWindow(SCREEN_ID, WindowManager.OpenType.DIALOG,
                ParamsMap.of(WORKFLOW_TASK, task, REQUIRED_COMMENT, required));
    }

    @Inject
    private Metadata metadata;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private DataManager dataManager;

    @Inject
    private DatasourceImplementation<WorkflowInstanceComment> commentDs;
    @Inject
    private FieldGroup fieldGroup;

    @WindowParam(name = WORKFLOW_TASK, required = true)
    private WorkflowInstanceTask task;

    @WindowParam(name = REQUIRED_COMMENT)
    private Boolean required;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        task = reload(task, "workflowInstanceTask-browse");
        setupComment();
    }

    private void setupComment() {
        //create item and set into datasource
        WorkflowInstanceComment comment = metadata.create(WorkflowInstanceComment.class);
        comment.setInstance(task.getInstance());
        comment.setTask(task);
        comment.setAuthor(userSessionSource.getUserSession().getCurrentOrSubstitutedUser());

        commentDs.setItem(comment);
        commentDs.setModified(false);

        if (Boolean.TRUE.equals(required)) {
            fieldGroup.getFieldNN("comment").setRequired(true);
        }
    }

    public void onOkClick() {
        WorkflowInstanceComment comment = commentDs.getItem();

        if (!isEmpty(comment)) {
            dataManager.commit(comment);
            close(COMMIT_ACTION_ID, true);
        } else {
            if (Boolean.TRUE.equals(required)) {
                showNotification(getMessage("workflowInstanceCommentDialog.pleaseAddComment"), NotificationType.HUMANIZED);
            } else {
                close(CLOSE_ACTION_ID, true);
            }
        }
    }

    public void onCancelClick() {
        close(CLOSE_ACTION_ID);
    }

    //check is provided comment are filled to commit or not
    private boolean isEmpty(WorkflowInstanceComment comment) {
        return StringUtils.isEmpty(comment.getComment()) && comment.getAttachment() == null;
    }

    private <T extends Entity> T reload(T entity, String view) {
        if (entity != null) {
            entity = dataManager.reload(entity, view);
        }
        return entity;
    }
}
