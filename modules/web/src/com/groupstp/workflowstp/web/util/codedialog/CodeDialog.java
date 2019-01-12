package com.groupstp.workflowstp.web.util.codedialog;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.cuba.gui.components.SourceCodeEditor;

import javax.inject.Inject;
import java.util.Map;

/**
 * This dialog show code editor in full screen mode
 *
 * @author adiatullin
 */
public class CodeDialog extends AbstractWindow {
    public static final String SCREEN_ID = "code-dialog";

    private static final String CODE = "code";
    private static final String MODE = "mode";

    /**
     * Open dialog with code editor
     *
     * @param frame opening screen frame
     * @param code  code text to show
     */
    public static CodeDialog show(Frame frame, String code) {
        return show(frame, code, null);
    }

    /**
     * Open dialog with code editor
     *
     * @param frame opening screen frame
     * @param code  code text to show
     * @param mode  code mode (language)
     */
    public static CodeDialog show(Frame frame, String code, String mode) {
        return (CodeDialog) frame.openWindow(SCREEN_ID, WindowManager.OpenType.DIALOG, ParamsMap.of(CODE, code, MODE, mode));
    }

    @Inject
    private SourceCodeEditor codeEditor;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        codeEditor.setMode(SourceCodeEditor.Mode.parse((String) params.get(MODE)));
        codeEditor.setValue(params.get(CODE));
    }

    /**
     * @return edited source code result
     */
    public String getCode() {
        return codeEditor.getValue();
    }

    public void onOk() {
        close(COMMIT_ACTION_ID, true);
    }

    public void onCancel() {
        close(CLOSE_ACTION_ID, true);
    }
}
