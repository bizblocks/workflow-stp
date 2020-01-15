package com.groupstp.workflowstp.web.workflowinstance.dialog;

import com.groupstp.workflowstp.entity.Step;
import com.groupstp.workflowstp.entity.Workflow;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.cuba.gui.components.LookupField;
import org.apache.commons.collections4.CollectionUtils;

import javax.inject.Inject;
import java.util.Map;
import java.util.TreeMap;

/**
 * Special workflow steps chooser dialog
 *
 * @author adiatullin
 */
public class WorkflowStepChooserDialog extends AbstractWindow {
    private static final String SCREEN_ID = "workflow-step-chooser-dialog";
    private static final String WORKFLOW_PARAMETER = "workflow";

    /**
     * Show to user a dialog to choose the step from the workflow instance
     *
     * @param frame    calling UI frame
     * @param workflow current workflow entity
     * @return opened dialog
     */
    public static WorkflowStepChooserDialog show(Frame frame, Workflow workflow) {
        Preconditions.checkNotNullArgument(frame);
        Preconditions.checkNotNullArgument(workflow);

        return (WorkflowStepChooserDialog) frame.openWindow(SCREEN_ID, WindowManager.OpenType.DIALOG,
                ParamsMap.of(WORKFLOW_PARAMETER, workflow));
    }

    @Inject
    protected DataManager dataManager;

    @Inject
    protected LookupField stepField;

    /**
     * @return user selected step from provided workflow
     */
    public Step getStep() {
        return (Step) stepField.getValue();
    }

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initStepField(dataManager.reload((Workflow) params.get(WORKFLOW_PARAMETER), "workflow-edit"));
    }

    protected void initStepField(Workflow workflow) {
        Map<String, Step> options = new TreeMap<>();
        if (!CollectionUtils.isEmpty(workflow.getSteps())) {
            for (Step step : workflow.getSteps()) {
                options.put(step.getStage().getName(), step);
            }
        }
        stepField.setOptionsMap(options);
    }

    public void onOk() {
        if (validateAll()) {
            close(COMMIT_ACTION_ID, true);
        }
    }

    public void onCancel() {
        close(CLOSE_ACTION_ID, true);
    }
}
