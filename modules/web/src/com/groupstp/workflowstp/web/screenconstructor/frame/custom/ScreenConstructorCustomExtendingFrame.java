package com.groupstp.workflowstp.web.screenconstructor.frame.custom;

import com.groupstp.workflowstp.web.screenconstructor.frame.AbstractScreenConstructorFrame;

/**
 * @author adiatullin
 */
public class ScreenConstructorCustomExtendingFrame extends AbstractScreenConstructorFrame {

    public void editCustomBeforeScript() {
        editGroovyInDialog(screenConstructor, "customBeforeScript");
    }

    public void testCustomBeforeScript() {
        test(screenConstructor.getCustomBeforeScript());
    }

    public void editCustomAfterScript() {
        editGroovyInDialog(screenConstructor, "customAfterScript");
    }

    public void testCustomAfterScript() {
        test(screenConstructor.getCustomAfterScript());
    }

    public void customBeforeScriptHint() {
        if (Boolean.TRUE.equals(screenConstructor.getIsBrowserScreen())) {
            createDialog(getMessage("screenConstructorCustomExtendingFrame.browse.customBeforeScriptTitle"),
                    getMessage("screenConstructorCustomExtendingFrame.browse.customBeforeScriptContent"));
        } else {
            createDialog(getMessage("screenConstructorCustomExtendingFrame.edit.customBeforeScriptTitle"),
                    getMessage("screenConstructorCustomExtendingFrame.edit.customBeforeScriptContent"));
        }
    }

    public void customAfterScriptHint() {
        if (Boolean.TRUE.equals(screenConstructor.getIsBrowserScreen())) {
            createDialog(getMessage("screenConstructorCustomExtendingFrame.browse.customAfterScriptTitle"),
                    getMessage("screenConstructorCustomExtendingFrame.browse.customAfterScriptContent"));
        } else {
            createDialog(getMessage("screenConstructorCustomExtendingFrame.edit.customAfterScriptTitle"),
                    getMessage("screenConstructorCustomExtendingFrame.edit.customAfterScriptContent"));
        }
    }
}
