package com.groupstp.workflowstp.rest;

import com.haulmont.restapi.controllers.RestControllerExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Rest exceptions handler
 *
 * @author adiatullin
 */
@ControllerAdvice("com.groupstp.workflowstp.rest.controller")
public class ExtRestControllerExceptionHandler extends RestControllerExceptionHandler {
}