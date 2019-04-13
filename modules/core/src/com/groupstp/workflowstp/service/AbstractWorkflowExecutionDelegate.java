package com.groupstp.workflowstp.service;

import com.groupstp.workflowstp.core.bean.MessageableBean;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;

import javax.inject.Inject;

/**
 * Basic workflow execution delegate class
 *
 * @author adiatullin
 */
public abstract class AbstractWorkflowExecutionDelegate extends MessageableBean implements WorkflowExecutionDelegate {

    @Inject
    protected DataManager dataManager;
    @Inject
    protected Metadata metadata;
    @Inject
    protected Persistence persistence;

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
