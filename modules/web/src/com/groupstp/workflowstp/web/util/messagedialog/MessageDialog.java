package com.groupstp.workflowstp.web.util.messagedialog;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.cuba.gui.components.ResizableTextArea;

import javax.inject.Inject;
import java.util.Map;

/**
 * This dialog show long message to user
 *
 * @author adiatullin
 */
public class MessageDialog extends AbstractWindow {
    public static final String SCREEN_ID = "message-dialog";
    private static final String MESSAGE = "message";
    private static final String EDITABLE = "editable";
    private static final String OK_ONLY = "ok_only";

    /**
     * Open dialog with long message
     *
     * @param frame   opening screen frame
     * @param message message to show
     */
    public static void showText(Frame frame, String message) {
        showText(frame, message, false);
    }

    /**
     * Open dialog with long message
     *
     * @param frame    opening screen frame
     * @param message  message to show
     * @param editable can message be edited by user
     */
    public static MessageDialog showText(Frame frame, String message, boolean editable) {
        return (MessageDialog) frame.openWindow(SCREEN_ID, WindowManager.OpenType.DIALOG, ParamsMap.of(MESSAGE, message, EDITABLE, editable));
    }

    public static MessageDialog showText(Frame frame, String message, boolean editable, boolean showOkOnly) {
        return (MessageDialog) frame.openWindow(SCREEN_ID, WindowManager.OpenType.DIALOG, ParamsMap.of(MESSAGE, message, EDITABLE, editable, OK_ONLY, showOkOnly));
    }

    @Inject
    private Button okBtn;
    @Inject
    private Button cancelBtn;
    @Inject
    private ResizableTextArea textArea;


    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        textArea.setValue(params.get(MESSAGE));
        boolean editable = Boolean.TRUE.equals(params.get(EDITABLE));
        boolean okOnly = Boolean.TRUE.equals(params.get(OK_ONLY));
        textArea.setEditable(editable);
        okBtn.setVisible(okOnly || editable);
        cancelBtn.setVisible(!okOnly);
    }

    public String getMessage() {
        return textArea.getValue();
    }

    public void onOk() {
        close(COMMIT_ACTION_ID, true);
    }

    public void onCancel() {
        close(CLOSE_ACTION_ID, true);
    }
}
