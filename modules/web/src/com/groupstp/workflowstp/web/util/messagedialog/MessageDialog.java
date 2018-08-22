package com.groupstp.workflowstp.web.util.messagedialog;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.cuba.gui.components.ResizableTextArea;

import javax.inject.Inject;

/**
 * This dialog show long message to user
 *
 * @author adiatullin
 */
public class MessageDialog extends AbstractWindow {
    public static final String SCREEN_ID = "message-dialog";
    public static final String MESSAGE = "message";

    /**
     * Open dialog with long message
     *
     * @param frame   opening screen frame
     * @param message message to show
     */
    public static void showText(Frame frame, String message) {
        frame.openWindow(SCREEN_ID, WindowManager.OpenType.DIALOG, ParamsMap.of(MESSAGE, message));
    }

    @WindowParam(name = MESSAGE)
    private String message;

    @Inject
    private ResizableTextArea textArea;

    @Override
    public void ready() {
        super.ready();

        textArea.setValue(message);
    }

    public void close() {
        close(CLOSE_ACTION_ID);
    }
}
