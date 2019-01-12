package com.groupstp.workflowstp.exception;

/**
 * Workflow execution specific exception
 *
 * @author adiatullin
 */
public class WorkflowException extends Exception {
    private static final long serialVersionUID = 5853498885081345588L;

    public WorkflowException(String message) {
        super(message);
    }

    public WorkflowException(String message, Exception e) {
        super(message, e);
    }
}
