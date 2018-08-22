package com.groupstp.workflowstp.exception;

/**
 * Workflow execution specific exception
 *
 * @author adiatullin
 */
public class WorkflowException extends Exception {

    private static final long serialVersionUID = 4878508813588005931L;

    public WorkflowException(String message) {
        super(message);
    }

    public WorkflowException(String message, Exception e) {
        super(message, e);
    }
}
