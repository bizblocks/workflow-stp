package com.groupstp.workflowstp.web.workflow.step.settings;

import com.groupstp.workflowstp.entity.Step;
import com.groupstp.workflowstp.entity.Workflow;
import com.groupstp.workflowstp.util.EqualsUtils;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.FieldGroup;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;

import static com.groupstp.workflowstp.entity.StageType.*;

/**
 * Workflow step additional settings dialog
 *
 * @author adiatullin
 */
public class StepSettingsDialog extends AbstractWindow {
    protected static final String SCREEN_ID = "step-settings-dialog";
    protected static final String WORKFLOW_PARAMETER = "workflow";
    protected static final String STEP_PARAMETER = "step";

    /**
     * Show to user a additional step settings dialog
     *
     * @param frame calling UI frame
     * @param wf    editing workflow
     * @param step  current editing workflow step
     */
    public static void show(Frame frame, Workflow wf, Step step) {
        Preconditions.checkNotNullArgument(frame, "Frame is empty");
        Preconditions.checkNotNullArgument(wf, "Workflow is empty");
        Preconditions.checkNotNullArgument(step, "Step is empty");

        frame.openWindow(SCREEN_ID, WindowManager.OpenType.DIALOG,
                ParamsMap.of(WORKFLOW_PARAMETER, wf, STEP_PARAMETER, step));
    }

    public static boolean support(@Nullable Workflow wf, @Nullable Step step) {
        return step != null && EqualsUtils.equalAny(step.getStage().getType(), ALGORITHM_EXECUTION, USERS_INTERACTION);
    }

    @Inject
    protected MetadataTools metadataTools;

    @Inject
    protected DatasourceImplementation<Step> stepDs;
    @Inject
    protected FieldGroup mainFieldGroup;

    protected Step originalStep;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initSettings((Workflow) params.get(WORKFLOW_PARAMETER), (Step) params.get(STEP_PARAMETER));
    }

    protected void initSettings(Workflow workflow, Step step) {
        originalStep = step;

        stepDs.setItem(metadataTools.copy(originalStep));
        stepDs.setModified(false);

        mainFieldGroup.getFieldNN("repeatSec").setVisible(ALGORITHM_EXECUTION.equals(step.getStage().getType()));
        mainFieldGroup.getFieldNN("timeoutSec").setVisible(EqualsUtils.equalAny(step.getStage().getType(), ALGORITHM_EXECUTION, USERS_INTERACTION));
    }

    public void onOk() {
        if (validateAll()) {
            metadataTools.copy(stepDs.getItem(), originalStep);

            close(COMMIT_ACTION_ID, true);
        }
    }
}
