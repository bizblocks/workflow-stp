package com.groupstp.workflowstp.web.util.action;

import com.haulmont.cuba.gui.components.actions.BaseAction;

import javax.annotation.Nullable;

/**
 * Base always active action
 *
 * @author adiatullin
 */
public class AlwaysActiveBaseAction extends BaseAction implements AlwaysActiveAction {
    public AlwaysActiveBaseAction(String id) {
        super(id);
    }

    protected AlwaysActiveBaseAction(String id, @Nullable String shortcut) {
        super(id, shortcut);
    }
}
