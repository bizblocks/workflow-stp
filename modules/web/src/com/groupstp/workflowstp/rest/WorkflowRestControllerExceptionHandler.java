package com.groupstp.workflowstp.rest;

import com.haulmont.addon.restapi.api.controllers.RestControllerExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Rest exceptions handler
 *
 * @author adiatullin
 */
@ControllerAdvice("com.groupstp.workflowstp.rest.controller")
public class WorkflowRestControllerExceptionHandler extends RestControllerExceptionHandler {
}