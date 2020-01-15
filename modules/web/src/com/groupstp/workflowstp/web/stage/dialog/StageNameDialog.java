package com.groupstp.workflowstp.web.stage.dialog;

import com.groupstp.workflowstp.entity.Stage;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.cuba.gui.components.TextField;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;

/**
 * Stage name entering dialog
 *
 * @author adiatullin
 */
public class StageNameDialog extends AbstractWindow {
    protected static final String SCREEN_ID = "stage-name-dialog";
    protected static final String NAME = "stage-name";
    protected static final String ENTITY_NAME = "entity-name";

    /**
     * Show to user dialog to enter name of the new stage
     *
     * @param frame        calling UI frame
     * @param possibleName possible new stage name
     * @param entityName   stage entity name
     * @return opened dialog
     */
    public static StageNameDialog show(Frame frame, @Nullable String possibleName, String entityName) {
        Preconditions.checkNotNullArgument(frame);
        Preconditions.checkNotEmptyString(entityName);

        return (StageNameDialog) frame.openWindow(SCREEN_ID, WindowManager.OpenType.DIALOG,
                ParamsMap.of(NAME, possibleName, ENTITY_NAME, entityName));
    }

    @Inject
    protected DataManager dataManager;

    @Inject
    protected TextField stageNameField;

    @WindowParam(name = NAME)
    protected String possibleStageName;
    @WindowParam(name = ENTITY_NAME, required = true)
    protected String entityName;

    /**
     * @return user entered stage name
     */
    public String getStageName() {
        return (String) stageNameField.getValue();
    }

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        stageNameField.setValue(possibleStageName);
    }

    public void onOk() {
        if (validateAll()) {
            if (!isUnique(getStageName())) {
                showNotification(getMessage("stageNameDialog.warning"),
                        getMessage("stageNameDialog.nameOfStageAlreadyUsing"), NotificationType.TRAY);
                return;
            }
            close(COMMIT_ACTION_ID, true);
        }
    }

    protected boolean isUnique(String stageName) {
        return dataManager.load(Stage.class)
                .query("select e from wfstp$Stage e where e.name = :name and e.entityName = :entityName")
                .parameter("name", stageName)
                .parameter("entityName", entityName)
                .view(View.MINIMAL)
                .optional()
                .orElse(null) == null;
    }

    public void onCancel() {
        close(CLOSE_ACTION_ID, true);
    }
}
