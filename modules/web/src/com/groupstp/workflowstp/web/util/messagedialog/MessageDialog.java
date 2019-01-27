package com.groupstp.workflowstp.web.util.messagedialog;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.global.UuidProvider;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.cuba.gui.components.ResizableTextArea;
import com.haulmont.cuba.web.toolkit.ui.CubaCopyButtonExtension;

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
    protected Button okBtn;
    @Inject
    protected Button cancelBtn;
    @Inject
    protected Button copyBtn;
    @Inject
    protected ResizableTextArea textArea;


    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initText(params);
        initButtons(params);
        initCopyButton(params);
    }

    protected void initText(Map<String, Object> params) {
        textArea.setValue(params.get(MESSAGE));
        textArea.setEditable(Boolean.TRUE.equals(params.get(EDITABLE)));
    }

    protected void initButtons(Map<String, Object> params) {
        boolean editable = Boolean.TRUE.equals(params.get(EDITABLE));
        boolean okOnly = Boolean.TRUE.equals(params.get(OK_ONLY));
        okBtn.setVisible(okOnly || editable);
        cancelBtn.setVisible(!okOnly);
    }

    protected void initCopyButton(Map<String, Object> params) {
        if (CubaCopyButtonExtension.browserSupportCopy()) {
            com.vaadin.ui.Button button = copyBtn.unwrap(com.vaadin.ui.Button.class);
            button.setIcon(com.vaadin.server.FontAwesome.CLIPBOARD);
            button.setDescription(messages.getMainMessage("systemInfoWindow.copy"));

            String contentClass = "copy_message-" + UuidProvider.createUuid();
            textArea.addStyleName(contentClass);
            CubaCopyButtonExtension copyExtension = CubaCopyButtonExtension.copyWith(button, contentClass + " textarea");

            String success = messages.getMainMessage("systemInfoWindow.copingSuccessful");
            String failed = messages.getMainMessage("systemInfoWindow.copingFailed");
            copyExtension.addCopyListener(e -> com.vaadin.ui.Notification.show(
                    e.isSuccess() ? success : failed, com.vaadin.ui.Notification.Type.TRAY_NOTIFICATION));
        } else {
            copyBtn.setVisible(false);
        }
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
