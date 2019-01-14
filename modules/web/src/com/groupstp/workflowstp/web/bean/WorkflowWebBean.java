package com.groupstp.workflowstp.web.bean;

import com.groupstp.workflowstp.entity.Stage;
import com.groupstp.workflowstp.entity.WorkflowEntity;
import com.groupstp.workflowstp.entity.WorkflowInstance;
import com.groupstp.workflowstp.entity.WorkflowInstanceTask;
import com.groupstp.workflowstp.exception.WorkflowException;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.cuba.security.entity.User;

import java.io.Serializable;
import java.util.List;

/**
 * Web side workflow bean to extend a screens
 *
 * @author adiatullin
 */
public interface WorkflowWebBean {
    String NAME = "wfstp_WorkflowWebBean";

    String TARGET = "target";
    String STAGE = "stage";
    String VIEW_ONLY = "viewOnly";
    String SCREEN = "screen";
    String ENTITY = "entity";
    String CONTEXT = "context";
    String WORKFLOW_INSTANCE = "workflowInstance";
    String WORKFLOW_INSTANCE_TASK = "workflowInstanceTask";

    /**
     * Get all system workflow entities meta classes
     *
     * @return workflow entities metaclasses in system
     */
    List<MetaClass> getWorkflowEntities();

    /**
     * Receive a workflow entity extendable screens
     *
     * @param metaClass workflow entity meta class
     * @return information about the extendable screens of specified workflow entity class
     */
    WorkflowScreenInfo getWorkflowEntityScreens(MetaClass metaClass);

    /**
     * Check what provided user is actor of this stage
     *
     * @param user  user to check
     * @param stage stage to access
     * @return is actor user or not
     */
    boolean isActor(User user, Stage stage);

    /**
     * Check what provided user is viewer of this stage
     *
     * @param user  user to check
     * @param stage stage to access
     * @return is viewer user or not
     */
    boolean isViewer(User user, Stage stage);

    /**
     * Extend a browser screen.
     *
     * @param stage    current extending workflow stage
     * @param screen   extending screen link
     * @param viewOnly is browser screen opening for view only
     * @throws WorkflowException in case of any unexpected problems
     */
    void extendBrowser(Stage stage, Frame screen, boolean viewOnly) throws WorkflowException;

    /**
     * Extend a editor screen
     *
     * @param entity           current processing entity
     * @param screen           extending screen link
     * @throws WorkflowException in case of any unexpected problems
     */
    void extendEditor(WorkflowEntity entity, Frame screen) throws WorkflowException;

    /**
     * Extend a editor screen
     *
     * @param stage            current extending workflow stage
     * @param entity           current processing entity
     * @param screen           extending screen link
     * @param workflowInstance current active workflow instance
     * @param task             current processing workflow instance task
     * @throws WorkflowException in case of any unexpected problems
     */
    void extendEditor(Stage stage, WorkflowEntity entity, Frame screen,
                      WorkflowInstance workflowInstance, WorkflowInstanceTask task) throws WorkflowException;

    /**
     * External extension of specific screen
     *
     * @param templateKey extension template unique key
     * @param screen      extending screen
     * @throws WorkflowException in case of any unexpected problems
     */
    void extendScreen(String templateKey, Frame screen) throws WorkflowException;


    /**
     * Workflow entity extendable screen information
     */
    class WorkflowScreenInfo implements Serializable {
        private static final long serialVersionUID = 8206880247489658299L;

        private final MetaClass metaClass;
        private final String browserScreenId;
        private final String editorScreenId;

        public WorkflowScreenInfo(MetaClass metaClass, String browserScreenId, String editorScreenId) {
            this.metaClass = metaClass;
            this.browserScreenId = browserScreenId;
            this.editorScreenId = editorScreenId;
        }

        public MetaClass getMetaClass() {
            return metaClass;
        }

        public String getBrowserScreenId() {
            return browserScreenId;
        }

        public String getEditorScreenId() {
            return editorScreenId;
        }
    }
}
